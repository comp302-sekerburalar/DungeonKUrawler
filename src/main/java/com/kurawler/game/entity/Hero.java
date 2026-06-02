package com.kurawler.game.entity;

import com.kurawler.engine.Vec2;
import com.kurawler.game.objects.GameObject;

public class Hero {

    public static final int ENERGY_COST_WALK = 1;
    public static final int ENERGY_COST_ATTACK = 3;
    public static final int ENERGY_COST_BREAK = 5;

    private Vec2 pos;
    private CharacterStats stats;
    private final Inventory inventory;
    private final String name;
    private GameObject equippedWeapon; // null = unarmed

    public Hero(String name, Vec2 startPos, int str) {
        this.name = name;
        this.pos = startPos;
        this.inventory = new Inventory();
        // spec §2.4.1: HP=17, Mana=80, STR=random 8-15, DEF=2, Energy=100
        this.stats = new CharacterStats(17, 80, str, 2, 100);
    }

    // ── position ──
    public Vec2 getPos() {
        return pos;
    }

    public void setPos(Vec2 pos) {
        this.pos = pos;
    }

    // ── stats ──
    public int getStat(StatType t) {
        return stats.get(t);
    }

    public int getStatMax(StatType t) {
        return stats.getMax(t);
    }

    public void modifyStat(StatType t, int d) {
        stats.modify(t, d);
    }

    public boolean isAlive() {
        return stats.get(StatType.HP) > 0;
    }

    public void spendEnergy(int amount) {
        stats.modify(StatType.ENERGY, -amount);
    }

    // ── inventory ──
    public Inventory getInventory() {
        return inventory;
    }

    // ── weapon ──
    public GameObject getEquippedWeapon() {
        return equippedWeapon;
    }

    public boolean hasWeaponEquipped() {
        return equippedWeapon != null;
    }

    public void equipWeapon(GameObject weapon) {
        equippedWeapon = weapon;
    }

    public void unequipWeapon() {
        equippedWeapon = null;
    }

    // ── identity ──
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Hero[" + name + "@" + pos + " HP=" + stats.get(StatType.HP) + "]";
    }
}
