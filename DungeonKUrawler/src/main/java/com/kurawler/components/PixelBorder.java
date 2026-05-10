package com.kurawler.components;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Factory for decorative stone tile cap bars placed above and below panels.
 * Creates a row of alternating stone-colored tiles to simulate dungeon masonry.
 */
public class PixelBorder {

    private static final double TILE_W  = 28;
    private static final double TILE_H  = 10;
    private static final Color  STONE_A = Color.web("#3d2a2a");
    private static final Color  STONE_B = Color.web("#2e1e1e");
    private static final Color  BORDER  = Color.web("#6b3a2a");

    private PixelBorder() {}

    public static Pane stoneTop(double totalWidth) {
        return stoneTile(totalWidth, true);
    }

    public static Pane stoneBottom(double totalWidth) {
        return stoneTile(totalWidth, false);
    }

    private static Pane stoneTile(double totalWidth, boolean isTop) {
        Pane pane = new Pane();
        pane.setPrefSize(totalWidth, TILE_H + 2);
        pane.setMaxWidth(totalWidth);

        // Outer border line
        Rectangle border = new Rectangle(0, isTop ? 0 : 1, totalWidth, TILE_H + 1);
        border.setFill(BORDER);
        pane.getChildren().add(border);

        // Tile fill on top of border
        int count = (int) Math.ceil(totalWidth / TILE_W) + 1;
        for (int i = 0; i < count; i++) {
            Rectangle tile = new Rectangle(i * TILE_W, isTop ? 1 : 0, TILE_W - 2, TILE_H - 1);
            tile.setFill(i % 2 == 0 ? STONE_A : STONE_B);
            pane.getChildren().add(tile);
        }
        return pane;
    }
}
