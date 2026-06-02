package com.kurawler.screens;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.util.Duration;
import com.kurawler.components.PixelBorder;

public class WaveSurvivalScreen extends BaseScreen {

    public WaveSurvivalScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());
        root.getChildren().add(buildParticles());

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(520);

        // Header
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 16, 0));
        HBox waves = new HBox(8);
        waves.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            Text w = new Text("~");
            w.setFont(Font.font("Courier New", FontWeight.BOLD, 18));
            w.setFill(Color.web("#7ee8fa"));
            int fi = i;
            FadeTransition ft = new FadeTransition(Duration.millis(600), w);
            ft.setFromValue(0.3);
            ft.setToValue(1);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setDelay(Duration.millis(fi * 120));
            ft.play();
            waves.getChildren().add(w);
        }
        Text title = new Text("WAVE SURVIVAL MODE");
        title.setFont(Font.font("Courier New", FontWeight.BOLD, 20));
        title.setFill(Color.web("#7ee8fa"));
        Text sub = new Text("HOW LONG CAN YOU SURVIVE?");
        sub.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        sub.setFill(Color.web("#1a8fa8"));
        header.getChildren().addAll(waves, title, sub);

        Pane stoneTop = PixelBorder.stoneTop(520);

        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-surface");
        panel.setMaxWidth(520);

        // Stats strip
        HBox strip = new HBox(0);
        strip.setStyle("-fx-background-color:#0d3a4a;");
        for (String[] s : new String[][] { { "ENDLESS", "waves of enemies" }, { "NO RELIC", "pure combat" },
                { "SCORE", "waves survived" } }) {
            VBox cell = new VBox(3);
            cell.setAlignment(Pos.CENTER);
            cell.setPadding(new Insets(12, 0, 12, 0));
            HBox.setHgrow(cell, Priority.ALWAYS);
            Text v = new Text(s[0]);
            v.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            v.setFill(Color.web("#7ee8fa"));
            Text l = new Text(s[1]);
            l.setFont(Font.font("Courier New", 9));
            l.setFill(Color.web("#1a8fa8"));
            cell.getChildren().addAll(v, l);
            if (!strip.getChildren().isEmpty()) {
                Region sep = new Region();
                sep.setPrefWidth(1);
                sep.setStyle("-fx-background-color:#1a5a70;");
                strip.getChildren().add(sep);
            }
            strip.getChildren().add(cell);
        }

        // Divider
        Region d1 = new Region();
        d1.setPrefHeight(1);
        d1.setMaxWidth(Double.MAX_VALUE);
        d1.setStyle("-fx-background-color:#3d2a2a;");

        Text diffLabel = new Text("  SELECT DIFFICULTY");
        diffLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        diffLabel.setFill(Color.web("#c9a227"));
        HBox dlw = new HBox(diffLabel);
        dlw.setPadding(new Insets(10, 0, 8, 0));
        dlw.setStyle("-fx-background-color:#2a1a1a;");

        HBox diffRow = new HBox(12);
        diffRow.setPadding(new Insets(0, 16, 16, 16));
        diffRow.setStyle("-fx-background-color:#2a1a1a;");
        for (String[][] d : new String[][][] {
                { { "RECRUIT", "#2ecc71", "#0a3a1a" }, { "Slow spawns", "More HP", "Easier" } },
                { { "VETERAN", "#c9a227", "#3a2a00" }, { "Normal spawns", "Base stats", "Balanced" } },
                { { "NIGHTMARE", "#e74c3c", "#3a0a0a" }, { "Fast spawns", "Reduced HP", "Brutal" } }
        }) {
            String lbl = d[0][0], acc = d[0][1], bg = d[0][2];
            Button btn = new Button();
            VBox content = new VBox(4);
            content.setAlignment(Pos.CENTER);
            Text n = new Text(lbl);
            n.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
            n.setFill(Color.web(acc));
            VBox desc = new VBox(2);
            desc.setAlignment(Pos.CENTER);
            for (int i = 1; i < d.length; i++) {
                Text t = new Text(d[i][0]);
                t.setFont(Font.font("Courier New", 9));
                t.setFill(Color.web("#8a7060"));
                desc.getChildren().add(t);
            }
            content.getChildren().addAll(n, desc);
            btn.setGraphic(content);
            btn.setPrefSize(148, 80);
            String norm = "-fx-background-color:" + bg + ";-fx-border-color:" + acc
                    + ";-fx-border-width:1.5;-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
            String hov = "-fx-background-color:" + acc + "22;-fx-border-color:" + acc
                    + ";-fx-border-width:2;-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
            btn.setStyle(norm);
            btn.setOnMouseEntered(e -> btn.setStyle(hov));
            btn.setOnMouseExited(e -> btn.setStyle(norm));
            String captured = lbl;
            btn.setOnAction(e -> manager.startWaveSurvival(captured));
            HBox.setHgrow(btn, Priority.ALWAYS);
            diffRow.getChildren().add(btn);
        }

        Region d2 = new Region();
        d2.setPrefHeight(1);
        d2.setMaxWidth(Double.MAX_VALUE);
        d2.setStyle("-fx-background-color:#3d2a2a;");

        HBox infoRow = new HBox(0);
        infoRow.setPadding(new Insets(10, 16, 10, 16));
        infoRow.setStyle("-fx-background-color:#1e0e0e;");
        VBox info = new VBox(4);
        for (String line : new String[] { "▸ Each wave spawns faster and more numerous enemies.",
                "▸ Your score is the wave number you survive to.",
                "▸ Collect items between enemy spawns to stay alive." }) {
            Text t = new Text(line);
            t.setFont(Font.font("Courier New", 10));
            t.setFill(Color.web("#8a7060"));
            t.setWrappingWidth(480);
            info.getChildren().add(t);
        }
        infoRow.getChildren().add(info);

        panel.getChildren().addAll(strip, d1, dlw, diffRow, d2, infoRow);

        Pane stoneBottom = PixelBorder.stoneBottom(520);
        Button back = new Button("◄  BACK TO MAIN MENU");
        back.getStyleClass().add("link-btn");
        VBox bw = new VBox(back);
        bw.setAlignment(Pos.CENTER);
        bw.setPadding(new Insets(10, 0, 0, 0));
        back.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(header, stoneTop, panel, stoneBottom, bw);
        root.getChildren().add(center);
        root.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                manager.showMainMenu();
        });
        root.setFocusTraversable(true);
        return root;
    }

    private Pane buildParticles() {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        layer.setPrefSize(W(), H());
        for (int i = 0; i < 30; i++) {
            Circle c = new Circle(1.5, Color.web("#7ee8fa", 0.18));
            c.setLayoutX(Math.random() * 800);
            c.setLayoutY(Math.random() * 600);
            FadeTransition ft = new FadeTransition(Duration.millis(1500 + Math.random() * 2500), c);
            ft.setFromValue(0.05);
            ft.setToValue(0.5);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setDelay(Duration.millis(Math.random() * 3000));
            ft.play();
            layer.getChildren().add(c);
        }
        return layer;
    }
}
