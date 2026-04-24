package com.kurawler.game.objects;

import com.kurawler.engine.Vec2;
import com.kurawler.game.action.Action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every entity placed on the map (spec §2).
 *
 * Subclasses set:
 *   blocksMovement – true for walls/crates (STATIC), false for items
 *   actions        – what the player can do when adjacent
 */
public abstract class GameObject {

    private final String      name;
    private       Vec2        pos;
    private final boolean     blocksMovement;
    private final List<Action> actions = new ArrayList<>();

    protected GameObject(String name, Vec2 pos, boolean blocksMovement) {
        this.name           = name;
        this.pos            = pos;
        this.blocksMovement = blocksMovement;
    }

    // ---------- Identity ----------

    public String getName()          { return name; }
    public boolean blocksMovement()  { return blocksMovement; }

    // ---------- Position ----------

    public Vec2 getPos()             { return pos; }
    public void setPos(Vec2 pos)     { this.pos = pos; }

    // ---------- Actions ----------

    public void addAction(Action a)              { actions.add(a); }
    public List<Action> getActions()             { return Collections.unmodifiableList(actions); }
    public boolean hasActions()                  { return !actions.isEmpty(); }

    // ---------- Display hint for renderer ----------

    /** Short code used by the renderer to pick a colour / glyph. */
    public abstract String renderTag();

    @Override
    public String toString() { return name + "@" + pos; }
}
