package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import com.kurawler.components.PixelBorder;

/**
 * Help / how-to-play screen explaining core game mechanics.
 */
public class HelpScreen extends BaseScreen {

    public HelpScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(800, 600);

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(540);

        // Screen title
        Text title = new Text("HOW TO PLAY");
        title.getStyleClass().add("title-line2-small");
        VBox titleWrap = new VBox(title);
        titleWrap.setAlignment(Pos.CENTER);
        titleWrap.setPadding(new Insets(0, 0, 14, 0));

        Pane stoneTop = PixelBorder.stoneTop(540);

        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-surface");
        panel.setMaxWidth(540);

        String[][] entries = {
            {"MOVEMENT",    "ARROW KEYS",    "Move your hero N / S / E / W through the dungeon grid."},
            {"INTERACT",    "MOUSE CLICK",   "Click objects in the 3×3 area around you to see TAKE, EAT, WEAR, BREAK, SEARCH actions."},
            {"INVENTORY",   "[ I ] KEY",     "Open your 8-slot inventory. Equip weapons & armor to boost STR, DEF and HP."},
            {"COMBAT",      "CLICK ENEMY",   "Equip a weapon and click an adjacent enemy to attack. Damage depends on STR, ATK and DEF stats."},
            {"PAUSE",       "[ P ] KEY",     "Pause and resume the game at any time during play."},
            {"OBJECTIVE",   "TARGET RELIC",  "Find the hidden item shown at game start. Survive enemies while you search!"}
        };

        for (int i = 0; i < entries.length; i++) {
            panel.getChildren().add(buildHelpRow(entries[i][0], entries[i][1], entries[i][2], i < entries.length - 1));
        }

        Pane stoneBottom = PixelBorder.stoneBottom(540);

        Button btnBack = new Button("◄  BACK TO MAIN MENU");
        btnBack.getStyleClass().add("link-btn");
        VBox backWrap = new VBox(btnBack);
        backWrap.setAlignment(Pos.CENTER);
        backWrap.setPadding(new Insets(12, 0, 0, 0));
        btnBack.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(titleWrap, stoneTop, panel, stoneBottom, backWrap);
        root.getChildren().add(center);

        root.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) manager.showMainMenu();
        });
        root.setFocusTraversable(true);

        return root;
    }

    private HBox buildHelpRow(String category, String shortcut, String description, boolean withDivider) {
        HBox row = new HBox(16);
        row.setPadding(new Insets(14, 24, 14, 24));
        row.setAlignment(Pos.TOP_LEFT);
        if (withDivider) row.getStyleClass().add("help-row");

        VBox left = new VBox(4);
        left.setMinWidth(120);
        left.setMaxWidth(120);

        Text catText = new Text(category);
        catText.getStyleClass().add("help-category");

        Text keyText = new Text(shortcut);
        keyText.getStyleClass().add("help-shortcut");

        left.getChildren().addAll(catText, keyText);

        Text desc = new Text(description);
        desc.getStyleClass().add("help-desc");
        desc.setWrappingWidth(340);
        VBox right = new VBox(desc);
        right.setAlignment(Pos.TOP_LEFT);

        row.getChildren().addAll(left, right);
        return row;
    }
}
