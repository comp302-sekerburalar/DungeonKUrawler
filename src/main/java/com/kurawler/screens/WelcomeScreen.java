package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import com.kurawler.components.DungeonButton;
import com.kurawler.components.PixelBorder;

/**
 * Welcome / pre-game screen shown after successful login or registration.
 * Displays the quest briefing and a "Begin Quest" button.
 */
public class WelcomeScreen extends BaseScreen {

    private Text  txtHeroName;
    private String heroName = "HERO";

    public WelcomeScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(800, 600);

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(480);

        // Stone top
        Pane stoneTop = PixelBorder.stoneTop(480);

        // Panel
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(32, 40, 32, 40));
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("panel-surface");
        panel.setMaxWidth(480);

        // Trophy emoji replacement with text
        Text trophy = new Text("[ HERO UNLOCKED ]");
        trophy.getStyleClass().add("welcome-badge");

        txtHeroName = new Text("WELCOME, " + heroName + "!");
        txtHeroName.getStyleClass().add("welcome-title");

        Text sub = new Text("YOUR ADVENTURE BEGINS...");
        sub.getStyleClass().add("welcome-subtitle");

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(2);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.getStyleClass().add("divider");

        // Quest briefing
        VBox briefing = buildBriefing();

        // Buttons
        DungeonButton btnBegin = new DungeonButton("⚔  BEGIN QUEST", true);
        DungeonButton btnBack  = new DungeonButton("◄  BACK TO MENU");

        btnBegin.setMaxWidth(Double.MAX_VALUE);
        btnBack.setMaxWidth(Double.MAX_VALUE);

        btnBegin.setOnAction(e -> manager.startGame(heroName));
        btnBack.setOnAction(e -> manager.showMainMenu());

        panel.getChildren().addAll(trophy, txtHeroName, sub, divider, briefing, btnBegin, btnBack);

        // Stone bottom
        Pane stoneBottom = PixelBorder.stoneBottom(480);

        center.getChildren().addAll(stoneTop, panel, stoneBottom);
        root.getChildren().add(center);

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER  -> manager.startGame(heroName);
                case ESCAPE -> manager.showMainMenu();
                default -> {}
            }
        });
        root.setFocusTraversable(true);

        return root;
    }

    private VBox buildBriefing() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        String[] lines = {
            "▸  FIND THE HIDDEN RELIC TO WIN.",
            "▸  DEFEAT KNIGHTS & SORCERERS.",
            "▸  DON'T LET YOUR HP REACH ZERO.",
            "▸  USE ITEMS, SPELLS & WEAPONS WISELY."
        };
        for (String line : lines) {
            Text t = new Text(line);
            t.getStyleClass().add("briefing-text");
            box.getChildren().add(t);
        }
        return box;
    }

    /** Update the hero name label before the screen is shown. */
    public void setHeroName(String name) {
        this.heroName = name;
        if (txtHeroName != null) {
            txtHeroName.setText("WELCOME, " + name + "!");
        }
    }
}
