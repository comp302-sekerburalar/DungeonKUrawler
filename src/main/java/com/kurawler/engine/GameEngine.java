package com.kurawler.engine;

import com.kurawler.game.action.Action;
import com.kurawler.game.entity.*;
import com.kurawler.game.objects.GameObjects;
import com.kurawler.game.objects.GameObject;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Central game engine: owns the map, hero, enemy list, and the game-tick timer.
 *
 * Responsibilities:
 * – Move hero (with collision check and energy drain)
 * – Query adjacent objects for the 3×3 interaction radius
 * – Execute actions
 * – Spawn enemies on a timer (spec §2.5: every 9 s, max 5)
 * – Run enemy AI ticks
 * – Notify the UI layer via callbacks
 */

/*
 * Overview:
 * GameEngine manages the core state and behavior of the dungeon game.
 * It controls the map, hero, enemies, object interactions,
 * enemy spawning, and game update timers.
 *
 * Abstract Function:
 * AF(map, hero, enemies) =
 * the current playable game state consisting of:
 * - the dungeon map layout
 * - the hero state and position
 * - the active enemy entities
 * - game interaction and spawning logic
 *
 * Representation Invariant:
 * - map != null
 * - hero != null
 * - enemies != null
 * - enemies.size() <= MAX_ENEMIES
 * - no enemy in enemies is null
 * - rng != null
 */
public class GameEngine {

    // ---------- Spec constants ----------
    private static final int SPAWN_INTERVAL_S = 9;
    private static final int MAX_ENEMIES = 5;
    private static final double SPAWN_PROBABILITY_KNIGHT = 0.60;
    private static final double SPAWN_PROBABILITY_SORCERER = 0.30;
    // remaining 10 % = no spawn

    // ---------- Core state ----------
    private final GridMap map;
    private final Hero hero;
    private final List<Enemy> enemies = new ArrayList<>();
    // private final Random rng = new Random();
    private Random rng = new Random();
    private int enemyIdCounter = 0;

    // ---------- UI callbacks ----------
    private Runnable onMapChanged;
    private Runnable onStatsChanged;
    private Consumer<String> onMessage;
    private Consumer<String> onAiLog; // receives enemy AI state strings

    // ---------- Timelines ----------
    private Timeline spawnTimer;
    private Timeline aiTimer;

    // =========================================================================
    // Construction & initialisation
    // =========================================================================

    public GameEngine() {
        map = new GridMap(20, 15);
        map.buildBorderWalls();

        // Hero STR is random 8-15 (spec §2.4.1)
        int str = 8 + rng.nextInt(8);
        hero = new Hero("Hero", new Vec2(5, 5), str);

        populateTestMap();
        initTimers();
    }

    /**
     * Build a small demonstrable map:
     * – a few wall segments to test collision
     * – a key (passable item) the hero can walk over and pick up
     * – a crate (blocking) the hero cannot pass through
     * – a red potion to demonstrate stat change
     */
    private void populateTestMap() {
        // Internal wall segment (column 8, rows 3-7)
        for (int r = 3; r <= 7; r++) {
            map.setTile(new Vec2(8, r), TileType.WALL);
        }

        // Crate at (12, 7) – blocks movement
        map.placeObject(GameObjects.crate(new Vec2(12, 7)));

        // Key at (6, 5) – passable, hero starts at (5,5) and can walk onto it
        map.placeObject(GameObjects.key(new Vec2(6, 5)));

        // Gem at (3, 8)
        map.placeObject(GameObjects.gem(new Vec2(3, 8)));

        // Red potion at (7, 5) – TAKE + drink to restore HP
        map.placeObject(GameObjects.redPotion(new Vec2(7, 5)));
    }

