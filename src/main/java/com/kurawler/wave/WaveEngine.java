package com.kurawler.wave;

import com.kurawler.engine.*;
import com.kurawler.game.entity.*;
import com.kurawler.game.objects.*;
import com.kurawler.model.UserStore;

import javafx.animation.*;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Wave Survival Engine — endless survival with persistent account progression.
 *
 * Key design:
 * - No maximum wave (runs forever until hero dies)
 * - Enemies scale every wave (count, HP, damage, spawn rate)
 * - Boss waves every 5th wave (x3 HP, x2 damage)
 * - Coins written directly to UserStore on every kill
 * - Loadout (weapon/armor/skin) applied at session start from UserStore
 * - Consumables loaded from UserStore; used via 1/2/3 key bindings
 */
public class WaveEngine {

    private static final int COLS = 20, ROWS = 15;

    private final UserStore userStore;
    private final GridMap map;
    private final Hero hero;
    private final WaveDifficulty diff;
    private final WaveState state = new WaveState();

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Set<String> purchasedIds = new HashSet<>();

    private int heroSkinIndex = 0;

    private final Random rng = new Random();
    private int enemyIdCounter = 0;

    // ── Timelines ─────────────────────────────────────────────────────────────
    private Timeline spawnTimer;
    private Timeline aiTimer;
    private Timeline knightTimer;
    private Timeline waveTickTimer;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private Runnable onMapChanged;
    private Runnable onStatsChanged;
    private Runnable onStateChanged;
    private Consumer<String> onMessage;
    private Consumer<Integer> onCoinDrop;
    private Runnable onWaveComplete;
    private Runnable onMarketClose;
    private Runnable onGameOver;
    private Runnable onInventoryChanged;

    private final List<MarketItem> shopInventory = new ArrayList<>(MarketItem.buildCatalogue());

    // =========================================================================
    public WaveEngine(String heroName, WaveDifficulty diff, UserStore userStore) {
        this.userStore = userStore;
        this.diff = diff;
        this.map = MapGenerator.generate(COLS, ROWS);
        int str = 8 + rng.nextInt(8);
        this.hero = new Hero(heroName, new Vec2(5, 5), str);
        applyLoadout(heroName);
        initTimers();
    }

