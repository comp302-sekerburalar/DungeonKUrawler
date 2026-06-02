package com.kurawler.wave;

/**
 * A purchasable item in the Marketplace.
 *
 * Consumables (potions etc.) stack in the UserStore and are used one at a time
 * during Wave Survival. Permanent items (weapons, armour, skins, power-ups)
 * unlock once and cannot be repurchased.
 */
public class MarketItem {

    public enum Category {
        CONSUMABLE, WEAPON, ARMOR, POWER_UP, SKIN
    }

    public enum EffectType {
        RESTORE_HP, RESTORE_MANA, RESTORE_ENERGY,
        BOOST_MAX_HP, BOOST_STR, BOOST_DEF, BOOST_MAX_ENERGY,
        GRANT_WEAPON, SKIN_CHANGE
    }

    private final String id;
    private final String name;
    private final String description;
    private final int price;
    private final Category category;
    private final EffectType effectType;
    private final int effectValue;
    /**
     * true = consumable (quantity-based, always purchasable)
     * false = permanent (unlock once, blocks duplicate purchase)
     */
    private final boolean consumable;

    public MarketItem(String id, String name, String description,
            int price, Category category,
            EffectType effectType, int effectValue, boolean consumable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.effectType = effectType;
        this.effectValue = effectValue;
        this.consumable = consumable;
    }

    public static java.util.List<MarketItem> buildCatalogue() {
        java.util.List<MarketItem> items = new java.util.ArrayList<>();

        // ── Consumables (consumable = TRUE → quantity stacks) ────────────────
        items.add(new MarketItem("hp_small", "Minor Health Potion",
                "Restore 8 HP. Press 1 in-game.", 20, Category.CONSUMABLE, EffectType.RESTORE_HP, 8, true));
        items.add(new MarketItem("hp_large", "Greater Health Potion",
                "Restore 20 HP. Press 1 in-game.", 45, Category.CONSUMABLE, EffectType.RESTORE_HP, 20, true));
        items.add(new MarketItem("mana_pot", "Mana Potion",
                "Restore 30 Mana. Press 2 in-game.", 30, Category.CONSUMABLE, EffectType.RESTORE_MANA, 30, true));
        items.add(new MarketItem("energy_pot", "Energy Drink",
                "Restore 40 Energy. Press 3.", 25, Category.CONSUMABLE, EffectType.RESTORE_ENERGY, 40, true));

        // ── Power-ups (permanent, consumable = FALSE) ────────────────────────
        items.add(new MarketItem("max_hp_up", "Iron Will",
                "+10 max HP permanently.", 80, Category.POWER_UP, EffectType.BOOST_MAX_HP, 10, false));
        items.add(new MarketItem("str_up", "Strength Rune",
                "+2 STR permanently.", 90, Category.POWER_UP, EffectType.BOOST_STR, 2, false));
        items.add(new MarketItem("def_up", "Stone Skin",
                "+2 DEF permanently.", 85, Category.POWER_UP, EffectType.BOOST_DEF, 2, false));
        items.add(new MarketItem("energy_up", "Endurance Rune",
                "+20 max Energy.", 70, Category.POWER_UP, EffectType.BOOST_MAX_ENERGY, 20, false));

        // ── Weapons (permanent) ───────────────────────────────────────────────
        items.add(new MarketItem("w_dagger", "Steel Dagger", "ATK +5.", 60, Category.WEAPON, EffectType.GRANT_WEAPON, 5,
                false));
        items.add(new MarketItem("w_sword", "Iron Sword", "ATK +8.", 100, Category.WEAPON, EffectType.GRANT_WEAPON, 8,
                false));
        items.add(new MarketItem("w_axe", "Battle Axe", "ATK +12.", 150, Category.WEAPON, EffectType.GRANT_WEAPON, 12,
                false));
        items.add(new MarketItem("w_greatsword", "Greatsword", "ATK +18.", 250, Category.WEAPON,
                EffectType.GRANT_WEAPON, 18, false));

        // ── Armour (permanent) ────────────────────────────────────────────────
        items.add(new MarketItem("armor_leather", "Leather Armor", "+3 DEF.", 80, Category.ARMOR, EffectType.BOOST_DEF,
                3, false));
        items.add(new MarketItem("armor_chain", "Chain Mail", "+6 DEF.", 160, Category.ARMOR, EffectType.BOOST_DEF, 6,
                false));
        items.add(new MarketItem("armor_plate", "Plate Armor", "+10 DEF.", 280, Category.ARMOR, EffectType.BOOST_DEF,
                10, false));

        // ── Skins (permanent) ─────────────────────────────────────────────────
        items.add(new MarketItem("skin_red", "Red Knight", "Hero turns crimson.", 40, Category.SKIN,
                EffectType.SKIN_CHANGE, 1, false));
        items.add(new MarketItem("skin_blue", "Blue Mage", "Hero turns sapphire.", 40, Category.SKIN,
                EffectType.SKIN_CHANGE, 2, false));
        items.add(new MarketItem("skin_green", "Shadow Rogue", "Hero turns emerald.", 40, Category.SKIN,
                EffectType.SKIN_CHANGE, 3, false));
        items.add(new MarketItem("skin_gold", "Golden Warrior", "Hero turns gold.", 80, Category.SKIN,
                EffectType.SKIN_CHANGE, 4, false));

        return items;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public int getEffectValue() {
        return effectValue;
    }

    public boolean isConsumable() {
        return consumable;
    }
}
