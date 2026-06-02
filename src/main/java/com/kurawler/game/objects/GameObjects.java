package com.kurawler.game.objects;

import com.kurawler.engine.Vec2;
import com.kurawler.game.action.Action;
import com.kurawler.game.effect.Effects;
import com.kurawler.game.entity.StatType;

import java.util.Random;

// ── Internal subclasses ──────────────────────────────────────────────────────

class WallObject extends GameObject {
    WallObject(Vec2 pos) {
        super("Wall", pos, true, "walls_and_statics_x2", 0, 0);
    }

    @Override
    public String renderTag() {
        return "WALL";
    }
}

class CrateObject extends GameObject {
    CrateObject(Vec2 pos) {
        super("Crate", pos, true, "walls_and_statics_x2", 2, 0);
    }

    @Override
    public String renderTag() {
        return "CRATE";
    }
}

class ColumnObject extends GameObject {
    ColumnObject(Vec2 pos) {
        super("Column", pos, true, "walls_and_statics_x2", 1, 2);
    }

    @Override
    public String renderTag() {
        return "COLUMN";
    }
}

class ItemObject extends GameObject {
    private final String tag;

    ItemObject(String name, Vec2 pos, String tag, String sheet, int sc, int sr) {
        super(name, pos, false, sheet, sc, sr);
        this.tag = tag;
        addAction(new Action("Take " + name, Effects.addToInventory()));
    }

    @Override
    public String renderTag() {
        return tag;
    }
}

class WeaponObject extends GameObject {
    private final int atkValue;

    WeaponObject(String name, Vec2 pos, int atk, int ssCol, int ssRow) {
        super(name, pos, false, "weapons_x2", ssCol, ssRow);
        this.atkValue = atk;
        addAction(new Action("Take " + name, Effects.addToInventory()));
        addAction(new Action("Equip " + name, Effects.equipWeapon()));
    }

    @Override
    public String renderTag() {
        return "WEAPON";
    }

    public int getAtk() {
        return atkValue;
    }
}

class ArmorObject extends GameObject {
    private final int defBonus;

    ArmorObject(String name, Vec2 pos, int def, int ssCol, int ssRow) {
        super(name, pos, false, "items_x2", ssCol, ssRow);
        this.defBonus = def;
        addAction(new Action("Take " + name, Effects.addToInventory()));
        addAction(new Action("Wear " + name, Effects.wearArmor(def)));
    }

    @Override
    public String renderTag() {
        return "ARMOR";
    }

    public int getDef() {
        return defBonus;
    }
}

class PotionObject extends GameObject {
    PotionObject(String name, Vec2 pos, StatType stat, int amount, int ssCol, int ssRow) {
        super(name, pos, false, "items_x2", ssCol, ssRow);
        addAction(new Action("Take " + name, Effects.addToInventory()));
        addAction(new Action("Drink " + name, Effects.modifyStat(stat, amount, true)));
    }

    @Override
    public String renderTag() {
        return "POTION";
    }
}

class ShadowCloneScrollObject extends GameObject {
    ShadowCloneScrollObject(Vec2 pos) {
        super("Shadow Clone Scroll", pos, false, "items_x2", 3, 2);
        addAction(new Action("Take Shadow Clone Scroll", Effects.addToInventory()));
        addAction(new Action("READ Shadow Clone", Effects.shadowClone()));
    }

    @Override
    public String renderTag() {
        return "SHADOW_SCROLL";
    }
}

class ChestObject extends GameObject {
    ChestObject(Vec2 pos) {
        super("Chest", pos, true, "containers_x2", 0, 0);
    }

    @Override
    public String renderTag() {
        return "CHEST";
    }
}

class SearchableWall extends GameObject {
    SearchableWall(Vec2 pos, GameObject hidden) {
        super("Cracked Wall", pos, true, "walls_and_statics_x2", 3, 1);
        addAction(new Action("Search Cracked Wall", Effects.search(hidden)));
    }

    @Override
    public String renderTag() {
        return "SEARCH_WALL";
    }
}

class BreakableCrate extends GameObject {
    BreakableCrate(Vec2 pos) {
        super("Wooden Crate", pos, true, "containers_x2", 1, 0);
        addAction(new Action("Break Wooden Crate", Effects.breakObject()));
    }

