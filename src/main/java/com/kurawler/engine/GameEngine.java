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

    public enum Mode {
        PLAY, TEAM_MATCH
    }

    // ── spec constants ──
    private static final int SPAWN_INTERVAL_S = 9;
    private static final int MAX_ENEMIES = 5;
    private static final double SPAWN_KNIGHT_PROB = 0.60;
    private static final double SPAWN_SORCERER_PROB = 0.30;
    private static final int TEAM_RED = 1;
    private static final int TEAM_BLUE = 2;

    // ── state ──
    private final GridMap map;
    private final Hero hero;
    private final Mode mode;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Random rng = new Random();
    private int enemyIdCounter = 0;
    private boolean gameOver = false;
    private boolean victory = false;
    private String victoryMessage = "";
    private Vec2 shadowClonePos = null;

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
    private Timeline shadowScrollTimer;
    private Timeline shadowCloneTimer;
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
        this(heroName, existingMap, Mode.PLAY);
    }

    public GameEngine(String heroName, GridMap existingMap, Mode mode) {
        this.mode = mode;
        int str = 8 + rng.nextInt(8);
        hero = new Hero(heroName, new Vec2(5, 5), str);

        if (existingMap != null) {
            map = existingMap;
        } else {
            map = MapGenerator.generate(20, 15);
        }
        if (isTeamMatch()) {
            setupTeamMatch();
        } else {
            chooseTargetRelic();
            hideRelicInMap();
        }
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

    private void setupTeamMatch() {
        targetRelicName = "Team Match";
        removeMapWeapons();
        placeTeam(TEAM_RED, 0, 4, Enemy.Type.SORCERER, Enemy.Type.KNIGHT, Enemy.Type.KNIGHT, Enemy.Type.KNIGHT);
        placeTeam(TEAM_BLUE, map.getCols() / 2, 3, Enemy.Type.SORCERER, Enemy.Type.KNIGHT, Enemy.Type.KNIGHT);
        hero.setPos(randomFreeCell(map.getCols() / 2, map.getCols()));
        placeTeamWeapons();
        victoryMessage = "";
        postMessage("Team Match started: Blue team vs Red team.");
    }

    private void placeTeam(int team, int minCol, int count, Enemy.Type... types) {
        for (int i = 0; i < count; i++) {
            Vec2 pos = randomFreeCell(minCol, team == TEAM_RED ? map.getCols() / 2 : map.getCols());
            Enemy enemy = new Enemy(String.valueOf(++enemyIdCounter), types[i], pos);
            enemy.setTeam(team);
            enemies.add(enemy);
        }
    }

    private void placeTeamWeapons() {
        String[] names = {
                "Training Blade", "Skirmish Dagger", "Ranger Bow",
                "Knight Sword", "War Axe", "Champion Greatsword"
        };
        int[] atk = { 3, 4, 5, 6, 8, 10 };
        int[] sc = { 0, 1, 8, 0, 2, 0 };
        int[] sr = { 0, 0, 9, 0, 0, 4 };
        for (int i = 0; i < names.length; i++) {
            Vec2 pos = randomFreeCell(0, map.getCols());
            map.placeObject(GameObjects.teamWeapon(names[i], pos, atk[i], sc[i], sr[i]));
        }
    }

    private void removeMapWeapons() {
        List<GameObject> weapons = new ArrayList<>();
        for (var list : map.allObjects())
            for (GameObject obj : list)
                if ("WEAPON".equals(obj.renderTag()))
                    weapons.add(obj);
        weapons.forEach(map::removeObject);
    }

    private Vec2 randomFreeCell(int minCol, int maxCol) {
        List<Vec2> cells = map.allFloorCells();
        cells.removeIf(v -> v.col() < minCol || v.col() >= maxCol || isOccupiedByCharacter(v));
        if (cells.isEmpty()) {
            cells = map.allFloorCells();
            cells.removeIf(this::isOccupiedByCharacter);
        }
        return cells.get(rng.nextInt(cells.size()));
    }

    private boolean isOccupiedByCharacter(Vec2 pos) {
        if (hero.getPos().equals(pos))
            return true;
        if (shadowClonePos != null && shadowClonePos.equals(pos))
            return true;
        for (Enemy enemy : enemies)
            if (enemy.getPos().equals(pos))
                return true;
        return false;
    }

    public String getTargetRelicName() {
        return targetRelicName;
    }

    public boolean isTeamMatch() {
        return mode == Mode.TEAM_MATCH;
    }

    public String getVictoryMessage() {
        return victoryMessage;
    }

    public boolean hasShadowClone() {
        return shadowClonePos != null;
    }

    public Vec2 getShadowClonePos() {
        return shadowClonePos;
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

        shadowScrollTimer = new Timeline(new KeyFrame(Duration.seconds(15), e -> spawnShadowCloneScroll()));
        shadowScrollTimer.setCycleCount(Animation.INDEFINITE);

        shadowCloneTimer = new Timeline(new KeyFrame(Duration.seconds(7), e -> dismissShadowClone()));
        shadowCloneTimer.setCycleCount(1);
    }

    public void start() {
        if (!isTeamMatch())
            spawnTimer.play();
        aiTimer.play();
        knightAttackTimer.play();
        shadowScrollTimer.play();
    }

    public void pause() {
        spawnTimer.pause();
        aiTimer.pause();
        knightAttackTimer.pause();
        shadowScrollTimer.pause();
        shadowCloneTimer.pause();
    }

    public void resume() {
        if (!isTeamMatch())
            spawnTimer.play();
        aiTimer.play();
        knightAttackTimer.play();
        shadowScrollTimer.play();
        if (shadowClonePos != null)
            shadowCloneTimer.play();
    }

    public void stop() {
        spawnTimer.stop();
        aiTimer.stop();
        knightAttackTimer.stop();
        shadowScrollTimer.stop();
        shadowCloneTimer.stop();
    }

    // =========================================================================
    // Hero movement (spec §1.1)
    // =========================================================================
    public boolean moveHero(int dc, int dr) {
        if (gameOver || !hero.isAlive())
            return false;
        Vec2 target = hero.getPos().add(dc, dr);
        if (!map.isPassable(target)) {
            postMessage("Blocked!");
            return false;
        }
        hero.setPos(target);
        moveShadowClone(-dc, -dr);
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
        if (isTeamMatch() && enemy.getTeam() != TEAM_RED) {
            postMessage("That character is on your team.");
            return;
        }
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
            checkTeamMatchVictory();
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
        if (isTeamMatch()) {
            tickTeamMatchAI();
            return;
        }
        StringBuilder log = new StringBuilder();
        List<Enemy> toRemove = new ArrayList<>();

        for (Enemy e : enemies) {
            Vec2 target = nearestHeroLikeTarget(e.getPos());
            String status = e.tick(target, map);
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
            if (shadowClonePos != null && p.getPos().equals(shadowClonePos)) {
                postMessage("Projectile passes through the shadow clone.");
                p.destroy();
                deadProjectiles.add(p);
                continue;
            }
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

    private void tickTeamMatchAI() {
        StringBuilder log = new StringBuilder();
        List<Enemy> dead = new ArrayList<>();

        for (Enemy e : new ArrayList<>(enemies)) {
            if (!e.isAlive())
                continue;

            if (e.getType() == Enemy.Type.KNIGHT && !e.hasWeaponEquipped()) {
                moveKnightTowardWeapon(e);
                log.append(teamName(e.getTeam())).append(" knight #").append(e.getId()).append(" seeks weapon\n");
                continue;
            }

            CharacterTarget target = nearestEnemyTarget(e);
            if (target == null)
                continue;

            if (e.isAdjacentTo(target.pos())) {
                if (target.enemy() != null) {
                    int dmg = CombatSystem.enemyAttackEnemy(e, target.enemy());
                    postMessage(teamName(e.getTeam()) + " " + e.getType() + " #" + e.getId()
                            + " hits " + teamName(target.enemy().getTeam()) + " #" + target.enemy().getId()
                            + " for " + dmg + ".");
                    if (!target.enemy().isAlive())
                        dead.add(target.enemy());
                } else if (target.hero()) {
                    int dmg = CombatSystem.enemyAttackHero(e, hero);
                    postMessage("Red " + e.getType() + " #" + e.getId() + " hits the hero for " + dmg + ".");
                    checkDeath();
                }
            } else if (e.getType() == Enemy.Type.SORCERER) {
                String status = e.tick(target.pos(), map);
                log.append(status).append("\n");
                Vec2 projTarget = e.consumePendingProjectile();
                if (projTarget != null) {
                    projectiles.add(new Projectile(e.getPos(), projTarget, e));
                    postMessage(teamName(e.getTeam()) + " sorcerer #" + e.getId() + " fires!");
                }
            } else {
                Vec2 next = e.stepTowardTarget(target.pos(), map);
                if (next != null && map.isPassable(next) && !isOccupiedByCharacter(next))
                    e.setPos(next);
                log.append(teamName(e.getTeam())).append(" knight #").append(e.getId()).append(" advances\n");
            }
        }

        enemies.removeAll(dead);
        advanceTeamProjectiles();
        checkTeamMatchVictory();
        if (onAiLog != null)
            onAiLog.accept(log.toString().trim());
        notifyMapChanged();
    }

    private record CharacterTarget(Vec2 pos, Enemy enemy, boolean hero) {
    }

    private CharacterTarget nearestEnemyTarget(Enemy actor) {
        CharacterTarget best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Enemy other : enemies) {
            if (other == actor || other.getTeam() == actor.getTeam() || !other.isAlive())
                continue;
            int d = manhattan(actor.getPos(), other.getPos());
            if (d < bestDist) {
                bestDist = d;
                best = new CharacterTarget(other.getPos(), other, false);
            }
        }
        if (actor.getTeam() == TEAM_RED && hero.isAlive()) {
            int d = manhattan(actor.getPos(), hero.getPos());
            if (d < bestDist)
                best = new CharacterTarget(hero.getPos(), null, true);
        }
        return best;
    }

    private Vec2 nearestHeroLikeTarget(Vec2 from) {
        if (shadowClonePos == null)
            return hero.getPos();
        return manhattan(from, shadowClonePos) < manhattan(from, hero.getPos()) ? shadowClonePos : hero.getPos();
    }

    private void moveKnightTowardWeapon(Enemy knight) {
        GameObject weapon = nearestMapWeapon(knight.getPos());
        if (weapon == null)
            return;
        if (knight.getPos().equals(weapon.getPos())) {
            knight.equipWeapon(GameObjects.getWeaponAtk(weapon));
            map.removeObject(weapon);
            postMessage(teamName(knight.getTeam()) + " knight #" + knight.getId()
                    + " picked up " + weapon.getName() + ".");
            return;
        }
        Vec2 next = knight.stepTowardTarget(weapon.getPos(), map);
        if (next != null && map.isPassable(next) && !isOccupiedByCharacter(next))
            knight.setPos(next);
        List<GameObject> here = new ArrayList<>(map.objectsAt(knight.getPos()));
        for (GameObject obj : here) {
            if ("WEAPON".equals(obj.renderTag())) {
                knight.equipWeapon(GameObjects.getWeaponAtk(obj));
                map.removeObject(obj);
                postMessage(teamName(knight.getTeam()) + " knight #" + knight.getId()
                        + " picked up " + obj.getName() + ".");
                break;
            }
        }
    }

    private GameObject nearestMapWeapon(Vec2 from) {
        GameObject best = null;
        int bestDist = Integer.MAX_VALUE;
        for (var list : map.allObjects()) {
            for (GameObject obj : list) {
                if (!"WEAPON".equals(obj.renderTag()))
                    continue;
                int d = manhattan(from, obj.getPos());
                if (d < bestDist) {
                    bestDist = d;
                    best = obj;
                }
            }
        }
        return best;
    }

    private void advanceTeamProjectiles() {
        List<Projectile> deadProjectiles = new ArrayList<>();
        List<Enemy> deadEnemies = new ArrayList<>();
        for (Projectile p : projectiles) {
            p.advance(map);
            if (!p.isActive()) {
                deadProjectiles.add(p);
                continue;
            }
            Enemy source = p.getSource();
            if (source.getTeam() == TEAM_RED && p.getPos().equals(hero.getPos())) {
                int dmg = CombatSystem.projectileHitsHero(hero);
                postMessage("Projectile hit the hero for " + dmg + " damage!");
                p.destroy();
                deadProjectiles.add(p);
                notifyStatsChanged();
                checkDeath();
                continue;
            }
            for (Enemy target : enemies) {
                if (target.getTeam() == source.getTeam() || !target.isAlive())
                    continue;
                if (p.getPos().equals(target.getPos())) {
                    int dmg = CombatSystem.enemyAttackEnemy(source, target);
                    postMessage("Projectile hit " + teamName(target.getTeam()) + " #" + target.getId()
                            + " for " + dmg + ".");
                    if (!target.isAlive())
                        deadEnemies.add(target);
                    p.destroy();
                    deadProjectiles.add(p);
                    break;
                }
            }
        }
        enemies.removeAll(deadEnemies);
        projectiles.removeAll(deadProjectiles);
    }

    private void checkTeamMatchVictory() {
        if (!isTeamMatch() || gameOver)
            return;
        int redAlive = countTeamAlive(TEAM_RED);
        int blueAlive = countTeamAlive(TEAM_BLUE) + (hero.isAlive() ? 1 : 0);
        if (redAlive == 0 || blueAlive == 0) {
            victory = true;
            gameOver = true;
            victoryMessage = (redAlive == 0 ? "Blue team" : "Red team") + " has won the match!";
            postMessage(victoryMessage);
            stop();
            if (onVictory != null)
                onVictory.run();
        }
    }

    private int countTeamAlive(int team) {
        int count = 0;
        for (Enemy enemy : enemies)
            if (enemy.getTeam() == team && enemy.isAlive())
                count++;
        return count;
    }

    private int manhattan(Vec2 a, Vec2 b) {
        return Math.abs(a.col() - b.col()) + Math.abs(a.row() - b.row());
    }

    private String teamName(int team) {
        return team == TEAM_BLUE ? "Blue" : "Red";
    }

    // =========================================================================
    // Knight melee attacks
    // =========================================================================
    private void knightMeleeAttacks() {
        if (gameOver || isTeamMatch())
            return;
        for (Enemy e : enemies) {
            if (e.getType() != Enemy.Type.KNIGHT)
                continue;
            if (shadowClonePos != null && e.isAdjacentTo(shadowClonePos)
                    && manhattan(e.getPos(), shadowClonePos) <= manhattan(e.getPos(), hero.getPos())) {
                postMessage("Knight #" + e.getId() + " strikes the shadow clone.");
                continue;
            }
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
        if (isTeamMatch()) {
            if (!hero.isAlive()) {
                postMessage("Blue hero has been eliminated!");
                checkTeamMatchVictory();
            }
            return;
        }
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
    // Shadow Clone
    // =========================================================================
    private void spawnShadowCloneScroll() {
        if (gameOver)
            return;
        List<Vec2> cells = map.allFloorCells();
        cells.removeIf(this::isOccupiedByCharacter);
        if (cells.isEmpty())
            return;
        Vec2 pos = cells.get(rng.nextInt(cells.size()));
        map.placeObject(GameObjects.shadowCloneScroll(pos));
        postMessage("A Shadow Clone scroll appears.");
        notifyMapChanged();
    }

    public boolean activateShadowClone() {
        if (shadowClonePos != null) {
            postMessage("A shadow clone is already active.");
            return false;
        }
        for (Vec2 cell : adjacentCells(hero.getPos())) {
            if (map.isPassable(cell) && !isOccupiedByCharacter(cell)) {
                shadowClonePos = cell;
                shadowCloneTimer.stop();
                shadowCloneTimer.playFromStart();
                postMessage("Shadow clone summoned.");
                notifyMapChanged();
                return true;
            }
        }
        postMessage("No empty adjacent tile for the shadow clone.");
        return false;
    }

    private void dismissShadowClone() {
        if (shadowClonePos != null) {
            shadowClonePos = null;
            postMessage("Shadow clone disappears.");
            notifyMapChanged();
        }
    }

    private void moveShadowClone(int dc, int dr) {
        if (shadowClonePos == null)
            return;
        Vec2 target = shadowClonePos.add(dc, dr);
        if (map.isPassable(target) && !isOccupiedByCharacter(target))
            shadowClonePos = target;
    }

    private List<Vec2> adjacentCells(Vec2 origin) {
        List<Vec2> cells = new ArrayList<>();
        for (int dc = -1; dc <= 1; dc++)
            for (int dr = -1; dr <= 1; dr++)
                if (dc != 0 || dr != 0)
                    cells.add(origin.add(dc, dr));
        Collections.shuffle(cells, rng);
        return cells;
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
