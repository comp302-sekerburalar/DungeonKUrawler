package com.kurawler.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Knight walk animation.
 *
 * Expects knight_1.png … knight_N.png in /images/.
 * If only some frames exist the animator uses what it finds.
 * Falls back gracefully to characters_x2 sprite sheet if nothing loads.
 */
public final class KnightAnimator {

    private static final int MAX_FRAMES = 8;

    private final Image[] frames;
    private final int frameCount;
    private int current = 0;

    public KnightAnimator() {
        Image[] tmp = new Image[MAX_FRAMES];
        int found = 0;
        for (int i = 1; i <= MAX_FRAMES; i++) {
            Image img = ImageCache.get("knight_" + i + ".png");
            if (img != null) {
                tmp[found++] = img;
            }
        }
        // If nothing found, fill with any knight frame we have (e.g. knight_7)
        if (found == 0) {
            Image fallback = ImageCache.get("knight_7.png");
            if (fallback != null) {
                tmp[0] = fallback;
                found = 1;
            }
        }
        frames = new Image[found];
        System.arraycopy(tmp, 0, frames, 0, found);
        frameCount = found;
        System.out.println("[KnightAnimator] frames=" + frameCount);
    }

    public void step() {
        if (frameCount > 0)
            current = (current + 1) % frameCount;
    }

    public void reset() {
        current = 0;
    }

    public void draw(GraphicsContext gc, double x, double y, double w, double h) {
        if (frameCount == 0) {
            // Fallback to sprite sheet tile
            SpriteRenderer.drawTile(gc, "characters_x2", 0, 1, x, y, w, h);
            return;
        }
        gc.drawImage(frames[current], x, y, w, h);
    }

    /** Draw with a colour tint overlay (for enemy state feedback). */
    public void draw(GraphicsContext gc, double x, double y, double w, double h, Color tint) {
        if (tint != null) {
            gc.setFill(tint);
            gc.fillRect(x, y, w, h);
        }
        draw(gc, x, y, w, h);
    }

    public boolean isLoaded() {
        return frameCount > 0;
    }
}
