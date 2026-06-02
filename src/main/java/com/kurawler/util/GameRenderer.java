package com.kurawler.util;

import com.kurawler.engine.TileType;
import com.kurawler.engine.Vec2;
import com.kurawler.game.entity.Enemy;
import com.kurawler.game.entity.EnemyState;
import com.kurawler.game.entity.StatType;
import com.kurawler.game.objects.GameObject;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Central renderer for all in-game visuals.
 *
 * ALL image lookups use ImageCache.get("exact_filename.png") — no sprite-sheet
 * math, no SpriteRenderer tile coordinates. If a file is missing the tile
 * turns magenta (immediately obvious in testing).
 *
 * Wall tiles : wall_0.png … wall_3.png (48×48, cycle by position)
 * Floor tiles : floor_0.png … floor_2.png
 * Item images : potion_hp.png, potion_mana.png, potion_energy.png,
 * key_skull.png, key_gold.png, key_ornate.png,
 * ring_green.png, ring_blue.png,
 * coins.png
 */
public final class GameRenderer {

    // Wall filenames (4 variants for visual variety)
    private static final String[] WALL_FILES = { "wall_0.png", "wall_1.png", "wall_2.png", "wall_3.png" };
    // Floor filenames (3 variants)
    private static final String[] FLOOR_FILES = { "floor_0.png", "floor_1.png", "floor_2.png" };

    private GameRenderer() {
    }

    // ── Tile ─────────────────────────────────────────────────────────────────

    public static void drawTile(GraphicsContext gc, TileType type,
            int col, int row, double px, double py, double tile) {
        if (type == TileType.WALL) {
            // Use position-based variant so walls look natural, not repetitive
            int variant = ((col * 3 + row * 7) & 0x3);
            ImageCache.draw(gc, WALL_FILES[variant], px, py, tile, tile);
        } else {
            // Floor — alternate between 3 variants
            int variant = ((col + row) % 3 + 3) % 3;
            ImageCache.draw(gc, FLOOR_FILES[variant], px, py, tile, tile);
        }
    }

    // ── Game objects ─────────────────────────────────────────────────────────

    /**
     * Draw a game object at its canvas position.
     * Uses the renderTag() string to select the exact filename.
     */
    public static void drawObject(GraphicsContext gc, GameObject obj,
            double px, double py, double tile) {
        String tag = obj.renderTag();

        // Special draw cases
        if ("SEARCH_WALL".equals(tag)) {
            drawSearchableWall(gc, px, py, tile);
            return;
        }
        if ("WEAPON".equals(tag)) {
            ImageCache.draw(gc, weaponFile(obj), px + tile * 0.1, py + tile * 0.1, tile * 0.8, tile * 0.8);
            return;
        }
        if ("ARMOR".equals(tag)) {
            drawArmor(gc, px, py, tile);
            return;
        }
        if ("RELIC".equals(tag)) {
            drawRelic(gc, px, py, tile);
            return;
        }

        String file = fileForTag(tag, obj);
        if (file == null) {
            gc.setFill(Color.web("#555555"));
            gc.fillRoundRect(px + 4, py + 4, tile - 9, tile - 9, 4, 4);
            return;
        }
        if (obj.blocksMovement()) {
            ImageCache.draw(gc, file, px, py, tile, tile);
        } else {
            double off = 6;
            ImageCache.draw(gc, file, px + off, py + off, tile - off * 2, tile - off * 2);
        }
    }

    /** Draw the relic item as a glowing golden gem. */
    public static void drawRelic(GraphicsContext gc, double px, double py, double tile) {
        // Golden glow background
        gc.setFill(Color.web("#f1c40f", 0.25));
        gc.fillOval(px + 4, py + 4, tile - 8, tile - 8);
        // Gem sprite
        ImageCache.draw(gc, "ring_green.png", px + tile * 0.15, py + tile * 0.15, tile * 0.7, tile * 0.7);
        // Gold sparkle border
        gc.setStroke(Color.web("#f1c40f", 0.8));
        gc.setLineWidth(1.5);
        gc.strokeOval(px + 4, py + 4, tile - 8, tile - 8);
    }

    private static String fileForTag(String tag, GameObject obj) {
        return switch (tag) {
            // Structural
            case "WALL" -> "wall_1.png";
            case "CRATE" -> "wall_2.png";
            case "BREAK_CRATE" -> "wall_3.png";
            case "COLUMN" -> "wall_0.png";
            case "CHEST" -> "chest_brown.png";
            case "SEARCH_WALL" -> null; // rendered specially
            // New container types
            case "SEARCH_BOX" -> "box_closed.png";
            case "RELIC_CHEST" -> "chest_golden.png";
            case "RELIC" -> null; // rendered as glowing gem

            // Potions — pick by item name for exact match
            case "POTION" -> switch (obj.getName()) {
                case "Blue Potion" -> "potion_mana.png";
                case "Green Potion" -> "potion_energy.png";
                default -> "potion_hp.png";
            };

            // Items
            case "KEY" -> "key_gold.png";
            case "GEM" -> "ring_green.png";
            case "RING" -> "ring_blue.png";
            case "BOOK" -> null; // no dedicated image yet
            case "SHADOW_SCROLL" -> "scroll2.png";
            case "WEAPON" -> weaponFile(obj);
            case "ARMOR" -> null; // drawn by drawArmor()

            default -> null;
        };
    }

