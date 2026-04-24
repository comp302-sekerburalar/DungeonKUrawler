package com.kurawler.game.effect;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.entity.StatType;
import com.kurawler.game.objects.GameObject;

/**
 * Modifies one of the hero's stats by {@code delta} (positive = increase, negative = decrease).
 * Optionally consumes the subject object (e.g. a potion disappears after use).
 */
public class ModifyStatEffect implements Effect {

    private final StatType stat;
    private final int      delta;
    private final boolean  consumeSubject;

    public ModifyStatEffect(StatType stat, int delta, boolean consumeSubject) {
        this.stat           = stat;
        this.delta          = delta;
        this.consumeSubject = consumeSubject;
    }

    @Override
    public void apply(GameEngine engine, GameObject subject) {
        int before = engine.getHero().getStat(stat);
        engine.getHero().modifyStat(stat, delta);
        int after = engine.getHero().getStat(stat);

        String sign = delta >= 0 ? "+" : "";
        engine.postMessage(stat.displayName() + " " + sign + delta +
                           "  (" + before + " → " + after + ")");

        if (consumeSubject) {
            engine.getHero().getInventory().remove(subject);
            engine.getMap().removeObject(subject); // in case it was on the ground
            engine.postMessage("Used: " + subject.getName());
        }

        engine.notifyStatsChanged();
    }
}
