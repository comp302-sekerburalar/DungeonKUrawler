package com.kurawler.engine;

import com.kurawler.game.entity.*;
import com.kurawler.game.objects.GameObjects;
import com.kurawler.game.objects.GameObject;

/**
 * Damage calculation (spec §3):
 *
 * damage_generated = floor( ATK * (1 + STR/20) * magicMod )
 * def_stat = armorDEF + floor(STR/10) + magicDefMod
 * damage_received = max(1, damage_generated - def_stat)
 * new_HP = current_HP - damage_received
 */
public final class CombatSystem {

    private CombatSystem() {
    }

    /**
     * Hero attacks a specific enemy.
     * 
     * @return actual damage dealt after enemy DEF absorption.
     */
    public static int heroAttack(Hero hero, Enemy enemy) {
        int atk = hero.hasWeaponEquipped()
                ? GameObjects.getWeaponAtk(hero.getEquippedWeapon())
                : 1; // unarmed = 1 ATK
        int str = hero.getStat(StatType.STR);
        int raw = (int) (atk * (1.0 + str / 20.0));

        // Enemy DEF
        int enemyDef = enemy.getStat(StatType.DEF);
        // Knights reduce by 1 extra (armour, spec §2.5.1)
        if (enemy.getType() == Enemy.Type.KNIGHT)
            enemyDef += 1;

        int actual = Math.max(1, raw - enemyDef);
        enemy.receiveDamage(raw); // receiveDamage already applies enemy DEF internally
        hero.spendEnergy(Hero.ENERGY_COST_ATTACK);
        return actual;
    }

    /**
     * Enemy melee-attacks the hero.
     * 
     * @return damage received by hero.
     */
    public static int enemyAttackHero(Enemy enemy, Hero hero) {
        int raw = enemy.generateDamage();
        int heroDef = hero.getStat(StatType.DEF);
        int actual = Math.max(1, raw - heroDef);
        hero.modifyStat(StatType.HP, -actual);
        return actual;
    }

    public static int enemyAttackEnemy(Enemy attacker, Enemy defender) {
        int raw;
        if (attacker.getType() == Enemy.Type.KNIGHT) {
            int atk = attacker.hasWeaponEquipped() ? attacker.getWeaponAtk() : 1;
            raw = (int) (atk * (1.0 + attacker.getStat(StatType.STR) / 20.0));
        } else {
            raw = attacker.generateDamage();
        }
        int def = defender.getStat(StatType.DEF) + (defender.getType() == Enemy.Type.KNIGHT ? 1 : 0);
        int actual = Math.max(1, raw - def);
        defender.receiveDamage(raw);
        return actual;
    }

    /**
     * Sorcerer projectile hits hero.
     */
    public static int projectileHitsHero(Hero hero) {
        int raw = 8; // spec §2.5.2
        int heroDef = hero.getStat(StatType.DEF);
        int actual = Math.max(1, raw - heroDef);
        hero.modifyStat(StatType.HP, -actual);
        return actual;
    }
}
