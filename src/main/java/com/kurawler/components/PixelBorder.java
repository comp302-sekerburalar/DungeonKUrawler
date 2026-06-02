package com.kurawler.components;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public final class PixelBorder {
    private static final double TILE_W = 18, TILE_H = 10;
    private static final Color SA = Color.web("#3d2a2a"), SB = Color.web("#2e1e1e"), BR = Color.web("#6b3a2a");

    private PixelBorder() {
    }

    public static Pane stoneTop(double w) {
        return stone(w, true);
    }

    public static Pane stoneBottom(double w) {
        return stone(w, false);
    }

    private static Pane stone(double w, boolean top) {
        Pane pane = new Pane();
        pane.setPrefSize(w, TILE_H + 2);
        pane.setMaxWidth(w);
        Rectangle border = new Rectangle(0, top ? 0 : 1, w, TILE_H + 1);
        border.setFill(BR);
        pane.getChildren().add(border);
        int count = (int) Math.ceil(w / TILE_W);
        for (int i = 0; i < count; i++) {

            double tileWidth = Math.min(TILE_W - 2,
                    w - (i * TILE_W));

            if (tileWidth <= 0)
                continue;

            Rectangle tile = new Rectangle(
                    i * TILE_W,
                    top ? 1 : 0,
                    tileWidth,
                    TILE_H - 1);

            tile.setFill(i % 2 == 0 ? SA : SB);
            pane.getChildren().add(tile);
        }
        return pane;
    }
}
