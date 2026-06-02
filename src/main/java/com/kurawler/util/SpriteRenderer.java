package com.kurawler.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads sprite sheets from resources and renders tiles onto a JavaFX Canvas.
 *
 * Tile size is 32px.  All x2 sheets:
 *   characters_x2.png   9×8
 *   items_x2.png        6×4
 *   weapons_x2.png      10×10
 *   containers_x2.png   4×6
 *   walls_and_statics_x2.png  30×8
 */
public final class SpriteRenderer {

    public static final int TILE_SIZE = 32;

    private static final Map<String, Image> cache = new HashMap<>();

    private SpriteRenderer() {}

    /**
     * Load (and cache) an image by filename (with extension).
     * Tries multiple resource paths so it works regardless of class-loader quirks.
     */
    public static Image loadImage(String filename) {
        return cache.computeIfAbsent(filename, name -> {
            // Try all common resource paths
            String[] paths = {
                "/images/" + name,
                "images/" + name,
                "/" + name,
                name
            };
            for (String p : paths) {
                try {
                    InputStream is = SpriteRenderer.class.getResourceAsStream(p);
                    if (is != null) {
                        Image img = new Image(is);
                        if (!img.isError()) {
                            System.out.println("[SpriteRenderer] Loaded: " + p);
                            return img;
                        }
                    }
                } catch (Exception ignored) {}
                try {
                    URL url = SpriteRenderer.class.getResource(p);
                    if (url != null) {
                        Image img = new Image(url.toExternalForm());
                        if (!img.isError()) {
                            System.out.println("[SpriteRenderer] Loaded via URL: " + p);
                            return img;
                        }
                    }
                } catch (Exception ignored) {}
            }
            System.err.println("[SpriteRenderer] FAILED to load: " + name);
            return null;
        });
    }

    /** Load sheet by name without extension (legacy API). */
    public static Image getSheet(String name) {
        return loadImage(name + ".png");
    }

    // ── Sheet tile rendering ──────────────────────────────────────────────────

    public static void drawTile(GraphicsContext gc, String sheetName,
                                int srcCol, int srcRow,
                                double destX, double destY,
                                double destW, double destH) {
        Image sheet = getSheet(sheetName);
        if (sheet == null) {
            // Draw a magenta placeholder so missing sprites are obvious
            gc.setFill(javafx.scene.paint.Color.MAGENTA);
            gc.fillRect(destX, destY, destW, destH);
            return;
        }
        double sx = srcCol * TILE_SIZE;
        double sy = srcRow * TILE_SIZE;
        gc.drawImage(sheet, sx, sy, TILE_SIZE, TILE_SIZE, destX, destY, destW, destH);
    }

    public static void drawTile(GraphicsContext gc, String sheetName,
                                int srcCol, int srcRow,
                                double destX, double destY) {
        drawTile(gc, sheetName, srcCol, srcRow, destX, destY, TILE_SIZE, TILE_SIZE);
    }

    // ── Full image rendering ──────────────────────────────────────────────────

    public static void drawImage(GraphicsContext gc, String name,
                                 double x, double y, double w, double h) {
        Image img = loadImage(name);
        if (img == null) return;
        gc.drawImage(img, x, y, w, h);
    }

    /**
     * Draw a named image (with extension) cropped from (sx,sy) at (sw×sh)
     * into dest rect (dx,dy,dw,dh).
     */
    public static void drawImageRegion(GraphicsContext gc, String name,
                                       double sx, double sy, double sw, double sh,
                                       double dx, double dy, double dw, double dh) {
        Image img = loadImage(name);
        if (img == null) return;
        gc.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);
    }
}
