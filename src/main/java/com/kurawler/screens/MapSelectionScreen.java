package com.kurawler.screens;

import com.kurawler.components.*;
import com.kurawler.engine.MapSerializer;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.List;

/**
 * Pre-game screen shown after login / "Begin Quest".
 *
 * Offers three paths (spec §4.1 bullet 1):
 * 1. RANDOM MAP – generate and play immediately
 * 2. DESIGN MAP – open the map editor
 * 3. LOAD MAP – pick a previously saved map and play it
 */
public class MapSelectionScreen extends BaseScreen {

    private final String heroName;

    public MapSelectionScreen(ScreenManager manager, String heroName) {
        super(manager);
        this.heroName = heroName;
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());
        root.getChildren().add(buildParticles());

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(560);

        // Header
        VBox header = buildHeader();

        Pane stoneTop = PixelBorder.stoneTop(560);

        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-surface");
        panel.setMaxWidth(560);

        // Mode cards row
        HBox cardRow = buildModeCards();
        cardRow.setPadding(new Insets(20, 20, 20, 20));
        cardRow.setStyle("-fx-background-color:#2a1a1a;");

        // Divider
        Region div = divider();

        // Saved maps section
        VBox savedSection = buildSavedMapsSection();

        panel.getChildren().addAll(cardRow, div, savedSection);

        Pane stoneBottom = PixelBorder.stoneBottom(560);

        Button btnBack = new Button("◄  BACK TO MAIN MENU");
        btnBack.getStyleClass().add("link-btn");
        VBox backWrap = new VBox(btnBack);
        backWrap.setAlignment(Pos.CENTER);
        backWrap.setPadding(new Insets(12, 0, 0, 0));
        btnBack.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(header, stoneTop, panel, stoneBottom, backWrap);
        root.getChildren().add(center);

