package com.kurawler.game.entity;

import com.kurawler.engine.Vec2;

/**
 * The player-controlled hero (spec §2.4).
 *
 * Starting stats (spec §2.4.1):
 *   HP     = 17
 *   Mana   = 80
 *   STR    = random 8–15 (set by GameEngine at start)
 *   DEF    = 2
 *   Energy = 100 (design decision: start full)
 */
public class Hero {

    private Vec2           pos;
    private CharacterStats stats;
    private final Inventory inventory;
    private final String   name;

    public static final int ENERGY_COST_WALK = 1;

    public Hero(String name, Vec2 startPos, int str) {
        this.name      = name;
        this.pos       = startPos;
        this.inventory = new Inventory();

        // spec §2.4.1 starting values
        this.stats = new CharacterStats(
            17,   // HP
            80,   // Mana
            str,  // STR (random 8-15)
            2,    // DEF
            100   // Energy (design decision)
        );
    }

    // ---------- Movement ----------

    public Vec2 getPos()         { return pos; }
    public void setPos(Vec2 pos) { this.pos = pos; }

    // ---------- Stats ----------

    public int     getStat(StatType t)           { return stats.get(t); }
    public int     getStatMax(StatType t)        { return stats.getMax(t); }
    public void    modifyStat(StatType t, int d) { stats.modify(t, d); }
    public boolean isAlive()                     { return stats.get(StatType.HP) > 0; }

    /** Consume energy for walking; returns false if out of energy (design decision: still moves). */
    public void spendEnergy(int amount) { stats.modify(StatType.ENERGY, -amount); }

    // ---------- Inventory ----------

    public Inventory getInventory() { return inventory; }

    // ---------- Identity ----------

    public String getName() { return name; }

    @Override
    public String toString() {
        return "Hero[" + name + " @" + pos + " HP=" + stats.get(StatType.HP) + "]";
    }
}
