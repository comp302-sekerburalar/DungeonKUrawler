package main.java.com.kurawler.wave;

/**
 * Encodes how the difficulty scales across waves.
 *
 * Three preset difficulties (RECRUIT / VETERAN / NIGHTMARE) seed different
 * base values; after that every wave applies a multiplier to enemy count,
 * HP, damage, and spawn speed.
 */
public class WaveDifficulty {

    public enum Preset { RECRUIT, VETERAN, NIGHTMARE }

    // ── Wave spawn settings ──────────────────────────────────────────────────
    public final int    baseEnemiesPerWave;   // enemies in wave 1
    public final double enemyGrowthPerWave;   // +N enemies each wave
    public final int    maxEnemiesOnField;    // cap on simultaneous enemies
    public final double spawnIntervalSeconds; // gap between individual spawns

    // ── Stat multipliers per wave ────────────────────────────────────────────
    public final double hpMultiplierPerWave;    // enemy HP * (1 + wave * this)
    public final double dmgMultiplierPerWave;   // enemy dmg * (1 + wave * this)

    // ── Hero scaling per wave (reward for surviving) ─────────────────────────
    public final int heroHpBonus;       // +HP per wave cleared
    public final int heroEnergyBonus;   // +Energy per wave cleared
    public final int heroStrBonus;      // +STR every 3 waves

    // ── Score & coin config ──────────────────────────────────────────────────
    public final int  baseKillScore;        // score for killing wave-1 enemy
    public final double killScoreGrowth;    // multiplier per wave
    public final double coinDropChance;     // base probability enemy drops a coin
    public final int  streakBonusThreshold; // kills in a row needed for streak bonus
    public final double streakScoreMultiplier;
    public final double streakCoinMultiplier;

    // ── Between-wave market break ────────────────────────────────────────────
    public final int marketBreakSeconds;    // how long the market window lasts

    // ── preset name ─────────────────────────────────────────────────────────
    public final Preset preset;

    private WaveDifficulty(Preset p,
                           int base, double growth, int maxField, double spawnSec,
                           double hpMult, double dmgMult,
                           int herHp, int herEn, int herStr,
                           int killScore, double killGrowth,
                           double coinDrop, int streakThresh,
                           double streakScore, double streakCoin,
                           int marketSec) {
        this.preset                = p;
        this.baseEnemiesPerWave    = base;
        this.enemyGrowthPerWave    = growth;
        this.maxEnemiesOnField     = maxField;
        this.spawnIntervalSeconds  = spawnSec;
        this.hpMultiplierPerWave   = hpMult;
        this.dmgMultiplierPerWave  = dmgMult;
        this.heroHpBonus           = herHp;
        this.heroEnergyBonus       = herEn;
        this.heroStrBonus          = herStr;
        this.baseKillScore         = killScore;
        this.killScoreGrowth       = killGrowth;
        this.coinDropChance        = coinDrop;
        this.streakBonusThreshold  = streakThresh;
        this.streakScoreMultiplier = streakScore;
        this.streakCoinMultiplier  = streakCoin;
        this.marketBreakSeconds    = marketSec;
    }

    // ── Factories ────────────────────────────────────────────────────────────

    public static WaveDifficulty recruit() {
        return new WaveDifficulty(Preset.RECRUIT,
            3, 1.5, 4, 3.5,
            0.10, 0.08,
            4, 10, 1,
            50, 0.15,
            0.30, 3, 1.5, 1.8,
            20);
    }

    public static WaveDifficulty veteran() {
        return new WaveDifficulty(Preset.VETERAN,
            4, 2.0, 5, 2.5,
            0.18, 0.15,
            2, 5, 1,
            75, 0.20,
            0.25, 3, 1.8, 2.0,
            18);
    }

    public static WaveDifficulty nightmare() {
        return new WaveDifficulty(Preset.NIGHTMARE,
            5, 2.5, 6, 1.8,
            0.28, 0.22,
            1, 3, 0,
            100, 0.25,
            0.20, 3, 2.0, 2.5,
            15);
    }

    public static WaveDifficulty fromLabel(String label) {
        return switch (label.toUpperCase()) {
            case "NIGHTMARE" -> nightmare();
            case "VETERAN"   -> veteran();
            default          -> recruit();
        };
    }

    /** How many enemies total in the given wave (1-indexed). */
    public int enemiesInWave(int wave) {
        return (int) Math.ceil(baseEnemiesPerWave + (wave - 1) * enemyGrowthPerWave);
    }

    /** Enemy HP multiplier for a given wave. */
    public double enemyHpScale(int wave) {
        return 1.0 + (wave - 1) * hpMultiplierPerWave;
    }

    /** Enemy damage multiplier for a given wave. */
    public double enemyDmgScale(int wave) {
        return 1.0 + (wave - 1) * dmgMultiplierPerWave;
    }

    /** Score for a kill in a given wave (before streak bonus). */
    public int killScore(int wave) {
        return (int)(baseKillScore * (1.0 + (wave - 1) * killScoreGrowth));
    }
}
