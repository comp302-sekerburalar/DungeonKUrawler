package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import com.kurawler.components.*;

public class WelcomeScreen extends BaseScreen {

    private Text txtHero;
    private String heroName = "HERO";

    public WelcomeScreen(ScreenManager manager) { super(manager); }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());

        VBox center = new VBox(0); center.setAlignment(Pos.CENTER); center.setMaxWidth(480);

        Pane stoneTop = PixelBorder.stoneTop(480);

        VBox panel = new VBox(14); panel.setPadding(new Insets(28,36,28,36)); panel.setAlignment(Pos.CENTER); panel.getStyleClass().add("panel-surface"); panel.setMaxWidth(480);

        Text badge = new Text("[ HERO UNLOCKED ]"); badge.getStyleClass().add("welcome-badge");
        txtHero = new Text("WELCOME, " + heroName + "!"); txtHero.getStyleClass().add("welcome-title");
        Text sub = new Text("YOUR ADVENTURE BEGINS..."); sub.getStyleClass().add("welcome-subtitle");

        Region div = new Region(); div.setPrefHeight(2); div.setMaxWidth(Double.MAX_VALUE); div.getStyleClass().add("divider");

        VBox brief = new VBox(6);
        brief.setAlignment(Pos.CENTER_LEFT);
        for (String l : new String[]{"▸  FIND THE HIDDEN RELIC TO WIN.","▸  DEFEAT KNIGHTS & SORCERERS.","▸  DON'T LET YOUR HP REACH ZERO.","▸  COLLECT WEAPONS, ARMOUR & POTIONS."}) {
            Text t = new Text(l); t.getStyleClass().add("briefing-text"); brief.getChildren().add(t);
        }

        DungeonButton btnBegin = new DungeonButton("⚔  BEGIN QUEST", true); btnBegin.setMaxWidth(Double.MAX_VALUE); btnBegin.setOnAction(e->manager.showMapSelection(heroName));
        DungeonButton btnBack  = new DungeonButton("◄  BACK"); btnBack.setMaxWidth(Double.MAX_VALUE); btnBack.setOnAction(e->manager.showMainMenu());

        panel.getChildren().addAll(badge, txtHero, sub, div, brief, btnBegin, btnBack);

        Pane stoneBottom = PixelBorder.stoneBottom(480);
        center.getChildren().addAll(stoneTop, panel, stoneBottom);
        root.getChildren().add(center);
        root.setOnKeyPressed(e->{switch(e.getCode()){case ENTER->manager.showMapSelection(heroName);case ESCAPE->manager.showMainMenu();default->{}}});
        root.setFocusTraversable(true);
        return root;
    }

    public void setHeroName(String name) {
        this.heroName = name;
        if (txtHero != null) txtHero.setText("WELCOME, " + name + "!");
    }
}
