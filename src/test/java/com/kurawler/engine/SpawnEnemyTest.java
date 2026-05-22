package com.kurawler.engine;

import com.kurawler.game.entity.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for spawnEnemy().
 */
public class SpawnEnemyTest {

    private GameEngine game;

    @BeforeEach
    public void setUp() {
        game = new GameEngine();
    }

    /**
     * Tests that no enemy is spawned when
     * the random roll exceeds spawn probabilities.
     */
    @Test
    public void testNoSpawnBecauseProbabilityTooHigh() {

        game.setRandom(new FixedRandom(0.99));

        int before = game.getEnemies().size();

        game.spawnEnemy();

        int after = game.getEnemies().size();

        assertEquals(before, after);
    }

    /**
     * Tests that a knight enemy is spawned.
     */
    @Test
    public void testKnightSpawn() {

        game.setRandom(new FixedRandom(0.10));

        int before = game.getEnemies().size();

        game.spawnEnemy();

        int after = game.getEnemies().size();

        assertEquals(before + 1, after);

        Enemy spawned = game.getEnemies().get(game.getEnemies().size() - 1);

        assertEquals(Enemy.Type.KNIGHT, spawned.getType());
    }

    /**
     * Tests that a sorcerer enemy is spawned.
     */
    @Test
    public void testSorcererSpawn() {

        game.setRandom(new FixedRandom(0.70));

        int before = game.getEnemies().size();

        game.spawnEnemy();

        int after = game.getEnemies().size();

        assertEquals(before + 1, after);

        Enemy spawned = game.getEnemies().get(game.getEnemies().size() - 1);

        assertEquals(Enemy.Type.SORCERER, spawned.getType());
    }

    /**
     * Tests that no enemy is spawned when
     * maximum enemy count is reached.
     */
    @Test
    public void testNoSpawnAtMaxEnemies() {

        while (game.getEnemies().size() < game.getMaxEnemies()) {

            game.addEnemyForTest(
                    new Enemy(
                            "temp",
                            Enemy.Type.KNIGHT,
                            game.getHero().getPos()));
        }

        int before = game.getEnemies().size();

        game.spawnEnemy();

        int after = game.getEnemies().size();

        assertEquals(before, after);
    }

    /**
     * Helper random class for deterministic testing.
     */
    private static class FixedRandom extends Random {

        private final double value;

        public FixedRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}