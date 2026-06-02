package com.kurawler.engine;

import com.kurawler.game.action.Action;
import com.kurawler.game.entity.*;
import com.kurawler.game.objects.*;

import javafx.animation.*;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Central game engine (spec §1–4).
 *
 * Owns: GridMap, Hero, enemies, projectiles, timers.
 * Notifies the UI layer via callback lambdas.
 */
public class GameEngine {

    // ── spec constants ──
    private static final int SPAWN_INTERVAL_S = 9;
    private static final int MAX_ENEMIES = 5;
    private static final double SPAWN_KNIGHT_PROB = 0.60;
    private static final double SPAWN_SORCERER_PROB = 0.30;

    // ── state ──
    private final GridMap map;
    private final Hero hero;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Random rng = new Random();
    private int enemyIdCounter = 0;
    private boolean gameOver = false;
    private boolean victory = false;

    // ── target relic (spec §4.1) ──
    private String targetRelicName = null;

    // ── UI callbacks ──
    private Runnable onMapChanged;
    private Runnable onStatsChanged;
    private Runnable onInventoryChanged;
    private Consumer<String> onMessage;
    private Consumer<String> onAiLog;
    private Runnable onGameOver;
    private Runnable onVictory;

    // ── timers ──
    private Timeline spawnTimer;
    private Timeline aiTimer;
    private Timeline knightAttackTimer;
    private int knightAttackCooldown = 0;

    // =========================================================================
    /** Standard constructor — generates a random map. */
    public GameEngine(String heroName) {
        this(heroName, null);
    }

    /**
     * Constructor that accepts a pre-built GridMap from the editor.
     * If existingMap is null a random map is generated.
     */
    public GameEngine(String heroName, GridMap existingMap) {
        int str = 8 + rng.nextInt(8);
        hero = new Hero(heroName, new Vec2(5, 5), str);

        if (existingMap != null) {
            map = existingMap;
        } else {
            map = MapGenerator.generate(20, 15);
        }
        chooseTargetRelic();
        hideRelicInMap();
        initTimers();
    }

    // ── target relic ──
    private void chooseTargetRelic() {
        String[] relics = { "Crystal Orb", "Golden Ring", "Diamond", "Ancient Key", "Magic Amulet" };
        targetRelicName = relics[rng.nextInt(relics.length)];
    }

    /**
     * Create the relic item and hide it in a container on the map.
     * Called after map generation and relic selection.
     */
    private void hideRelicInMap() {
        // Create the relic as a named game object
        com.kurawler.game.objects.GameObject relicObj = com.kurawler.game.objects.GameObjects.relicItem(targetRelicName,
                new Vec2(0, 0));
        // 60% chance relic chest, 40% chance searchable wall/box
        boolean useChest = rng.nextDouble() < 0.6;
        MapGenerator.placeRelicContainers(map, relicObj, useChest);
    }

    public String getTargetRelicName() {
        return targetRelicName;
    }