        root.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                manager.showMainMenu();
        });
        root.setFocusTraversable(true);
        return root;
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private VBox buildHeader() {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 0, 14, 0));

        Text title = new Text("CHOOSE YOUR DUNGEON");
        title.setFont(Font.font("Courier New", FontWeight.BOLD, 18));
        title.setFill(Color.web("#c9a227"));

        Text sub = new Text("HERO: " + heroName);
        sub.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        sub.setFill(Color.web("#8a7060"));

        box.getChildren().addAll(title, sub);
        return box;
    }

    // ── Mode cards ────────────────────────────────────────────────────────────

    private HBox buildModeCards() {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);

        // Card 1: Random
        VBox randomCard = buildModeCard(
                "RANDOM MAP",
                "⚄",
                new String[] {
                        "Auto-generated dungeon",
                        "5 random items placed",
                        "1 hidden relic to find",
                        "Play immediately"
                },
                "#c9a227", "#3a2a00",
                () -> manager.startGame(heroName, null));

        // Card 2: Design
        VBox designCard = buildModeCard(
                "DESIGN MAP",
                "✏",
                new String[] {
                        "Draw walls & place items",
                        "Save your map to disk",
                        "Load & play anytime",
                        "Full editor controls"
                },
                "#3498db", "#0a1a3a",
                () -> manager.showMapEditor(heroName));

        row.getChildren().addAll(randomCard, designCard);
        return row;
    }

    private VBox buildModeCard(String title, String icon, String[] bullets,
            String accent, String bg, Runnable onClick) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 16, 16, 16));
        card.setPrefWidth(240);
        card.setPrefHeight(180);
        card.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-border-color:" + accent + ";" +
                        "-fx-border-width:2; -fx-cursor:hand;");

        Text iconText = new Text(icon);
        iconText.setFont(Font.font("Courier New", FontWeight.BOLD, 22));
        iconText.setFill(Color.web(accent));

        Text titleText = new Text(title);
        titleText.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        titleText.setFill(Color.web(accent));

        VBox bulletBox = new VBox(4);
        for (String b : bullets) {
            Text t = new Text("▸ " + b);
            t.setFont(Font.font("Courier New", 9));
            t.setFill(Color.web("#8a7060"));
            bulletBox.getChildren().add(t);
        }

        card.getChildren().addAll(iconText, titleText, bulletBox);

        // Hover effect
        String normalStyle = "-fx-background-color:" + bg + "; -fx-border-color:" + accent
                + "; -fx-border-width:2; -fx-cursor:hand;";
        String hoverStyle = "-fx-background-color:" + accent + "22; -fx-border-color:" + accent
                + "; -fx-border-width:2.5; -fx-cursor:hand;";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));
        card.setOnMouseClicked(e -> onClick.run());

        return card;
    }

    // ── Saved maps section ────────────────────────────────────────────────────

    private VBox buildSavedMapsSection() {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:#1e0e0e;");

        // Header bar
        HBox headerRow = new HBox();
        headerRow.setPadding(new Insets(8, 16, 6, 16));
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-border-color:transparent transparent #3d2a2a transparent; -fx-border-width:0 0 1 0;");

        Text savedTitle = new Text("SAVED MAPS");
        savedTitle.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        savedTitle.setFill(Color.web("#c9a227"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(savedTitle, spacer);
        section.getChildren().add(headerRow);

        List<String[]> maps = MapSerializer.listSavedMaps();

        if (maps.isEmpty()) {
            Text empty = new Text("  No saved maps yet. Design one and save it!");
            empty.setFont(Font.font("Courier New", 10));
            empty.setFill(Color.web("#3d2a2a"));
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(12, 16, 12, 16));
            section.getChildren().add(wrap);
        } else {
            // Show up to 4 saved maps
            int count = Math.min(4, maps.size());
            for (int i = 0; i < count; i++) {
                String[] entry = maps.get(i);
                section.getChildren().add(buildSavedMapRow(entry[0], entry[1]));
            }
        }
        return section;
    }

    private HBox buildSavedMapRow(String name, String savedAt) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-border-color:transparent transparent #2a1a1a transparent; -fx-border-width:0 0 1 0; -fx-cursor:hand;");

        Text mapIcon = new Text("⊞");
        mapIcon.setFont(Font.font("Courier New", 14));
        mapIcon.setFill(Color.web("#6b3a2a"));

        VBox info = new VBox(2);
        Text nameText = new Text(name);
        nameText.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        nameText.setFill(Color.web("#e8d5b0"));
        Text dateText = new Text(savedAt.isEmpty() ? "" : "Saved: " + savedAt);
        dateText.setFont(Font.font("Courier New", 8));
        dateText.setFill(Color.web("#8a7060"));
        info.getChildren().addAll(nameText, dateText);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnPlay = smallBtn("▶ PLAY", "#2ecc71");
        Button btnEdit = smallBtn("✏ EDIT", "#3498db");
        Button btnDelete = smallBtn("✕", "#e74c3c");

        btnPlay.setOnAction(e -> {
            try {
                manager.startGame(heroName, MapSerializer.load(name));
            } catch (Exception ex) {
                showErr("Could not load map: " + ex.getMessage());
            }
        });
        btnEdit.setOnAction(e -> {
            try {
                manager.showMapEditor(heroName, MapSerializer.load(name), name);
            } catch (Exception ex) {
                showErr("Could not load map: " + ex.getMessage());
            }
        });
        btnDelete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Map");
            confirm.setHeaderText("Delete \"" + name + "\"?");
            confirm.setContentText("This cannot be undone.");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    try {
                        Path p = Path.of(System.getProperty("user.home"), ".kurawler", "maps",
                                name.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".json");
                        java.nio.file.Files.deleteIfExists(p);
                        // Rebuild the screen
                        manager.showMapSelection(heroName);
                    } catch (Exception ex) {
                        showErr(ex.getMessage());
                    }
                }
            });
        });

        row.getChildren().addAll(mapIcon, info, btnPlay, btnEdit, btnDelete);

        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color:#2a1a1a; -fx-border-color:transparent transparent #2a1a1a transparent; -fx-border-width:0 0 1 0; -fx-cursor:hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-border-color:transparent transparent #2a1a1a transparent; -fx-border-width:0 0 1 0; -fx-cursor:hand;"));

        return row;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Button smallBtn(String label, String color) {
        Button btn = new Button(label);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
        btn.setStyle(
                "-fx-background-color:transparent; -fx-border-color:" + color + ";" +
                        "-fx-border-width:1; -fx-text-fill:" + color + "; -fx-padding:3 7 3 7;" +
                        "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + color + "33; -fx-border-color:" + color + ";" +
                        "-fx-border-width:1; -fx-text-fill:" + color + "; -fx-padding:3 7 3 7;" +
                        "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:transparent; -fx-border-color:" + color + ";" +
                        "-fx-border-width:1; -fx-text-fill:" + color + "; -fx-padding:3 7 3 7;" +
                        "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;"));
        return btn;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#3d2a2a;");
        return r;
    }

    private void showErr(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Map Error");
        a.setContentText(msg);
        a.showAndWait();
    }

    private Pane buildParticles() {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        layer.setPrefSize(W(), H());
        for (int i = 0; i < 25; i++) {
            Circle c = new Circle(1.5, Color.web("#c9a227", 0.2));
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
