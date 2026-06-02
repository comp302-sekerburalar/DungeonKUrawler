package com.kurawler.screens;

import com.kurawler.components.*;
import com.kurawler.util.SpriteRenderer;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Optional;

public class MainMenuScreen extends BaseScreen {

    public MainMenuScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        double W = W(), H = H();

        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W, H);
        root.getChildren().add(buildStarLayer(W, H));

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(520);

        VBox titleBox = buildTitle();

        // Torches
        HBox torchRow = new HBox();
        torchRow.setAlignment(Pos.CENTER);
        torchRow.setMaxWidth(520);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        TorchAnimation left = new TorchAnimation();
        left.setTranslateX(-8);
        TorchAnimation right = new TorchAnimation();
        right.setTranslateX(8);
        torchRow.getChildren().addAll(left, sp, right);

        Pane stoneTop = PixelBorder.stoneTop(520);

        VBox btnPanel = new VBox(10);
        btnPanel.setAlignment(Pos.CENTER);
        btnPanel.setPadding(new Insets(22, 36, 22, 36));
        btnPanel.getStyleClass().add("panel-surface");
        btnPanel.setMaxWidth(520);

        DungeonButton btnNew = new DungeonButton("⚔  START NEW GAME", true);
        DungeonButton btnContinue = new DungeonButton("▶  CONTINUE");

        // Wave Survival
        Button btnWave = new Button("〰  WAVE SURVIVAL MODE");
        styleBtn(btnWave, "#0d3a4a", "#1a8fa8", "#7ee8fa", false);
        btnWave.setOnMouseEntered(e -> styleBtn(btnWave, "#1a5a70", "#7ee8fa", "#ffffff", true));
        btnWave.setOnMouseExited(e -> styleBtn(btnWave, "#0d3a4a", "#1a8fa8", "#7ee8fa", false));
        btnWave.setOnAction(e -> manager.showWaveSurvival());
        btnWave.setMaxWidth(Double.MAX_VALUE);

        // Marketplace ── NEW
        Button btnMarket = new Button("🛒  MARKETPLACE");
        styleBtn(btnMarket, "#0a1a10", "#1a8a3a", "#2ecc71", false);
        btnMarket.setOnMouseEntered(e -> styleBtn(btnMarket, "#1a3a20", "#2ecc71", "#ffffff", true));
        btnMarket.setOnMouseExited(e -> styleBtn(btnMarket, "#0a1a10", "#1a8a3a", "#2ecc71", false));
        btnMarket.setOnAction(e -> {
            if (!manager.isLoggedIn()) {
                manager.showLogin();
                return;
            }

            manager.showMarketplace();
        });
        btnMarket.setMaxWidth(Double.MAX_VALUE);

        DungeonButton btnLoadout = new DungeonButton("⚔ LOADOUT");
        styleBtn(btnLoadout, "#0a1a10", "#8a1a1a", "#cc2e2e", false);
        btnLoadout.setOnMouseEntered(e -> styleBtn(btnLoadout, "#3a1a1a", "#cc2e2e", "#ffffff", true));
        btnLoadout.setOnMouseExited(e -> styleBtn(btnLoadout, "#0a1a10", "#8a1a1a", "#cc2e2e", false));
        btnLoadout.setOnAction(e -> {

            if (!manager.isLoggedIn()) {
                manager.showLogin();
                return;
            }

            manager.showLoadout();
        });
        btnLoadout.setMaxWidth(Double.MAX_VALUE);

        DungeonButton btnHelp = new DungeonButton("?  HELP");
        DungeonButton btnCredits = new DungeonButton("★  CREDITS");
        DungeonButton btnExit = new DungeonButton("✕  EXIT");

        btnNew.setOnAction(e -> {
            if (manager.isLoggedIn())
                manager.showWelcome(manager.getCurrentHero());
            else
                manager.showLogin();
        });

        btnContinue.setOnAction(e -> {
            if (manager.isLoggedIn())
                manager.showWelcome(manager.getCurrentHero());
            else
                manager.showLogin();
        });
        btnHelp.setOnAction(e -> manager.showHelp());
        btnCredits.setOnAction(e -> showCredits());
        btnExit.setOnAction(e -> confirmExit());

        for (DungeonButton b : new DungeonButton[] { btnNew, btnContinue, btnHelp, btnCredits, btnExit })
            b.setMaxWidth(Double.MAX_VALUE);

        btnPanel.getChildren().addAll(btnNew, btnContinue, btnWave, btnMarket, btnLoadout, btnHelp, btnCredits,
                btnExit);

        Pane stoneBottom = PixelBorder.stoneBottom(520);

        Text footer = new Text("ARROW KEYS: NAVIGATE   ENTER: SELECT   ESC: BACK");
        footer.getStyleClass().add("footer-text");

        center.getChildren().addAll(titleBox, torchRow, stoneTop, btnPanel, stoneBottom, footer);
        root.getChildren().add(center);

        Region scan = new Region();
        scan.setPrefSize(W, H);
        scan.setMouseTransparent(true);
        scan.getStyleClass().add("scanlines");
        root.getChildren().add(scan);

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    if (manager.isLoggedIn())
                        manager.showWelcome(manager.getCurrentHero());
                    else
                        manager.showLogin();
                }
                case H -> manager.showHelp();
                case M -> {
                    if (manager.isLoggedIn())
                        manager.showMarketplace();
                    else
                        manager.showLogin();
                }
                case ESCAPE -> confirmExit();
                default -> {
                }
            }
        });
        root.setFocusTraversable(true);
        return root;
    }

    private void styleBtn(Button btn, String bg, String border, String text, boolean hover) {
        btn.setStyle(
                "-fx-background-color:" + bg + "; -fx-border-color:" + border + ";" +
                        "-fx-border-width:2; -fx-text-fill:" + text + ";" +
                        "-fx-font-family:'Courier New'; -fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-padding:12 20; -fx-background-radius:0; -fx-border-radius:0;" +
                        "-fx-cursor:hand; -fx-effect:dropshadow(one-pass-box,#0a0505,4,0,3,3);");
    }

    private VBox buildTitle() {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 0, 20, 0));

        Text line1 = new Text("DUNGEON");
        line1.getStyleClass().add("title-line1");
        Text line2 = new Text("KURAWLER");
        line2.getStyleClass().add("title-line2");
        Text sub = new Text("COMP 302  \u00B7  SPRING 2026");
        sub.getStyleClass().add("title-subtitle");

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2), line2);
        pulse.setFromX(1.0);
        pulse.setToX(1.02);
        pulse.setFromY(1.0);
        pulse.setToY(1.02);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        box.getChildren().addAll(line1, line2, sub);
        return box;
    }

    private Pane buildStarLayer(double W, double H) {
        javafx.scene.layout.Pane layer = new javafx.scene.layout.Pane();
        layer.setMouseTransparent(true);
        layer.setPrefSize(W, H);
        for (int i = 0; i < 150; i++) {

            double size = 2 + Math.random() * 3;

            Circle star = new Circle(
                    size,
                    Color.web("#ffe082", 0.7));

            star.setEffect(new javafx.scene.effect.Glow(1.0));
            star.setLayoutX(Math.random() * W);
            star.setLayoutY(Math.random() * H);
            FadeTransition ft = new FadeTransition(Duration.millis(1200 + Math.random() * 2400), star);
            ft.setFromValue(0.1);
            ft.setToValue(1.0);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setDelay(Duration.millis(Math.random() * 3000));
            ft.play();
            layer.getChildren().add(star);
        }
        return layer;
    }

    private void showCredits() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Credits");
        a.setHeaderText("DUNGEON KURAWLER");
        a.setContentText(
                "COMP 302 \u2013 Software Engineering\nKo\u00e7 University \u00b7 Spring 2026\n\nBuilt with Java & JavaFX\nSprite assets included in project resources.");
        a.showAndWait();
    }

    private void confirmExit() {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Exit");
        c.setHeaderText("ABANDON THE DUNGEON?");
        c.setContentText("Are you sure you want to exit?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK)
            manager.exitGame();
    }
}
