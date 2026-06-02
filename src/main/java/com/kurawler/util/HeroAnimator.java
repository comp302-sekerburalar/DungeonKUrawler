package com.kurawler.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Manages the hero's 8-frame walk animation.
 *
 * Frames are loaded from hero_1.png … hero_8.png (96×122 RGBA each).
 * Each frame is a full image with a transparent background.
 *
 * Usage:
 * // In your screen field:
 * private final HeroAnimator heroAnim = new HeroAnimator();
 *
 * // Call once per game tick or on each move:
 * heroAnim.step(); // advance one frame
 *
 * // In drawMap():
 * heroAnim.draw(gc, px, py, TILE, TILE, skinTint);
 */
public final class HeroAnimator {

    /** Number of animation frames. */
    public static final int FRAME_COUNT = 8;

    /** Width and height of each source frame in pixels. */
    public static final int FRAME_W = 96;
    public static final int FRAME_H = 122;

    private final Image[] frames = new Image[FRAME_COUNT];
    private int currentFrame = 0;
    private boolean loaded = false;

    public HeroAnimator() {
        loadFrames();
    }

    private void loadFrames() {
        boolean ok = true;
        for (int i = 0; i < FRAME_COUNT; i++) {
            String name = "hero_" + (i + 1) + ".png";
            Image img = SpriteRenderer.loadImage(name);
            frames[i] = img;
            if (img == null) {
                ok = false;
                System.err.println("[HeroAnimator] Missing: " + name);
            }
        }
        loaded = ok;
        System.out.println("[HeroAnimator] loaded=" + loaded);
    }

    /** Advance to the next animation frame (call on each move). */
    public void step() {
        currentFrame = (currentFrame + 1) % FRAME_COUNT;
    }

    /** Reset to the idle (first) frame. */
    public void reset() {
        currentFrame = 0;
    }

    /**
     * Draw the current hero frame onto the given GraphicsContext.
     *
     * @param gc       target canvas context
     * @param destX    pixel X in canvas space
     * @param destY    pixel Y in canvas space
     * @param destW    destination width (usually TILE)
     * @param destH    destination height (usually TILE)
     * @param skinTint optional Color overlay for skin (null = no tint)
     */
    public void draw(GraphicsContext gc, double destX, double destY,
            double destW, double destH, Color skinTint) {
        if (!loaded || frames[currentFrame] == null) {
            // Fallback: draw from characters_x2 sprite sheet
            SpriteRenderer.drawTile(gc, "characters_x2", 0, 2, destX, destY, destW, destH);
            return;
        }

        // Apply skin tint overlay before drawing sprite
        if (skinTint != null) {
            gc.setFill(skinTint);
            gc.fillRect(destX, destY, destW, destH);
        }

        gc.drawImage(frames[currentFrame], destX, destY, destW, destH);
    }

    /** Draw without skin tint. */
    public void draw(GraphicsContext gc, double destX, double destY, double destW, double destH) {
        draw(gc, destX, destY, destW, destH, null);
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Returns the current frame image (may be null if not loaded). */
    public Image getCurrentImage() {
        return loaded ? frames[currentFrame] : null;
    }
}
