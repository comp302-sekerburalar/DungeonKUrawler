package com.kurawler.game.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CharacterStatsTest {

    @Test
    public void modifyIncreasesStatWithinMaximum() {
        CharacterStats stats = new CharacterStats(10, 5, 3, 2, 20);

        int damageChange = stats.modify(StatType.HP, -4);
        int healChange = stats.modify(StatType.HP, 2);

        assertEquals(-4, damageChange);
        assertEquals(2, healChange);
        assertEquals(8, stats.get(StatType.HP));
    }

    @Test
    public void modifyDoesNotIncreaseAboveMaximum() {
        CharacterStats stats = new CharacterStats(10, 5, 3, 2, 20);

        int actualChange = stats.modify(StatType.HP, 5);

        assertEquals(0, actualChange);
        assertEquals(10, stats.get(StatType.HP));
    }

    @Test
    public void modifyDoesNotDecreaseBelowZero() {
        CharacterStats stats = new CharacterStats(10, 5, 3, 2, 20);

        int actualChange = stats.modify(StatType.MANA, -10);

        assertEquals(-5, actualChange);
        assertEquals(0, stats.get(StatType.MANA));
    }
}