    // ── timers ──
    private void initTimers() {
        spawnTimer = new Timeline(new KeyFrame(Duration.seconds(SPAWN_INTERVAL_S), e -> spawnEnemy()));
        spawnTimer.setCycleCount(Animation.INDEFINITE);

        aiTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickAI()));
        aiTimer.setCycleCount(Animation.INDEFINITE);

        // Knights attack every 1.5s if adjacent
        knightAttackTimer = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> knightMeleeAttacks()));
        knightAttackTimer.setCycleCount(Animation.INDEFINITE);
    }

    public void start() {
        spawnTimer.play();
        aiTimer.play();
        knightAttackTimer.play();
    }

    public void pause() {
        spawnTimer.pause();
        aiTimer.pause();
        knightAttackTimer.pause();
    }

    public void resume() {
        spawnTimer.play();
        aiTimer.play();
        knightAttackTimer.play();
    }

    public void stop() {
        spawnTimer.stop();
        aiTimer.stop();
        knightAttackTimer.stop();
    }

    // =========================================================================
    // Hero movement (spec §1.1)
    // =========================================================================
    public boolean moveHero(int dc, int dr) {
        if (gameOver)
            return false;
        Vec2 target = hero.getPos().add(dc, dr);
        if (!map.isPassable(target)) {
            postMessage("Blocked!");
            return false;
        }
        hero.setPos(target);
        hero.spendEnergy(Hero.ENERGY_COST_WALK);
        checkVictoryCondition();
        notifyStatsChanged();
        notifyMapChanged();
        return true;
    }

    // =========================================================================
    // Interaction (spec §1.2)
    // =========================================================================
    public List<Action> getActionsFor(GameObject obj) {
        if (!map.isAdjacent(hero.getPos(), obj.getPos()))
            return Collections.emptyList();
        return obj.getActions();
    }

    public void executeAction(Action action, GameObject subject) {
        action.execute(this, subject);
    }

    // =========================================================================
    // Hero attack on enemy (spec §2.6)
    // =========================================================================
    public void heroAttackEnemy(Enemy enemy) {
        if (!map.isAdjacent(hero.getPos(), enemy.getPos())) {
            postMessage("Too far away to attack!");
            return;
        }
        if (!hero.hasWeaponEquipped()) {
            postMessage("Equip a weapon first!");
            return;
        }
        int dmg = CombatSystem.heroAttack(hero, enemy);
        postMessage("Hit " + enemy.getType() + " for " + dmg + " damage! " +
                "(HP left: " + enemy.getStat(StatType.HP) + ")");
        if (!enemy.isAlive()) {
            enemies.remove(enemy);
            postMessage(enemy.getType() + " #" + enemy.getId() + " defeated!");
        }
        notifyStatsChanged();
        notifyMapChanged();
    }

    // =========================================================================
    // Enemy spawning (spec §2.5)
    // =========================================================================
    private void spawnEnemy() {
        if (gameOver || enemies.size() >= MAX_ENEMIES)
            return;
        double roll = rng.nextDouble();
        if (roll > SPAWN_KNIGHT_PROB + SPAWN_SORCERER_PROB) {
            postMessage("[Spawn] No enemy this cycle.");
            return;
        }
        Enemy.Type type = roll < SPAWN_KNIGHT_PROB ? Enemy.Type.KNIGHT : Enemy.Type.SORCERER;

        List<Vec2> candidates = map.edgeFloorCells();
        candidates.removeIf(v -> v.equals(hero.getPos()));
        enemies.forEach(e -> candidates.remove(e.getPos()));
        if (candidates.isEmpty())
            return;

        Vec2 spawnPos = candidates.get(rng.nextInt(candidates.size()));
        String id = String.valueOf(++enemyIdCounter);
        Enemy enemy = new Enemy(id, type, spawnPos);
        enemies.add(enemy);

        postMessage("[Spawn] " + type + " #" + id + " appeared at " + spawnPos);
        notifyMapChanged();
    }

    // =========================================================================
    // AI tick
    // =========================================================================
    private void tickAI() {
        if (gameOver)
            return;
        StringBuilder log = new StringBuilder();
        List<Enemy> toRemove = new ArrayList<>();

        for (Enemy e : enemies) {
            String status = e.tick(hero.getPos(), map);
            log.append(status).append("\n");

            // Handle sorcerer projectile
            Vec2 projTarget = e.consumePendingProjectile();
            if (projTarget != null) {
                projectiles.add(new Projectile(e.getPos(), projTarget, e));
                postMessage("Sorcerer #" + e.getId() + " fires a projectile!");
            }
        }

        // Advance projectiles
        List<Projectile> deadProjectiles = new ArrayList<>();
        for (Projectile p : projectiles) {
            p.advance(map);
            if (!p.isActive()) {
                deadProjectiles.add(p);
                continue;
            }
            // Check hero collision
            if (p.getPos().equals(hero.getPos())) {
                int dmg = CombatSystem.projectileHitsHero(hero);
                postMessage("Projectile hit you for " + dmg + " damage!");
                p.destroy();
                deadProjectiles.add(p);
                notifyStatsChanged();
                checkDeath();
            }
        }
        projectiles.removeAll(deadProjectiles);

        if (onAiLog != null && !enemies.isEmpty())
            onAiLog.accept(log.toString().trim());
        notifyMapChanged();
    }

    // =========================================================================
    // Knight melee attacks
    // =========================================================================
    private void knightMeleeAttacks() {
        if (gameOver)
            return;
        for (Enemy e : enemies) {
            if (e.getType() != Enemy.Type.KNIGHT)
                continue;
            if (e.isAdjacentTo(hero.getPos())) {
                int dmg = CombatSystem.enemyAttackHero(e, hero);
                postMessage("Knight #" + e.getId() + " hits you for " + dmg + " damage!");
                notifyStatsChanged();
                checkDeath();
            }
        }
    }

    // =========================================================================
    // Win / lose checks
    // =========================================================================
    private void checkDeath() {
        if (!hero.isAlive() && !gameOver) {
            gameOver = true;
            stop();
            postMessage("YOU DIED! Game over.");
            if (onGameOver != null)
                onGameOver.run();
        }
    }

    private void checkVictoryCondition() {
        // Check inventory for the relic item (added when container is searched/opened)
        for (var item : hero.getInventory().all()) {
            if (item.getName().equalsIgnoreCase(targetRelicName)) {
                triggerVictory();
                return;
            }
        }
    }

    private void triggerVictory() {
        if (victory)
            return;
        victory = true;
        gameOver = true;
        stop();
        postMessage("✨ You found the " + targetRelicName + "! VICTORY! ✨");
        if (onVictory != null)
            onVictory.run();
    }

    // Called by inventory actions after adding an item
    public void checkVictoryAfterPickup() {
        checkVictoryCondition();
    }

    // =========================================================================
    // Notifications
    // =========================================================================
    public void notifyMapChanged() {
        if (onMapChanged != null)
            onMapChanged.run();
    }

    public void notifyStatsChanged() {
        if (onStatsChanged != null)
            onStatsChanged.run();
    }

    public void notifyInventoryChanged() {
        if (onInventoryChanged != null)
            onInventoryChanged.run();
        checkVictoryAfterPickup();
    }

    public void postMessage(String msg) {
        System.out.println("[Engine] " + msg);
        if (onMessage != null)
            onMessage.accept(msg);
    }

    // ── callbacks ──
    public void setOnMapChanged(Runnable r) {
        onMapChanged = r;
    }

    public void setOnStatsChanged(Runnable r) {
        onStatsChanged = r;
    }

    public void setOnInventoryChanged(Runnable r) {
        onInventoryChanged = r;
    }

    public void setOnMessage(Consumer<String> c) {
        onMessage = c;
    }

    public void setOnAiLog(Consumer<String> c) {
        onAiLog = c;
    }

    public void setOnGameOver(Runnable r) {
        onGameOver = r;
    }

    public void setOnVictory(Runnable r) {
        onVictory = r;
    }

    // ── accessors ──
    public GridMap getMap() {
        return map;
    }

    public Hero getHero() {
        return hero;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isVictory() {
        return victory;
    }
}
