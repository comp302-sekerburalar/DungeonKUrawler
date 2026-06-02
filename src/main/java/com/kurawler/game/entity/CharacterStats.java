package com.kurawler.game.entity;

import java.util.EnumMap;
import java.util.Map;

public class CharacterStats {

    private final Map<StatType, Integer> current = new EnumMap<>(StatType.class);
    private final Map<StatType, Integer> max = new EnumMap<>(StatType.class);

    public CharacterStats(int hp, int mana, int str, int def, int energy) {
        init(StatType.HP, hp, hp);
        init(StatType.MANA, mana, mana);
        init(StatType.STR, str, str);
        init(StatType.DEF, def, def);
        init(StatType.ENERGY, energy, energy);
    }

    private void init(StatType t, int cur, int mx) {
        current.put(t, cur);
        max.put(t, mx);
    }

    public int get(StatType t) {
        return current.get(t);
    }

    public int getMax(StatType t) {
        return max.get(t);
    }

    public int modify(StatType t, int delta) {
        int before = current.get(t);
        int clamped = Math.max(0, Math.min(max.get(t), before + delta));
        current.put(t, clamped);
        return clamped - before;
    }

    public void setMax(StatType t, int newMax) {
        max.put(t, newMax);
    }

    public void set(StatType t, int val) {
        current.put(t, Math.max(0, Math.min(max.get(t), val)));
    }
}
