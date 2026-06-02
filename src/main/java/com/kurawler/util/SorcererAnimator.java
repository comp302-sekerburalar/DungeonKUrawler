package com.kurawler.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Sorcerer animation — 8 frames from sorcerer_1.png … sorcerer_8.png.
 * Mirrors KnightAnimator: loads what it finds, graceful fallback.
 */
public final class SorcererAnimator {

    private static final int MAX_FRAMES = 8;

    private final Image[] frames;
    private final int     frameCount;
    private int           current = 0;

    public SorcererAnimator() {
        Image[] tmp = new Image[MAX_FRAMES];
        int found = 0;
        for (int i = 1; i <= MAX_FRAMES; i++) {
            Image img = ImageCache.get("sorcerer_" + i + ".png");
            if (img != null) { tmp[found++] = img; }
        }
        if (found == 0) {
            Image fallback = ImageCache.get("sorcerer_1.png");
            if (fallback != null) { tmp[0] = fallback; found = 1; }
        }
        frames = new Image[found];
        System.arraycopy(tmp, 0, frames, 0, found);
        frameCount = found;
        System.out.println("[SorcererAnimator] frames=" + frameCount);
    }

    public void step() {
        if (frameCount > 0) current = (current + 1) % frameCount;
    }

    public void reset() { current = 0; }

    public void draw(GraphicsContext gc, double x, double y, double w, double h) {
        if (frameCount == 0) {
            SpriteRenderer.drawTile(gc, "characters_x2", 0, 0, x, y, w, h);
            return;
        }
        gc.drawImage(frames[current], x, y, w, h);
    }

    public void draw(GraphicsContext gc, double x, double y, double w, double h, Color tint) {
        if (tint != null) { gc.setFill(tint); gc.fillRect(x, y, w, h); }
        draw(gc, x, y, w, h);
    }

    public boolean isLoaded() { return frameCount > 0; }
}
