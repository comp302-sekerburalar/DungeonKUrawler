package com.kurawler.util;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead-simple image loader that never uses a sprite renderer.
 * Each image is loaded by exact filename from /images/ resources.
 * All images are cached after first load.
 */
public final class ImageCache {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private ImageCache() {}

    /**
     * Load an image by exact filename (e.g. "potion_hp.png").
     * Returns null and prints an error if the file is missing.
     */
    public static Image get(String filename) {
        return CACHE.computeIfAbsent(filename, name -> {
            // Try every possible classpath variant
            String[] paths = {
                "/images/" + name,
                "images/"  + name,
                "/"        + name,
                name
            };
            for (String p : paths) {
                try {
                    InputStream is = ImageCache.class.getResourceAsStream(p);
                    if (is != null) {
                        Image img = new Image(is);
                        if (!img.isError()) { System.out.println("[IMG] OK: " + p); return img; }
                    }
                } catch (Exception ignored) {}
                try {
                    URL url = ImageCache.class.getResource(p);
                    if (url != null) {
                        Image img = new Image(url.toExternalForm());
                        if (!img.isError()) { System.out.println("[IMG] OK(url): " + p); return img; }
                    }
                } catch (Exception ignored) {}
            }
            System.err.println("[IMG] MISSING: " + name);
            return null;
        });
    }

    /** Draw image onto a GraphicsContext, scaling to fit the dest rect. */
    public static void draw(javafx.scene.canvas.GraphicsContext gc, String filename,
                            double x, double y, double w, double h) {
        Image img = get(filename);
        if (img == null) {
            // Bright magenta so missing sprites are immediately obvious
            gc.setFill(javafx.scene.paint.Color.MAGENTA);
            gc.fillRect(x, y, w, h);
            return;
        }
        gc.drawImage(img, x, y, w, h);
    }

    /** Draw a sub-region (sx,sy,sw,sh) of an image into (dx,dy,dw,dh). */
    public static void drawRegion(javafx.scene.canvas.GraphicsContext gc, String filename,
                                  double sx, double sy, double sw, double sh,
                                  double dx, double dy, double dw, double dh) {
        Image img = get(filename);
        if (img == null) { gc.setFill(javafx.scene.paint.Color.MAGENTA); gc.fillRect(dx,dy,dw,dh); return; }
        gc.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);
    }
}
