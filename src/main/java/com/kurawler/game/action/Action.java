package com.kurawler.game.action;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.effect.Effect;
import com.kurawler.game.objects.GameObject;

/**
 * A named action that can be performed on a GameObject (spec §1.2).
 *
 * An Action knows its display label and holds one or more Effects to execute.
 * New behaviours are added by creating new Effect instances – no Action subclasses needed.
 */
public class Action {

    private final String   label;
    private final Effect[] effects;

    public Action(String label, Effect... effects) {
        this.label   = label;
        this.effects = effects;
    }

    public String getLabel() { return label; }

    /** Execute all effects in order. */
    public void execute(GameEngine engine, GameObject subject) {
        for (Effect e : effects) e.apply(engine, subject);
    }

    @Override
    public String toString() { return label; }
}
