package com.kurawler.components;

import javafx.scene.control.Button;

public class DungeonButton extends Button {
    public DungeonButton(String label) {
        this(label, false);
    }

    public DungeonButton(String label, boolean primary) {
        super(label);
        getStyleClass().add("dungeon-btn");
        if (primary)
            getStyleClass().add("dungeon-btn-primary");
        setFocusTraversable(true);
    }
}
