package com.kurawler.screens;

import javafx.scene.layout.Pane;

/**
 * Abstract base for every screen.
 * Carries the resolved screen width/height so every subclass can size itself
 * correctly.
 */
public abstract class BaseScreen {

    protected final ScreenManager manager;
    protected Pane view;

    /** Normal constructor – calls buildUI() immediately. */
    protected BaseScreen(ScreenManager manager) {
        this.manager = manager;
        this.view = buildUI();
    }

    /**
     * Deferred constructor – subclass must call initView() after fields are ready.
     */
    protected BaseScreen(ScreenManager manager, boolean defer) {
        this.manager = manager;
    }

    protected final void initView() {
        this.view = buildUI();
    }

    protected abstract Pane buildUI();

    public Pane getView() {
        return view;
    }

    // ── Convenient dimension helpers ──────────────────────────────────────────
    protected double W() {
        return manager.getWidth();
    }

    protected double H() {
        return manager.getHeight();
    }
}
