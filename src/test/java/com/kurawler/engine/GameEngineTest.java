package com.kurawler.engine;

import com.kurawler.game.entity.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ADT tests for GameEngine.
 */
public class GameEngineTest {

    private GameEngine game;

    @BeforeEach
    public void setUp() {
        game = new GameEngine();
    }

    /**
     * Tests that GameEngine satisfies representation invariant.
     */
    @Test
    public void testRepOkInitially() {
        assertTrue(game.repOk());
    }

    /**
     * Tests hero movement updates position.
     */
    @Test
    public void testMoveHero() {

        int oldX = game.getHero().getPos().col();
        int oldY = game.getHero().getPos().row();

        game.moveHero(1, 0);

        int newX = game.getHero().getPos().col();
        int newY = game.getHero().getPos().row();

        assertNotEquals(oldX, newX);
        assertEquals(oldY, newY);
    }

    /**
     * Tests enemy addition.
     */
    @Test
    public void testAddEnemy() {

        int before = game.getEnemies().size();

        game.addEnemyForTest(
                new Enemy(
                        "1",
                        Enemy.Type.KNIGHT,
                        game.getHero().getPos()));

        int after = game.getEnemies().size();

        assertEquals(before + 1, after);
    }

    /**
     * Tests enemy limit invariant.
     */
    @Test
    public void testEnemyLimitInvariant() {

        while (game.getEnemies().size() < game.getMaxEnemies()) {

            game.addEnemyForTest(
                    new Enemy(
                            "temp",
                            Enemy.Type.KNIGHT,
                            game.getHero().getPos()));
        }

        assertTrue(game.repOk());
    }
}