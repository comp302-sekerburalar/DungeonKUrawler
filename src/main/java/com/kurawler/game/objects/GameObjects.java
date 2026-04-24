package com.kurawler.game.objects;

import com.kurawler.engine.Vec2;
import com.kurawler.game.action.Action;
import com.kurawler.game.effect.AddToInventoryEffect;
import com.kurawler.game.effect.ModifyStatEffect;
import com.kurawler.game.entity.StatType;

// ============================================================
//  Static objects  (blocksMovement = true)
// ============================================================

/**
 * An impassable wall tile – no actions.
 */
class WallObject extends GameObject {
    public WallObject(Vec2 pos) { super("Wall", pos, true); }
    @Override public String renderTag() { return "WALL"; }
}

/**
 * A crate – impassable, no actions in Phase 1 (BREAK comes Phase 2).
 */
class CrateObject extends GameObject {
    public CrateObject(Vec2 pos) { super("Crate", pos, true); }
    @Override public String renderTag() { return "CRATE"; }
}

// ============================================================
//  Items  (blocksMovement = false, hero can share tile)
// ============================================================

/**
 * A generic pickable item. Registers a TAKE action by default.
 */
class ItemObject extends GameObject {
    private final String tag;

    ItemObject(String name, Vec2 pos, String tag) {
        super(name, pos, false);
        this.tag = tag;
        addAction(new Action("Take " + name, new AddToInventoryEffect()));
    }

    @Override public String renderTag() { return tag; }
}

// ============================================================
//  Factory  (public entry point)
// ============================================================

/**
 * Static factory so the rest of the codebase never refers to anonymous subclasses.
 */
public class GameObjects {

    private GameObjects() {}

    public static GameObject wall(Vec2 pos) {
        return new WallObject(pos);
    }

    public static GameObject crate(Vec2 pos) {
        return new CrateObject(pos);
    }

    /** Generic key item (passable, TAKE action). */
    public static GameObject key(Vec2 pos) {
        return new ItemObject("Key", pos, "KEY");
    }

    /** Generic gem item (passable, TAKE action). */
    public static GameObject gem(Vec2 pos) {
        return new ItemObject("Gem", pos, "GEM");
    }

    /**
     * Red potion: passable, TAKE + EAT (restores +5 HP, consumes the potion).
     */
    public static GameObject redPotion(Vec2 pos) {
        ItemObject potion = new ItemObject("Red Potion", pos, "POTION");
        potion.addAction(new Action("Drink Red Potion",
            new ModifyStatEffect(StatType.HP, 5, true)));
        return potion;
    }
}
