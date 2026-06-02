package com.kurawler.game.effect;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.objects.GameObject;

public interface Effect {
    void apply(GameEngine engine, GameObject subject);
}
