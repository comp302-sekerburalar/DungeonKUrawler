package com.kurawler.components;

import javafx.scene.control.Button;

/**
 * Styled pixel-art button used throughout the dungeon UI.
 *
 * @param label   button text (already upper-cased by convention)
 * @param primary when true applies the red primary style, otherwise stone style
 */
public class DungeonButton extends Button {

    public DungeonButton(String label) {
        this(label, false);
    }

    public DungeonButton(String label, boolean primary) {
        super(label);
        getStyleClass().add("dungeon-btn");
        if (primary) {
            getStyleClass().add("dungeon-btn-primary");
        }
        setFocusTraversable(true);
    }
}
