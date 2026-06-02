package com.kurawler.components;

import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

/**
 * A small animated pixel-art torch made entirely from JavaFX shapes.
 * The outer flame flickers using a continuous ScaleTransition.
 */
public class TorchAnimation extends Pane {

    private static final double TOTAL_H = 56;
    private static final double TOTAL_W = 22;

    public TorchAnimation() {
        setPrefSize(TOTAL_W, TOTAL_H);
        setMinSize(TOTAL_W, TOTAL_H);
        setMaxSize(TOTAL_W, TOTAL_H);
        build();
    }

    private void build() {
        // --- Outer flame (red/orange) ---
        Polygon outerFlame = new Polygon(
            TOTAL_W / 2.0, 0,
            TOTAL_W,       14,
            TOTAL_W * 0.9, 22,
            TOTAL_W * 0.1, 22,
            0,             14
        );
        outerFlame.setFill(Color.web("#c0392b"));
        outerFlame.setOpacity(0.85);
        outerFlame.setLayoutX(0);
        outerFlame.setLayoutY(0);

        // --- Inner flame (gold) ---
        Polygon innerFlame = new Polygon(
            TOTAL_W / 2.0, 4,
            TOTAL_W * 0.8, 14,
            TOTAL_W * 0.75, 22,
            TOTAL_W * 0.25, 22,
            TOTAL_W * 0.2, 14
        );
        innerFlame.setFill(Color.web("#c9a227"));
        innerFlame.setLayoutX(0);
        innerFlame.setLayoutY(0);

        // --- Torch body ---
        Rectangle body = new Rectangle(
            (TOTAL_W - 8) / 2.0, 22,
            8, 24
        );
        body.setFill(Color.web("#5a3a1a"));
        body.setStroke(Color.web("#3a2010"));
        body.setStrokeWidth(1);
        body.setArcWidth(2);
        body.setArcHeight(2);

        // --- Torch base ---
        Rectangle base = new Rectangle(
            (TOTAL_W - 14) / 2.0, 46,
            14, 5
        );
        base.setFill(Color.web("#3a2010"));

        getChildren().addAll(outerFlame, innerFlame, body, base);

        // --- Flicker animation on the outer flame ---
        ScaleTransition flicker = new ScaleTransition(Duration.millis(120), outerFlame);
        flicker.setFromX(1.0); flicker.setToX(0.85);
        flicker.setFromY(1.0); flicker.setToY(1.12);
        flicker.setAutoReverse(true);
        flicker.setCycleCount(Animation.INDEFINITE);
        flicker.play();

        // --- Slower pulse on the inner flame ---
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), innerFlame);
        pulse.setFromX(0.9); pulse.setToX(1.05);
        pulse.setFromY(0.95); pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setDelay(Duration.millis(60));
        pulse.play();

        // Glow-like opacity flicker
        FadeTransition glowFade = new FadeTransition(Duration.millis(300), innerFlame);
        glowFade.setFromValue(0.8);
        glowFade.setToValue(1.0);
        glowFade.setAutoReverse(true);
        glowFade.setCycleCount(Animation.INDEFINITE);
        glowFade.play();
    }
}