    // ── Timers ────────────────────────────────────────────────────────────────
    private void initTimers() {
        spawnTimer = new Timeline(new KeyFrame(Duration.seconds(diff.spawnIntervalSeconds), e -> spawnNextEnemy()));
        spawnTimer.setCycleCount(Animation.INDEFINITE);
        aiTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickAI()));
        aiTimer.setCycleCount(Animation.INDEFINITE);
        knightTimer = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> knightMeleeCheck()));
        knightTimer.setCycleCount(Animation.INDEFINITE);
        waveTickTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> tickTimers()));
        waveTickTimer.setCycleCount(Animation.INDEFINITE);
    }

    public void startSession() {
        waveTickTimer.play();
        startNextWave();
    }

    private void pauseGameTimers() {
        spawnTimer.pause();
        aiTimer.pause();
        knightTimer.pause();
    }

    private void resumeGameTimers() {
        spawnTimer.play();
        aiTimer.play();
        knightTimer.play();
    }

    public void pause() {
        pauseGameTimers();
        waveTickTimer.pause();
    }

    public void resume() {
        if (state.isWaveActive())
            resumeGameTimers();
        waveTickTimer.play();
    }

    public void stop() {
        spawnTimer.stop();
        aiTimer.stop();
        knightTimer.stop();
        waveTickTimer.stop();
    }

    // ── Wave lifecycle ────────────────────────────────────────────────────────
    private void startNextWave() {
        int wave = state.getCurrentWave() + 1;
        // Endless scaling: 5 + wave*3, no cap
        int count = 5 + (wave * 3);
        state.startWave(wave, count);

        boolean isBoss = (wave % 5 == 0);
        post("=== WAVE " + wave + (isBoss ? " [BOSS WAVE!]" : "") + " BEGINS! === (" + count + " enemies)");

        applyWaveReward();

        // Spawn rate accelerates each wave, floor at 0.25s
        double spawnRate = Math.max(0.25, diff.spawnIntervalSeconds - wave * 0.08);
        spawnTimer.stop();
        spawnTimer = new Timeline(new KeyFrame(Duration.seconds(spawnRate), e -> spawnNextEnemy()));
        spawnTimer.setCycleCount(Animation.INDEFINITE);
        spawnTimer.play();
        aiTimer.play();
        knightTimer.play();
        notifyState();
    }

    private void applyWaveReward() {
        int wave = state.getCurrentWave();
        if (wave > 1) {
            hero.modifyStat(StatType.HP, diff.heroHpBonus);
            hero.modifyStat(StatType.ENERGY, diff.heroEnergyBonus);
            if (wave % 3 == 0 && diff.heroStrBonus > 0) {
                hero.modifyStat(StatType.STR, diff.heroStrBonus);
                post("STR +" + diff.heroStrBonus + "!");
            }
            notifyStats();
        }
    }

    // ── Enemy spawning ────────────────────────────────────────────────────────
    private void spawnNextEnemy() {
        if (!state.isWaveActive())
            return;
        if (state.getEnemiesSpawnedMut() >= state.getEnemiesThisWave()) {
            spawnTimer.pause();
            return;
        }
        int maxOnField = Math.min(50, 5 + state.getCurrentWave());
        if (enemies.size() >= maxOnField)
            return;

        List<Vec2> candidates = map.edgeFloorCells();
        candidates.removeIf(v -> v.equals(hero.getPos()));
        enemies.forEach(e -> candidates.remove(e.getPos()));
        if (candidates.isEmpty())
            return;

        Vec2 pos = candidates.get(rng.nextInt(candidates.size()));
        Enemy.Type type = rng.nextDouble() < 0.6 ? Enemy.Type.KNIGHT : Enemy.Type.SORCERER;
        String id = String.valueOf(++enemyIdCounter);
        enemies.add(buildScaledEnemy(id, type, pos));
        state.incrementSpawned();
        notifyMap();
        notifyState();
    }

    private Enemy buildScaledEnemy(String id, Enemy.Type type, Vec2 pos) {
        int wave = state.getCurrentWave();
        Enemy base = new Enemy(id, type, pos);
        int hp = base.getStat(StatType.HP) + wave * 4;
        int str = base.getStat(StatType.STR) + wave / 2;
        // Boss wave: massively stronger
        if (wave % 5 == 0) {
            hp += wave * 10;
            str += wave;
        }
        return new Enemy(id, type, pos, hp, str);
    }

    // ── AI tick ───────────────────────────────────────────────────────────────
    private void tickAI() {
        if (!state.isWaveActive() && enemies.isEmpty())
            return;

        for (Enemy e : enemies) {
            e.tick(hero.getPos(), map);
            Vec2 proj = e.consumePendingProjectile();
            if (proj != null)
                projectiles.add(new Projectile(e.getPos(), proj, e));
        }

        List<Projectile> dead = new ArrayList<>();
        for (Projectile p : projectiles) {
            if (!p.isActive()) {
                dead.add(p);
                continue;
            }
            p.advance(map);
            if (!p.isActive()) {
                dead.add(p);
                continue;
            }
            if (p.getPos().equals(hero.getPos())) {
                double mult = diff.enemyDmgScale(state.getCurrentWave());
                int raw = CombatSystem.projectileHitsHero(hero);
                int dmg = (int) (raw * mult);
                hero.modifyStat(StatType.HP, -Math.max(0, dmg - raw)); // net extra
                state.breakStreak();
                post("Projectile hit! -" + dmg + " HP");
                p.destroy();
                dead.add(p);
                notifyStats();
                checkDeath();
            }
        }
        projectiles.removeAll(dead);
        notifyMap();
    }

    // ── Knight melee ──────────────────────────────────────────────────────────
    private void knightMeleeCheck() {
        if (!state.isWaveActive())
            return;
        for (Enemy e : enemies) {
            if (e.getType() == Enemy.Type.KNIGHT && e.isAdjacentTo(hero.getPos())) {
                int raw = (int) (e.generateDamage() * diff.enemyDmgScale(state.getCurrentWave()));
                int dmg = Math.max(1, raw - hero.getStat(StatType.DEF));
                hero.modifyStat(StatType.HP, -dmg);
                state.breakStreak();
                post("Knight hits you for " + dmg + "!");
                notifyStats();
                checkDeath();
            }
        }
    }

    // ── Hero movement ─────────────────────────────────────────────────────────
    public boolean moveHero(int dc, int dr) {
        if (state.isSessionOver())
            return false;
        Vec2 target = hero.getPos().add(dc, dr);
        if (!map.isPassable(target)) {
            post("Blocked!");
            return false;
        }
        hero.setPos(target);
        hero.spendEnergy(Hero.ENERGY_COST_WALK);
        notifyStats();
        notifyMap();
        return true;
    }

    // ── Hero attacks enemy ────────────────────────────────────────────────────
    public void heroAttackEnemy(Enemy enemy) {
        if (!map.isAdjacent(hero.getPos(), enemy.getPos())) {
            post("Too far!");
            return;
        }
        if (!hero.hasWeaponEquipped()) {
            post("No weapon equipped!");
            return;
        }
        int dmg = CombatSystem.heroAttack(hero, enemy);
        post("Hit " + enemy.getType() + " for " + dmg + "!");
        if (!enemy.isAlive())
            onEnemyKilled(enemy);
        notifyStats();
        notifyMap();
    }

    private void onEnemyKilled(Enemy enemy) {
        enemies.remove(enemy);
        int wave = state.getCurrentWave();
        int score = diff.killScore(wave);
        int coins = state.recordKill(score, diff);
        // Extra bonus coin per wave
        coins += wave;
        // Write coins immediately to persistent store
        userStore.addCoins(hero.getName(), coins);

        String msg = enemy.getType() + " defeated! +" + score + " pts";
        if (state.getCurrentStreak() > 1)
            msg += " (x" + state.getCurrentStreak() + " streak!)";
        if (coins > 0)
            msg += "  \uD83E\uDE99+" + coins;
        post(msg);

        if (coins > 0 && onCoinDrop != null)
            onCoinDrop.accept(coins);
        notifyState();

        // Wave complete check — all spawned AND all dead
        if (state.getEnemiesSpawnedMut() >= state.getEnemiesThisWave() && enemies.isEmpty()) {
            waveComplete();
        }
    }

    private void waveComplete() {
        pauseGameTimers();
        state.setWaveActive(false);
        enemies.clear();
        projectiles.clear();
        post("=== WAVE " + state.getCurrentWave() + " COMPLETE! ===  Shop opens for " + (int) diff.marketBreakSeconds
                + "s");
        state.openMarket(diff.marketBreakSeconds);
        notifyState();
        if (onWaveComplete != null)
            onWaveComplete.run();
    }

    // ── Timer ticks ───────────────────────────────────────────────────────────
    private void tickTimers() {
        if (state.isWaveActive())
            state.tickWaveTimer(0.1);
        if (state.isMarketOpen()) {
            state.tickMarketTimer(0.1);
            if (state.getMarketTimerSec() <= 0) {
                state.closeMarket();
                if (onMarketClose != null)
                    onMarketClose.run();
                startNextWave();
            }
        }
        notifyState();
    }

    // ── Consumable use (key 1/2/3) ────────────────────────────────────────────
    /**
     * Use the Nth consumable in the player's stash (1-indexed).
     * Returns true if something was consumed.
     */
    public boolean useConsumable(int slot) {
        String heroName = hero.getName();
        Map<String, Integer> stash = userStore.getAllConsumables(heroName);
        List<String> keys = new ArrayList<>(stash.keySet());
        if (slot < 1 || slot > keys.size())
            return false;

        String id = keys.get(slot - 1);
        if (!userStore.useConsumable(heroName, id))
            return false;

        // Apply effect
        MarketItem template = shopInventory.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst().orElse(null);
        if (template == null)
            return false;

        int v = template.getEffectValue();
        switch (template.getEffectType()) {
            case RESTORE_HP -> hero.modifyStat(StatType.HP, v);
            case RESTORE_MANA -> hero.modifyStat(StatType.MANA, v);
            case RESTORE_ENERGY -> hero.modifyStat(StatType.ENERGY, v);
            default -> {
            }
        }
        post("Used " + template.getName() + " (+" + v + " " + template.getEffectType().name().replace("RESTORE_", "")
                + ")");
        notifyStats();
        if (onInventoryChanged != null)
            onInventoryChanged.run();
        return true;
    }

    // ── Marketplace ───────────────────────────────────────────────────────────
    public List<MarketItem> getShopInventory() {
        return Collections.unmodifiableList(shopInventory);
    }

    public boolean canAfford(MarketItem item) {
        return userStore.getCoins(hero.getName()) >= item.getPrice();
    }

    public boolean purchase(MarketItem item) {
        String heroName = hero.getName();
        boolean ok = userStore.buyItem(heroName, item.getId(), item.getPrice(), item.isConsumable());
        if (!ok)
            return false;

        if (!item.isConsumable()) {
            // Auto-equip permanent gear
            switch (item.getCategory()) {
                case WEAPON -> userStore.equipWeapon(heroName, item.getId());
                case ARMOR -> userStore.equipArmor(heroName, item.getId());
                case SKIN -> userStore.equipSkin(heroName, item.getId());
                default -> applyPermanentEffect(item);
            }
        }
        // Apply in-session effect for consumables and power-ups
        if (item.getCategory() == MarketItem.Category.POWER_UP)
            applyPermanentEffect(item);

        purchasedIds.add(item.getId());
        post("Purchased: " + item.getName());
        notifyStats();
        notifyState();
        if (onInventoryChanged != null)
            onInventoryChanged.run();
        return true;
    }

    private void applyPermanentEffect(MarketItem item) {
        int v = item.getEffectValue();
        switch (item.getEffectType()) {
            case BOOST_MAX_HP -> raiseStat(StatType.HP, v);
            case BOOST_STR -> raiseStat(StatType.STR, v);
            case BOOST_DEF -> raiseStat(StatType.DEF, v);
            case BOOST_MAX_ENERGY -> raiseStat(StatType.ENERGY, v);
            default -> {
            }
        }
    }

    private void raiseStat(StatType t, int amount) {
        hero.modifyStat(t, amount);
        try {
            var f = hero.getClass().getDeclaredField("stats");
            f.setAccessible(true);
            var cs = (CharacterStats) f.get(hero);
            cs.setMax(t, cs.getMax(t) + amount);
        } catch (Exception ignored) {
        }
    }

    // ── Loadout application ───────────────────────────────────────────────────
    private void applyLoadout(String heroName) {
        String weapon = userStore.getEquippedWeapon(heroName);
        String armor = userStore.getEquippedArmor(heroName);
        String skin = userStore.getEquippedSkin(heroName);

        // Apply weapon: create a proxy weapon object and equip it
        GameObject starterWeapon = mapWeaponIdToObject(weapon);
        if (starterWeapon == null)
            starterWeapon = GameObjects.dagger(hero.getPos());
        hero.getInventory().add(starterWeapon);
        hero.equipWeapon(starterWeapon);

        // Apply armour bonus to DEF
        applyArmorBonus(armor);

        // Apply skin
        heroSkinIndex = switch (skin == null ? "" : skin) {
            case "skin_red" -> 1;
            case "skin_blue" -> 2;
            case "skin_green" -> 3;
            case "skin_gold" -> 4;
            default -> 0;
        };
    }

    private GameObject mapWeaponIdToObject(String id) {
        if (id == null)
            return null;
        Vec2 p = hero.getPos();
        return switch (id) {
            case "w_dagger" -> GameObjects.dagger(p);
            case "w_sword" -> GameObjects.sword(p);
            case "w_axe" -> GameObjects.axe(p);
            case "w_greatsword" -> GameObjects.greatSword(p);
            default -> GameObjects.dagger(p);
        };
    }

    private void applyArmorBonus(String armor) {
        if (armor == null)
            return;
        int def = switch (armor) {
            case "armor_leather" -> 3;
            case "armor_chain" -> 6;
            case "armor_plate" -> 10;
            default -> 0;
        };
        if (def > 0)
            hero.modifyStat(StatType.DEF, def);
    }

    // ── Death check ───────────────────────────────────────────────────────────
    private void checkDeath() {
        if (!hero.isAlive()) {
            stop();
            state.endSession();
            post("YOU HAVE FALLEN in wave " + state.getCurrentWave() + "!");
            if (onGameOver != null)
                onGameOver.run();
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private void notifyMap() {
        if (onMapChanged != null)
            onMapChanged.run();
    }

    private void notifyStats() {
        if (onStatsChanged != null)
            onStatsChanged.run();
    }

    private void notifyState() {
        if (onStateChanged != null)
            onStateChanged.run();
    }

    private void post(String m) {
        System.out.println("[Wave] " + m);
        if (onMessage != null)
            onMessage.accept(m);
    }

    public void setOnMapChanged(Runnable r) {
        onMapChanged = r;
    }

    public void setOnStatsChanged(Runnable r) {
        onStatsChanged = r;
    }

    public void setOnStateChanged(Runnable r) {
        onStateChanged = r;
    }

    public void setOnMessage(Consumer<String> c) {
        onMessage = c;
    }

    public void setOnCoinDrop(Consumer<Integer> c) {
        onCoinDrop = c;
    }

    public void setOnWaveComplete(Runnable r) {
        onWaveComplete = r;
    }

    public void setOnMarketClose(Runnable r) {
        onMarketClose = r;
    }

    public void setOnGameOver(Runnable r) {
        onGameOver = r;
    }

    public void setOnInventoryChanged(Runnable r) {
        onInventoryChanged = r;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public GridMap getMap() {
        return map;
    }

    public Hero getHero() {
        return hero;
    }

    public WaveState getState() {
        return state;
    }

    public WaveDifficulty getDiff() {
        return diff;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public int getSkinIndex() {
        return heroSkinIndex;
    }

    public UserStore getUserStore() {
        return userStore;
    }

    public boolean isPurchased(String id) {
        return purchasedIds.contains(id) || userStore.ownsItem(hero.getName(), id);
    }
}
/ /   r e w a r d   s y n c   u p d a t e  
 