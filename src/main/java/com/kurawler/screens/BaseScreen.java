package com.kurawler.screens;

import javafx.scene.layout.Pane;

/**
 * Abstract base for every screen in the game.
 * Each subclass builds its own UI in buildUI() and exposes it via getView().
 */
public abstract class BaseScreen {

    protected final ScreenManager manager;
    protected Pane view;

    protected BaseScreen(ScreenManager manager) {
        this.manager = manager;
        this.view    = buildUI();
    }

    /** Construct and return the root pane for this screen. */
    protected abstract Pane buildUI();

    public Pane getView() { return view; }
}
