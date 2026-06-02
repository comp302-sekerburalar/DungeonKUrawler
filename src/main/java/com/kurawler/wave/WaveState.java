package com.kurawler.wave;

/**
 * Live runtime state for the current wave survival session.
 * Owned by WaveEngine; read by the UI.
 */
public class WaveState {

    // ── Wave progress ────────────────────────────────────────────────────────
    private int currentWave = 0;
    private int enemiesThisWave = 0; // total to spawn in current wave
    private int enemiesSpawned = 0; // how many have already been spawned
    private int enemiesKilled = 0; // killed this wave
    private boolean waveActive = false;
    private boolean marketOpen = false;

    // ── Timer ────────────────────────────────────────────────────────────────
    private double waveTimerSec = 0; // counts UP (display only)
    private double marketTimerSec = 0; // counts DOWN

    // ── Score & coins ────────────────────────────────────────────────────────
    private long totalScore = 0;
    private int coins = 0;

    // ── Streak ───────────────────────────────────────────────────────────────
    private int currentStreak = 0; // consecutive kills without taking damage
    private int maxStreak = 0;

    // ── Session stats ────────────────────────────────────────────────────────
    private int totalKills = 0;
    private int wavesCleared = 0;
    private boolean sessionOver = false;

    // ────────────────────────────────────────────────────────────────────────
    // Mutators (called by WaveEngine)
    // ────────────────────────────────────────────────────────────────────────

    public void startWave(int wave, int enemyCount) {
        currentWave = wave;
        enemiesThisWave = enemyCount;
        enemiesSpawned = 0;
        enemiesKilled = 0;
        waveActive = true;
        marketOpen = false;
        waveTimerSec = 0;
    }

    public void tickWaveTimer(double dtSec) {
        waveTimerSec += dtSec;
    }

    public void tickMarketTimer(double dtSec) {
        marketTimerSec -= dtSec;
    }

    public void openMarket(double durationSec) {
        marketOpen = true;
        waveActive = false;
        marketTimerSec = durationSec;
        wavesCleared = currentWave;
    }

    public void closeMarket() {
        marketOpen = false;
    }

    /** Record a kill, compute score & coin drop. Returns coins dropped. */
    public int recordKill(int baseScore, WaveDifficulty diff) {
        enemiesKilled++;
        totalKills++;
        currentStreak++;
        if (currentStreak > maxStreak)
            maxStreak = currentStreak;

        // Streak multiplier
        double scoreMult = currentStreak >= diff.streakBonusThreshold
                ? diff.streakScoreMultiplier
                : 1.0;
        totalScore += (long) (baseScore * scoreMult);

        // Coin drop
        double coinProb = diff.coinDropChance;
        if (currentStreak >= diff.streakBonusThreshold)
            coinProb *= diff.streakCoinMultiplier;
        int dropped = 0;
        if (Math.random() < coinProb) {
            dropped = 1 + (int) (Math.random() * 3); // 1-3 coins
            coins += dropped;
        }
        return dropped;
    }

    /** Called when the hero takes damage — resets the streak. */
    public void breakStreak() {
        currentStreak = 0;
    }

    public void addCoins(int n) {
        coins += n;
    }

    public boolean spendCoins(int n) {
        if (coins < n)
            return false;
        coins -= n;
        return true;
    }

    public void endSession() {
        sessionOver = true;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────────────────────────────

    public int getCurrentWave() {
        return currentWave;
    }

    public int getEnemiesThisWave() {
        return enemiesThisWave;
    }

    public int getEnemiesSpawned() {
        return enemiesSpawned;
    }

    public int getEnemiesKilled() {
        return enemiesKilled;
    }

    public int getEnemiesLeft() {
        return Math.max(0, enemiesThisWave - enemiesKilled);
    }

    public boolean isWaveActive() {
        return waveActive;
    }

    public boolean isMarketOpen() {
        return marketOpen;
    }

    public double getWaveTimerSec() {
        return waveTimerSec;
    }

    public double getMarketTimerSec() {
        return marketTimerSec;
    }

    public long getTotalScore() {
        return totalScore;
    }

    public int getCoins() {
        return coins;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getWavesCleared() {
        return wavesCleared;
    }

    public boolean isSessionOver() {
        return sessionOver;
    }

    // package-private for WaveEngine
    int getEnemiesSpawnedMut() {
        return enemiesSpawned;
    }

    void incrementSpawned() {
        enemiesSpawned++;
    }

    void setWaveActive(boolean b) {
        waveActive = b;
    }
}