    // Searchable wall rendered separately because it needs a text overlay
    public static void drawSearchableWall(GraphicsContext gc, double px, double py, double tile) {
        gc.setFill(Color.web("#4a2222", 0.6));
        gc.fillRect(px, py, tile - 1, tile - 1);
        gc.setStroke(Color.web("#8a5050"));
        gc.setLineWidth(1.5);
        gc.strokeRect(px + 1, py + 1, tile - 3, tile - 3);
        gc.setFill(Color.web("#c9a227", 0.9));
        gc.setFont(javafx.scene.text.Font.font("Courier New",
                javafx.scene.text.FontWeight.BOLD, tile * 0.3));
        gc.fillText("?", px + tile * 0.38, py + tile * 0.65);
    }

    // Weapon file lookup by item name
    private static String weaponFile(GameObject obj) {
        return switch (obj.getName()) {
            case "Dagger" -> "weapon_dagger.png";
            case "Iron Sword" -> "weapon_sword.png";
            case "Battle Axe" -> "weapon_axe.png";
            case "Great Sword" -> "weapon_greatsword.png";
            case "Short Bow" -> "weapon_bow.png";
            case "Training Blade" -> "weapon_sword.png";
            case "Skirmish Dagger" -> "weapon_dagger.png";
            case "Ranger Bow" -> "weapon_bow.png";
            case "Knight Sword" -> "weapon_sword.png";
            case "War Axe" -> "weapon_axe.png";
            case "Champion Greatsword" -> "weapon_greatsword.png";
            default -> "weapon_sword.png";
        };
    }

    // Weapon image drawn inline via drawObject; this is a fallback shape
    public static void drawWeapon(GraphicsContext gc, double px, double py, double tile) {
        ImageCache.draw(gc, "weapon_sword.png", px + tile * 0.1, py + tile * 0.1, tile * 0.8, tile * 0.8);
    }

    // Armour icon
    public static void drawArmor(GraphicsContext gc, double px, double py, double tile) {
        gc.setFill(Color.web("#7fa8c0", 0.85));
        double cx = px + tile / 2, cy = py + tile / 2;
        double aw = tile * 0.5, ah = tile * 0.45;
        gc.fillOval(cx - aw / 2, cy - ah / 2, aw, ah);
    }

    // ── Enemy ─────────────────────────────────────────────────────────────────

    /**
     * Draw an enemy at (px,py).
     * Knights use KnightAnimator; Sorcerers use a character tile.
     */
    public static void drawEnemy(GraphicsContext gc, Enemy enemy,
            double px, double py, double tile,
            KnightAnimator knightAnim,
            SorcererAnimator sorcererAnim,
            boolean selected) {
        boolean chasing = enemy.getState() == EnemyState.CHASING;

        if (enemy.getType() == Enemy.Type.KNIGHT) {
            if (chasing) {
                gc.setFill(Color.web("#e74c3c", 0.20));
                gc.fillRect(px, py, tile, tile);
            }
            knightAnim.draw(gc, px, py, tile, tile);
        } else {
            // Sorcerer — animated frames
            if (chasing) {
                gc.setFill(Color.web("#9b59b6", 0.25));
                gc.fillRect(px, py, tile, tile);
            }
            sorcererAnim.draw(gc, px, py, tile, tile);
        }

        if (enemy.getTeam() == 1 || enemy.getTeam() == 2) {
            gc.setStroke(enemy.getTeam() == 1 ? Color.web("#e74c3c") : Color.web("#3498db"));
            gc.setLineWidth(2);
            gc.strokeRect(px + 2, py + 2, tile - 5, tile - 5);
        }

        // HP bar above enemy
        int hp = enemy.getStat(StatType.HP);
        int maxHp = enemy.getType() == Enemy.Type.KNIGHT ? 20 : 10;
        double bw = tile - 4;
        gc.setFill(Color.web("#3d2a2a"));
        gc.fillRect(px + 2, py - 6, bw, 4);
        gc.setFill(chasing ? Color.web("#e74c3c") : Color.web("#2ecc71"));
        gc.fillRect(px + 2, py - 6, bw * Math.min(1.0, hp / (double) maxHp), 4);

        // Selection highlight
        if (selected) {
            gc.setStroke(Color.web("#e74c3c"));
            gc.setLineWidth(2.5);
            gc.strokeRect(px + 1, py + 1, tile - 3, tile - 3);
        }
    }

    // ── Projectile ────────────────────────────────────────────────────────────

    public static void drawProjectile(GraphicsContext gc, double px, double py, double tile) {
        double cx = px + tile / 2.0 - 5, cy = py + tile / 2.0 - 5;
        gc.setFill(Color.web("#7ee8fa"));
        gc.fillOval(cx, cy, 10, 10);
        gc.setStroke(Color.web("#ffffff", 0.6));
        gc.setLineWidth(1);
        gc.strokeOval(cx, cy, 10, 10);
    }

    // ── Coin drop pop (text) ──────────────────────────────────────────────────

    /** Draw a coin icon for use in HUD / status bars. */
    public static void drawCoinIcon(GraphicsContext gc, double x, double y, double size) {
        ImageCache.draw(gc, "coins.png", x, y, size, size);
    }
}
