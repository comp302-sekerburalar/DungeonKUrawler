package com.kurawler.game.action;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.effect.Effect;
import com.kurawler.game.objects.GameObject;

public class Action {

    private final String label;
    private final Effect[] effects;

    public Action(String label, Effect... effects) {
        this.label = label;
        this.effects = effects;
    }

    public String getLabel() {
        return label;
    }

    public void execute(GameEngine engine, GameObject subject) {
        for (Effect e : effects)
            e.apply(engine, subject);
    }

    @Override
    public String toString() {
        return label;
    }
}
