package com.kurawler.components;

import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

public class TorchAnimation extends Pane {

    private static final double W = 22, H = 56;

    public TorchAnimation() {
        setPrefSize(W, H);
        setMinSize(W, H);
        setMaxSize(W, H);
        build();
    }

    private void build() {
        Polygon outer = new Polygon(W / 2, 0, W, 14, W * .9, 22, W * .1, 22, 0, 14);
        outer.setFill(Color.web("#c0392b"));
        outer.setOpacity(0.85);

        Polygon inner = new Polygon(W / 2, 4, W * .8, 14, W * .75, 22, W * .25, 22, W * .2, 14);
        inner.setFill(Color.web("#c9a227"));

        Rectangle body = new Rectangle((W - 8) / 2, 22, 8, 24);
        body.setFill(Color.web("#5a3a1a"));
        body.setStroke(Color.web("#3a2010"));
        body.setStrokeWidth(1);

        Rectangle base = new Rectangle((W - 14) / 2, 46, 14, 5);
        base.setFill(Color.web("#3a2010"));

        getChildren().addAll(outer, inner, body, base);

        ScaleTransition ft = new ScaleTransition(Duration.millis(120), outer);
        ft.setFromX(1.0);
        ft.setToX(0.85);
        ft.setFromY(1.0);
        ft.setToY(1.12);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.play();

        FadeTransition fade = new FadeTransition(Duration.millis(300), inner);
        fade.setFromValue(0.8);
        fade.setToValue(1.0);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.play();
    }
}
