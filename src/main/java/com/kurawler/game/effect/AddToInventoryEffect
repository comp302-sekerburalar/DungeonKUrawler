package com.kurawler.game.effect;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.entity.Hero;
import com.kurawler.game.objects.GameObject;

/** Moves the subject from the map into the hero's inventory. */
public class AddToInventoryEffect implements Effect {
    @Override
    public void apply(GameEngine engine, GameObject subject) {
        Hero hero = engine.getHero();
        if (hero.getInventory().isFull()) {
            engine.postMessage("Inventory is full! Cannot pick up " + subject.getName() + ".");
            return;
        }
        engine.getMap().removeObject(subject);
        hero.getInventory().add(subject);
        engine.postMessage("Picked up: " + subject.getName());
        engine.notifyMapChanged();
    }
}