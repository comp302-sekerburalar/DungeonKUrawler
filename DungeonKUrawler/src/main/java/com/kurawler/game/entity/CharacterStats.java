package com.kurawler.game.entity;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the five stats for any character (hero or enemy).
 * All values are clamped to [0, max].
 */
public class CharacterStats {

    private final Map<StatType, Integer> current = new EnumMap<>(StatType.class);
    private final Map<StatType, Integer> max     = new EnumMap<>(StatType.class);

    public CharacterStats(int hp, int mana, int str, int def, int energy) {
        set(StatType.HP,     hp,     hp);
        set(StatType.MANA,   mana,   mana);
        set(StatType.STR,    str,    str);
        set(StatType.DEF,    def,    def);
        set(StatType.ENERGY, energy, energy);
    }

    private void set(StatType t, int cur, int maximum) {
        current.put(t, cur);
        max.put(t, maximum);
    }

    public int get(StatType t)    { return current.get(t); }
    public int getMax(StatType t) { return max.get(t); }

    /**
     * Apply delta to stat, clamping to [0, max].
     * @return the actual change applied
     */
    public int modify(StatType t, int delta) {
        int before  = current.get(t);
        int clamped = Math.max(0, Math.min(max.get(t), before + delta));
        current.put(t, clamped);
        return clamped - before;
    }

    /** Raise the max and current value (e.g. equipping armour boosts DEF max). */
    public void raiseMax(StatType t, int amount) {
        max.put(t, max.get(t) + amount);
        current.put(t, current.get(t) + amount);
    }
}
