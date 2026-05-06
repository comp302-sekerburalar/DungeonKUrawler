package com.kurawler.screens;

import javafx.scene.layout.Pane;

/**
 * Abstract base for every screen in the game.
 * Each subclass builds its own UI in buildUI() and exposes it via getView().
 */
public abstract class BaseScreen {

    protected final ScreenManager manager;
    protected Pane view;

    /**
     * Standard constructor: immediately calls buildUI().
     * Safe for all screens that have no fields to initialise before buildUI().
     */
    protected BaseScreen(ScreenManager manager) {
        this.manager = manager;
        this.view    = buildUI();
    }

    /**
     * Deferred constructor for subclasses that need to initialise their own
     * fields BEFORE buildUI() runs (e.g. GameScreen must assign 'engine' first).
     * The subclass MUST call initView() at the end of its own constructor.
     *
     * @param deferBuild pass {@code true} to skip the buildUI() call here
     */
    protected BaseScreen(ScreenManager manager, boolean deferBuild) {
        this.manager = manager;
        // view stays null until the subclass calls initView()
    }

    /** Subclass calls this after all its own fields are initialised. */
    protected final void initView() {
        this.view = buildUI();
    }

    /** Construct and return the root pane for this screen. */
    protected abstract Pane buildUI();

    public Pane getView() { return view; }
}
