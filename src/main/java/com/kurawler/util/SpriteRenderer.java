package com.kurawler.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads sprite sheets from resources and renders tiles onto a JavaFX Canvas.
 *
 * All sprite sheets use x2 versions (32×32 pixels per tile).
 *
 * Sheet layout (tile size = 32px):
 * characters_x2.png : 9 cols × 8 rows (32×32 each)
 * items_x2.png : 6 cols × 4 rows
 * weapons_x2.png : 10 cols × 10 rows
 * containers_x2.png : 4 cols × 6 rows
 * walls_and_statics_x2.png : 30 cols × 8 rows
 * Inventory_x4.png : inventory UI background (full image)
 */
public final class SpriteRenderer {

    public static final int TILE_SIZE = 32;

    private static final Map<String, Image> cache = new HashMap<>();

    private SpriteRenderer() {
    }

    /** Load (and cache) a sprite sheet by name (without extension). */
    public static Image getSheet(String name) {
        return cache.computeIfAbsent(name, n -> {
            String path = "/images/" + n + ".png";
            var stream = SpriteRenderer.class.getResourceAsStream(path);
            if (stream == null) {
                System.err.println("[SpriteRenderer] Missing: " + path);
                return null;
            }
            return new Image(stream);
        });
    }

    /**
     * Draw one tile from a sprite sheet at pixel position (destX, destY) on the
     * canvas.
     *
     * @param gc        target GraphicsContext
     * @param sheetName sprite sheet name (no extension)
     * @param srcCol    column index in the sheet (0-based)
     * @param srcRow    row index in the sheet (0-based)
     * @param destX     pixel x on canvas
     * @param destY     pixel y on canvas
     * @param destW     pixel width to draw (usually TILE_SIZE)
     * @param destH     pixel height to draw (usually TILE_SIZE)
     */
    public static void drawTile(GraphicsContext gc, String sheetName,
            int srcCol, int srcRow,
            double destX, double destY,
            double destW, double destH) {
        Image sheet = getSheet(sheetName);
        if (sheet == null)
            return;
        double sx = srcCol * TILE_SIZE;
        double sy = srcRow * TILE_SIZE;
        gc.drawImage(sheet, sx, sy, TILE_SIZE, TILE_SIZE,
                destX, destY, destW, destH);
    }

    /** Convenience: draw at TILE_SIZE × TILE_SIZE. */
    public static void drawTile(GraphicsContext gc, String sheetName,
            int srcCol, int srcRow,
            double destX, double destY) {
        drawTile(gc, sheetName, srcCol, srcRow, destX, destY, TILE_SIZE, TILE_SIZE);
    }

    /** Draw a full image (e.g. inventory background). */
    public static void drawImage(GraphicsContext gc, String name,
            double x, double y, double w, double h) {
        Image img = getSheet(name);
        if (img == null)
            return;
        gc.drawImage(img, x, y, w, h);
    }
}