    private void initTimers() {
        // Enemy spawn every 9 seconds (spec §2.5)
        spawnTimer = new Timeline(new KeyFrame(Duration.seconds(SPAWN_INTERVAL_S), e -> spawnEnemy()));
        spawnTimer.setCycleCount(Animation.INDEFINITE);

        // Enemy AI tick every 1 second (design decision: readable in the demo)
        aiTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickEnemies()));
        aiTimer.setCycleCount(Animation.INDEFINITE);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public void start() {
        spawnTimer.play();
        aiTimer.play();
    }

    public void pause() {
        spawnTimer.pause();
        aiTimer.pause();
    }

    public void resume() {
        spawnTimer.play();
        aiTimer.play();
    }

    // =========================================================================
    // Hero movement (spec §1.1)
    // =========================================================================

    /**
     * Attempt to move the hero one cell in direction (dc, dr).
     * Blocks on WALL base tiles and STATIC GameObjects.
     * Passes through ITEM tiles.
     * Drains ENERGY_COST_WALK energy (spec §2.4.1).
     */
    public boolean moveHero(int dc, int dr) {
        Vec2 target = hero.getPos().add(dc, dr);

        if (!map.isPassable(target)) {
            postMessage("Blocked!");
            return false;
        }

        hero.setPos(target);
        hero.spendEnergy(Hero.ENERGY_COST_WALK);
        notifyStatsChanged();
        notifyMapChanged();
        return true;
    }

    // =========================================================================
    // 3×3 Interaction (spec §1.2)
    // =========================================================================

    /**
     * Returns all GameObjects in the 3×3 area centred on the hero,
     * excluding the hero's own tile if you want only neighbours –
     * but the spec says "next to the player" so we include all 8+centre.
     */
    public List<GameObject> getInteractableObjects() {
        List<GameObject> result = new ArrayList<>();
        Vec2 h = hero.getPos();
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                Vec2 cell = h.add(dc, dr);
                if (cell.inBounds(map.getCols(), map.getRows())) {
                    result.addAll(map.objectsAt(cell));
                }
            }
        }
        return result;
    }

    /**
     * Returns the actions available for a given object,
     * or empty list if the hero is NOT within the 3×3 area (spec §1.2).
     */
    public List<Action> getActionsFor(GameObject obj) {
        if (!map.isAdjacent(hero.getPos(), obj.getPos())) {
            return Collections.emptyList();
        }
        return obj.getActions();

        // Inventory items are always accessible; inventory actions handled in UI
        // directly.
    }

    /** Execute the chosen action on the object. */
    public void executeAction(Action action, GameObject subject) {
        action.execute(this, subject);
    }

    // =========================================================================
    // Enemy spawning (spec §2.5)
    // =========================================================================
    /**
     * Spawns a new enemy on a valid edge floor cell based on spawn probabilities.
     *
     * Requires:
     * - rng, map, hero, and enemies are initialized
     * - SPAWN_PROBABILITY_KNIGHT + SPAWN_PROBABILITY_SORCERER <= 1
     *
     * Modifies:
     * - enemies
     * - enemyIdCounter
     * - game messages
     *
     * Effects:
     * - may create and add a new enemy if spawn conditions are satisfied
     * - does not spawn an enemy if:
     * - random roll exceeds spawn probabilities
     * - maximum number of enemies already exists
     * - no valid spawn positions are available
     * - posts status messages
     * - notifies observers when map changes
     */
    // For testing purposes its not private now. In original code this function is
    // private
    void spawnEnemy() {
        double roll = rng.nextDouble();

        if (roll > SPAWN_PROBABILITY_KNIGHT + SPAWN_PROBABILITY_SORCERER) {
            postMessage("[Spawn] No new enemy this cycle.");
            return;
        }
        if (enemies.size() >= MAX_ENEMIES) {
            postMessage("[Spawn] Max enemies reached, skipping spawn.");
            return;
        }

        Enemy.Type type = roll < SPAWN_PROBABILITY_KNIGHT ? Enemy.Type.KNIGHT : Enemy.Type.SORCERER;

        List<Vec2> candidates = map.edgeFloorCells();
        if (candidates.isEmpty())
            return;

        // Filter out cells occupied by the hero or another enemy
        Vec2 heroPos = hero.getPos();
        Set<Vec2> occupied = new HashSet<>();
        occupied.add(heroPos);
        enemies.forEach(e -> occupied.add(e.getPos()));
        candidates.removeIf(occupied::contains);
        if (candidates.isEmpty())
            return;

        Vec2 spawnPos = candidates.get(rng.nextInt(candidates.size()));
        String id = String.valueOf(++enemyIdCounter);
        Enemy enemy = new Enemy(id, type, spawnPos);
        enemies.add(enemy);

        String msg = "[Spawn] " + type + " #" + id + " spawned at " + spawnPos;
        System.out.println(msg);
        postMessage(msg);
        notifyMapChanged();
    }

    // =========================================================================
    // Enemy AI tick
    // =========================================================================

    private void tickEnemies() {
        StringBuilder log = new StringBuilder();
        for (Enemy e : enemies) {
            String status = e.tick(hero.getPos(), map);
            log.append(status).append("\n");
        }
        if (onAiLog != null && !enemies.isEmpty()) {
            onAiLog.accept(log.toString().trim());
        }
        notifyMapChanged();
    }

    // =========================================================================
    // UI notification helpers
    // =========================================================================

    public void notifyMapChanged() {
        if (onMapChanged != null)
            onMapChanged.run();
    }

    public void notifyStatsChanged() {
        if (onStatsChanged != null)
            onStatsChanged.run();
    }

    public void postMessage(String msg) {
        System.out.println("[Engine] " + msg);
        if (onMessage != null)
            onMessage.accept(msg);
    }

    // ---------- Callback registration ----------

    public void setOnMapChanged(Runnable r) {
        onMapChanged = r;
    }

    public void setOnStatsChanged(Runnable r) {
        onStatsChanged = r;
    }

    public void setOnMessage(Consumer<String> c) {
        onMessage = c;
    }

    public void setOnAiLog(Consumer<String> c) {
        onAiLog = c;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public GridMap getMap() {
        return map;
    }

    public Hero getHero() {
        return hero;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    // TESTERS

    public void setRandom(Random random) {
        this.rng = random;
    }

    public int getMaxEnemies() {
        return MAX_ENEMIES;
    }

    public void addEnemyForTest(Enemy enemy) {
        enemies.add(enemy);
    }

    public boolean repOk() {

        if (map == null)
            return false;

        if (hero == null)
            return false;

        if (enemies == null)
            return false;

        if (rng == null)
            return false;

        if (enemies.size() > MAX_ENEMIES)
            return false;

        for (Enemy e : enemies) {
            if (e == null)
                return false;
        }

        return true;
    }
}