    @Override
    public String renderTag() {
        return "BREAK_CRATE";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RelicGameObject — the target relic item; triggers victory when picked up
// ─────────────────────────────────────────────────────────────────────────────
class RelicGameObject extends GameObject {
    RelicGameObject(String name, com.kurawler.engine.Vec2 pos) {
        super(name, pos, false, "items_x2", 4, 1); // gem sprite
        addAction(new com.kurawler.game.action.Action("Take " + name,
                com.kurawler.game.effect.Effects.addToInventory()));
    }

    @Override
    public String renderTag() {
        return "RELIC";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Searchable box — small wooden crate that can be searched (holds hidden items)
// ─────────────────────────────────────────────────────────────────────────────
class SearchableBox extends GameObject {
    SearchableBox(Vec2 pos, com.kurawler.game.objects.GameObject hidden) {
        super("Wooden Box", pos, true, "", -1, -1);
        addAction(new com.kurawler.game.action.Action("Search Wooden Box",
                com.kurawler.game.effect.Effects.search(hidden)));
    }

    @Override
    public String renderTag() {
        return "SEARCH_BOX";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Relic chest — locked chest that holds the target relic; break to open
// ─────────────────────────────────────────────────────────────────────────────
class RelicChest extends GameObject {
    RelicChest(Vec2 pos, com.kurawler.game.objects.GameObject relic) {
        super("Ancient Chest", pos, true, "", -1, -1);
        addAction(new com.kurawler.game.action.Action("Open Ancient Chest",
                com.kurawler.game.effect.Effects.search(relic)));
        addAction(new com.kurawler.game.action.Action("Break Ancient Chest",
                com.kurawler.game.effect.Effects.breakObject()));
    }

    @Override
    public String renderTag() {
        return "RELIC_CHEST";
    }
}

// ── Public factory ───────────────────────────────────────────────────────────

public final class GameObjects {

    private static final Random RNG = new Random();

    private GameObjects() {
    }

    public static GameObject wall(Vec2 pos) {
        return new WallObject(pos);
    }

    public static GameObject crate(Vec2 pos) {
        return new CrateObject(pos);
    }

    public static GameObject column(Vec2 pos) {
        return new ColumnObject(pos);
    }

    public static GameObject chest(Vec2 pos) {
        return new ChestObject(pos);
    }

    public static GameObject breakableCrate(Vec2 p) {
        return new BreakableCrate(p);
    }

    // Items
    public static GameObject key(Vec2 pos) {
        return new ItemObject("Key", pos, "KEY", "items_x2", 0, 0);
    }

    public static GameObject gem(Vec2 pos) {
        return new ItemObject("Gem", pos, "GEM", "items_x2", 4, 1);
    }

    public static GameObject ring(Vec2 pos) {
        return new ItemObject("Ring", pos, "RING", "items_x2", 0, 1);
    }

    public static GameObject spellBook(Vec2 pos) {
        return new ItemObject("Spell Book", pos, "BOOK", "items_x2", 3, 2);
    }

    public static GameObject shadowCloneScroll(Vec2 pos) {
        return new ShadowCloneScrollObject(pos);
    }

    // Potions
    public static GameObject redPotion(Vec2 pos) {
        return new PotionObject("Red Potion", pos, StatType.HP, 5, 1, 0);
    }

    public static GameObject bluePotion(Vec2 pos) {
        return new PotionObject("Blue Potion", pos, StatType.MANA, 20, 2, 0);
    }

    public static GameObject greenPotion(Vec2 pos) {
        return new PotionObject("Green Potion", pos, StatType.ENERGY, 15, 3, 0);
    }

    // Weapons — ATK values and sprite coords from the weapons sheet (10x10 at 32px)
    public static WeaponObject sword(Vec2 pos) {
        return new WeaponObject("Iron Sword", pos, 6, 0, 0);
    }

    public static WeaponObject dagger(Vec2 pos) {
        return new WeaponObject("Dagger", pos, 4, 1, 0);
    }

    public static WeaponObject axe(Vec2 pos) {
        return new WeaponObject("Battle Axe", pos, 8, 2, 0);
    }

    public static WeaponObject bow(Vec2 pos) {
        return new WeaponObject("Short Bow", pos, 5, 8, 9);
    }

    public static WeaponObject greatSword(Vec2 pos) {
        return new WeaponObject("Great Sword", pos, 10, 0, 4);
    }

    public static WeaponObject teamWeapon(String name, Vec2 pos, int atk, int ssCol, int ssRow) {
        return new WeaponObject(name, pos, atk, ssCol, ssRow);
    }

    // Armor
    public static ArmorObject leatherArmor(Vec2 pos) {
        return new ArmorObject("Leather Armor", pos, 2, 4, 2);
    }

    public static ArmorObject chainArmor(Vec2 pos) {
        return new ArmorObject("Chain Armor", pos, 4, 5, 2);
    }

    // Searchable location
    public static GameObject searchableWall(Vec2 pos, GameObject hiddenItem) {
        return new SearchableWall(pos, hiddenItem);
    }

    /** Searchable wooden box — hides an item inside. */
    public static GameObject searchableBox(Vec2 pos, GameObject hidden) {
        return new SearchableBox(pos, hidden);
    }

    /** Relic item that triggers victory when picked up. */
    public static RelicGameObject relicItem(String name, com.kurawler.engine.Vec2 pos) {
        return new RelicGameObject(name, pos);
    }

    /** Ancient Chest — holds the target relic; can be opened or broken. */
    public static GameObject relicChest(Vec2 pos, GameObject relic) {
        return new RelicChest(pos, relic);
    }

    /** Random weapon with random ATK in given range. */
    public static WeaponObject randomWeapon(Vec2 pos) {
        WeaponObject[] pool = {
                sword(pos), dagger(pos), axe(pos), bow(pos), greatSword(pos)
        };
        return pool[RNG.nextInt(pool.length)];
    }

    /** Random item (potion, key, gem, ring). */
    public static GameObject randomItem(Vec2 pos) {
        int r = RNG.nextInt(6);
        return switch (r) {
            case 0 -> redPotion(pos);
            case 1 -> bluePotion(pos);
            case 2 -> greenPotion(pos);
            case 3 -> key(pos);
            case 4 -> gem(pos);
            default -> ring(pos);
        };
    }

    /** Random armor. */
    public static ArmorObject randomArmor(Vec2 pos) {
        return RNG.nextBoolean() ? leatherArmor(pos) : chainArmor(pos);
    }

    // Expose concrete type for ATK lookup
    public static int getWeaponAtk(GameObject obj) {
        return (obj instanceof WeaponObject w) ? w.getAtk() : 0;
    }
}
/ /   r e g i s t r y   s p r i t e   i n t e g r a t i o n  
 