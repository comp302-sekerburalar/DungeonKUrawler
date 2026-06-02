package com.kurawler.screens;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import com.kurawler.components.DungeonButton;
import com.kurawler.components.TorchAnimation;
import com.kurawler.components.PixelBorder;

import java.util.Optional;

/**
 * Main menu screen with animated torches, title and navigation buttons.
 */
public class MainMenuScreen extends BaseScreen {

    public MainMenuScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        // Root
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(800, 600);

        // Particle / star background
        Pane starLayer = buildStarLayer();
        root.getChildren().add(starLayer);

        // Center column
        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(480);
        StackPane.setAlignment(center, Pos.CENTER);

        // Title
        VBox titleBox = buildTitle();

        // Torch row
        HBox torchRow = buildTorchRow();
        torchRow.setPadding(new Insets(0, 0, 0, 0));

        // Stone top cap
        Pane stoneTop = PixelBorder.stoneTop(480);

        // Button panel
        VBox btnPanel = new VBox(10);
        btnPanel.setAlignment(Pos.CENTER);
        btnPanel.setPadding(new Insets(24, 32, 24, 32));
        btnPanel.getStyleClass().add("panel-surface");
        btnPanel.setMaxWidth(480);

        DungeonButton btnNew      = new DungeonButton("⚔  START NEW GAME", true);
        DungeonButton btnContinue = new DungeonButton("▶  CONTINUE");
        DungeonButton btnHelp     = new DungeonButton("?  HELP");
        DungeonButton btnCredits  = new DungeonButton("★  CREDITS");
        DungeonButton btnExit     = new DungeonButton("✕  EXIT");

        btnNew.setOnAction(e -> manager.showLogin());
        btnContinue.setOnAction(e -> manager.showLogin());
        btnHelp.setOnAction(e -> manager.showHelp());
        btnCredits.setOnAction(e -> showCredits());
        btnExit.setOnAction(e -> confirmExit());

        for (DungeonButton b : new DungeonButton[]{btnNew, btnContinue, btnHelp, btnCredits, btnExit}) {
            b.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(b, Priority.NEVER);
        }

        btnPanel.getChildren().addAll(btnNew, btnContinue, btnHelp, btnCredits, btnExit);

        // Stone bottom cap
        Pane stoneBottom = PixelBorder.stoneBottom(480);

        // Footer hint
        Text footer = new Text("ARROW KEYS: NAVIGATE   ENTER: SELECT   ESC: BACK");
        footer.getStyleClass().add("footer-text");

        center.getChildren().addAll(titleBox, torchRow, stoneTop, btnPanel, stoneBottom, footer);
        root.getChildren().add(center);

        // Scanline overlay
        root.getChildren().add(buildScanlines());

        // Keyboard shortcuts
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> manager.showLogin();
                case H     -> manager.showHelp();
                case ESCAPE, F4 -> confirmExit();
                default -> {}
            }
        });
        root.setFocusTraversable(true);

        return root;
    }

    // ---------- Sub-builders ----------

    private VBox buildTitle() {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 0, 24, 0));

        Text line1 = new Text("DUNGEON");
        line1.getStyleClass().add("title-line1");

        Text line2 = new Text("KURAWLER");
        line2.getStyleClass().add("title-line2");

        Text sub = new Text("COMP 302  ·  SPRING 2026");
        sub.getStyleClass().add("title-subtitle");

        // Subtle pulse on the main title
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2), line2);
        pulse.setFromX(1.0); pulse.setToX(1.02);
        pulse.setFromY(1.0); pulse.setToY(1.02);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        box.getChildren().addAll(line1, line2, sub);
        return box;
    }

    private HBox buildTorchRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(480);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TorchAnimation left  = new TorchAnimation();
        TorchAnimation right = new TorchAnimation();
        left.setTranslateX(-8);
        right.setTranslateX(8);

        row.getChildren().addAll(left, spacer, right);
        return row;
    }

    private Pane buildStarLayer() {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        layer.setPrefSize(800, 600);

        for (int i = 0; i < 40; i++) {
            Circle star = new Circle(1.5, Color.web("#c9a227", 0.25));
            star.setLayoutX(Math.random() * 800);
            star.setLayoutY(Math.random() * 600);

            FadeTransition ft = new FadeTransition(
                Duration.millis(1200 + Math.random() * 2400), star
            );
            ft.setFromValue(0.1);
            ft.setToValue(0.7);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setDelay(Duration.millis(Math.random() * 3000));
            ft.play();

            layer.getChildren().add(star);
        }
        return layer;
    }

    private Region buildScanlines() {
        Region scanlines = new Region();
        scanlines.setPrefSize(800, 600);
        scanlines.setMouseTransparent(true);
        scanlines.getStyleClass().add("scanlines");
        return scanlines;
    }

    private void showCredits() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Credits");
        alert.setHeaderText("DUNGEON KURAWLER");
        alert.setContentText(
            "COMP 302 – Software Engineering\n" +
            "Koç University · Spring 2026\n\n" +
            "Built with Java & JavaFX\n" +
            "Dungeon Crawler Genre · OOP Design"
        );
        alert.showAndWait();
    }

    private void confirmExit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Exit Game");
        confirm.setHeaderText("ABANDON THE DUNGEON?");
        confirm.setContentText("Are you sure you want to exit?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            manager.exitGame();
        }
    }
}
