package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import com.kurawler.components.PixelBorder;

public class HelpScreen extends BaseScreen {

    public HelpScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(560);

        Text title = new Text("HOW TO PLAY");
        title.getStyleClass().add("title-line2-small");
        VBox tw = new VBox(title);
        tw.setAlignment(Pos.CENTER);
        tw.setPadding(new Insets(0, 0, 14, 0));

        Pane stoneTop = PixelBorder.stoneTop(560);
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-surface");
        panel.setMaxWidth(560);

        String[][] entries = {
                { "MOVEMENT", "ARROW / WASD", "Move N/S/E/W on the grid." },
                { "INTERACT", "MOUSE CLICK",
                        "Click objects in the 3×3 area to see TAKE, EAT, WEAR, BREAK, SEARCH actions." },
                { "INVENTORY", "[ I ] KEY", "8 slots (2×4). Equip weapons & armour to boost stats." },
                { "COMBAT", "CLICK ENEMY", "Equip weapon first, then click adjacent enemy to attack." },
                { "OBJECTIVE", "FIND RELIC", "Find the hidden relic shown at game start before HP hits 0." },
                { "PAUSE", "[ ESC ]", "Pause the game and access the menu." }
        };

        for (int i = 0; i < entries.length; i++) {
            HBox row = new HBox(14);
            row.setPadding(new Insets(12, 20, 12, 20));
            row.setAlignment(Pos.TOP_LEFT);
            if (i < entries.length - 1)
                row.setStyle("-fx-border-color:transparent transparent #3d2a2a transparent; -fx-border-width:0 0 1 0;");

            VBox left = new VBox(3);
            left.setMinWidth(100);
            left.setMaxWidth(100);
            Text cat = new Text(entries[i][0]);
            cat.setFill(Color.web("#c9a227"));
            cat.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
            Text key = new Text(entries[i][1]);
            key.setFill(Color.web("#c0392b"));
            key.setFont(Font.font("Courier New", 8));
            left.getChildren().addAll(cat, key);

            Text desc = new Text(entries[i][2]);
            desc.setFill(Color.web("#8a7060"));
            desc.setFont(Font.font("Courier New", 12));
            desc.setWrappingWidth(380);
            row.getChildren().addAll(left, new VBox(desc));
            panel.getChildren().add(row);
        }

        Pane stoneBottom = PixelBorder.stoneBottom(560);
        Button back = new Button("◄  BACK TO MAIN MENU");
        back.getStyleClass().add("link-btn");
        VBox bw = new VBox(back);
        bw.setAlignment(Pos.CENTER);
        bw.setPadding(new Insets(12, 0, 0, 0));
        back.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(tw, stoneTop, panel, stoneBottom, bw);
        root.getChildren().add(center);
        root.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                manager.showMainMenu();
        });
        root.setFocusTraversable(true);
        return root;
    }
}
