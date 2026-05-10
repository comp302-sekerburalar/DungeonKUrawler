package com.kurawler.game.effect;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.entity.Hero;
import com.kurawler.game.objects.GameObject;

/**
 * An Effect is the game-state change produced when an Action is executed.
 *
 * Separating Effect from Action means N actions × M effects without NxM subclasses
 * (spec §1.2 hint).
 */

/**
 * Additionally, we implemented the Strategy Pattern here to separate
 * different effect behaviors from the game engine logic.
 *
 * Each effect (such as damage, healing, or item pickup)
 * provides its own implementation of the apply method.
 * This makes the system easier to extend and maintain.
 */


public interface Effect {
    /**
     * Apply this effect.
     * @param engine  the running game engine (access to map, hero, enemies …)
     * @param subject the object the action was performed on
     */
    void apply(GameEngine engine, GameObject subject);
}
