package com.kurawler.engine;

import com.kurawler.game.objects.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Saves and loads GridMap to/from a JSON file (spec §4.2: JSON-based serialization).
 *
 * Format:
 * {
 *   "cols": 20, "rows": 15,
 *   "savedAt": "2026-05-29T14:30:00",
 *   "tiles": [ [0,0,0,...], [0,1,0,...], ... ],   // cols×rows  0=FLOOR 1=WALL
 *   "objects": [
 *     { "type":"CRATE",  "col":5, "row":3 },
 *     { "type":"POTION_RED", "col":7, "row":8 },
 *     ...
 *   ]
 * }
 */
public final class MapSerializer {

    private static final Path SAVE_DIR = Path.of(System.getProperty("user.home"), ".kurawler", "maps");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private MapSerializer() {}

    // ── Save ────────────────────────────────────────────────────────────────

    public static void save(GridMap map, String mapName) throws IOException {
        Files.createDirectories(SAVE_DIR);
        String filename = sanitise(mapName) + ".json";
        Path path = SAVE_DIR.resolve(filename);
        Files.writeString(path, toJson(map, mapName));
    }

    // ── Load ────────────────────────────────────────────────────────────────

    public static GridMap load(String mapName) throws IOException {
        Path path = SAVE_DIR.resolve(sanitise(mapName) + ".json");
        return fromJson(Files.readString(path));
    }

    /** Returns list of [displayName, savedAt] pairs for all saved maps. */
    public static List<String[]> listSavedMaps() {
        List<String[]> result = new ArrayList<>();
        if (!Files.exists(SAVE_DIR)) return result;
        try {
            Files.list(SAVE_DIR)
                 .filter(p -> p.toString().endsWith(".json"))
                 .sorted(Comparator.comparing(p -> {
                     try { return Files.getLastModifiedTime(p); }
                     catch (IOException e) { return null; }
                 }, Comparator.reverseOrder()))
                 .forEach(p -> {
                     try {
                         String json = Files.readString(p);
                         String name = extractString(json, "name");
                         String savedAt = extractString(json, "savedAt");
                         if (name == null) name = p.getFileName().toString().replace(".json","");
                         result.add(new String[]{ name, savedAt == null ? "" : savedAt });
                     } catch (IOException ignored) {}
                 });
        } catch (IOException ignored) {}
        return result;
    }

    public static boolean exists(String mapName) {
        return Files.exists(SAVE_DIR.resolve(sanitise(mapName) + ".json"));
    }

    // ── Serialization ────────────────────────────────────────────────────────

    private static String toJson(GridMap map, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(esc(name)).append("\",\n");
        sb.append("  \"cols\": ").append(map.getCols()).append(",\n");
        sb.append("  \"rows\": ").append(map.getRows()).append(",\n");
        sb.append("  \"savedAt\": \"").append(LocalDateTime.now().format(FMT)).append("\",\n");

        // tile grid (col-major, 0=FLOOR, 1=WALL)
        sb.append("  \"tiles\": [\n");
        for (int c = 0; c < map.getCols(); c++) {
            sb.append("    [");
            for (int r = 0; r < map.getRows(); r++) {
                sb.append(map.getTile(new Vec2(c,r)) == TileType.WALL ? 1 : 0);
                if (r < map.getRows()-1) sb.append(",");
            }
            sb.append("]");
            if (c < map.getCols()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // objects
        sb.append("  \"objects\": [\n");
        List<String> objLines = new ArrayList<>();
        for (var list : map.allObjects()) {
            for (var obj : list) {
                String type = renderTagToType(obj.renderTag(), obj);
                if (type != null) {
                    objLines.add("    {\"type\":\"" + type + "\",\"col\":" +
                                 obj.getPos().col() + ",\"row\":" + obj.getPos().row() + "}");
                }
            }
        }
        sb.append(String.join(",\n", objLines));
        if (!objLines.isEmpty()) sb.append("\n");
        sb.append("  ]\n}");
        return sb.toString();
    }

    private static GridMap fromJson(String json) {
        int cols    = parseInt(json, "cols",    20);
        int rows    = parseInt(json, "rows",    15);
        GridMap map = new GridMap(cols, rows);

        // Parse tile grid
        String tilesBlock = extractBlock(json, "tiles");
        if (tilesBlock != null) {
            // Split by row arrays: each [ ... ] is one column
            String[] colArrays = tilesBlock.split("\\[");
            int c = 0;
            for (String colArr : colArrays) {
                if (colArr.isBlank() || !colArr.contains("]")) continue;
                String nums = colArr.substring(0, colArr.indexOf("]")).trim();
                if (nums.isBlank()) continue;
                String[] vals = nums.split(",");
                for (int r = 0; r < vals.length && r < rows; r++) {
                    String v = vals[r].trim();
                    if (v.equals("1")) map.setTile(new Vec2(c, r), TileType.WALL);
                }
                c++;
                if (c >= cols) break;
            }
        }

        // Parse objects
        String objBlock = extractBlock(json, "objects");
        if (objBlock != null) {
            // Each object: {"type":"...","col":N,"row":N}
            String[] entries = objBlock.split("\\{");
            for (String entry : entries) {
                if (!entry.contains("}")) continue;
                String inner = entry.substring(0, entry.indexOf("}")).trim();
                String type = extractString("{" + inner + "}", "type");
                int col     = parseInt("{" + inner + "}", "col", -1);
                int row     = parseInt("{" + inner + "}", "row", -1);
                if (type == null || col < 0 || row < 0) continue;
                Vec2 pos = new Vec2(col, row);
                GameObject obj = typeToObject(type, pos);
                if (obj != null) map.placeObject(obj);
            }
        }
        return map;
    }

    // ── Type mapping ─────────────────────────────────────────────────────────

    private static String renderTagToType(String tag, GameObject obj) {
        return switch (tag) {
            case "WALL"       -> null;   // walls are in the tile grid
            case "CRATE"      -> "CRATE";
            case "BREAK_CRATE"-> "BREAK_CRATE";
            case "COLUMN"     -> "COLUMN";
            case "CHEST"      -> "CHEST";
            case "KEY"        -> "KEY";
            case "GEM"        -> "GEM";
            case "RING"       -> "RING";
            case "BOOK"       -> "BOOK";
            case "POTION"     -> potionType(obj);
            case "WEAPON"     -> weaponType(obj);
            case "ARMOR"      -> armorType(obj);
            case "SEARCH_WALL"-> "SEARCH_WALL";
            default           -> null;
        };
    }

    private static String potionType(GameObject obj) {
        return switch (obj.getName()) {
            case "Red Potion"   -> "POTION_RED";
            case "Blue Potion"  -> "POTION_BLUE";
            case "Green Potion" -> "POTION_GREEN";
            default             -> "POTION_RED";
        };
    }

    private static String weaponType(GameObject obj) {
        return switch (obj.getName()) {
            case "Iron Sword"  -> "WEAPON_SWORD";
            case "Dagger"      -> "WEAPON_DAGGER";
            case "Battle Axe"  -> "WEAPON_AXE";
            case "Short Bow"   -> "WEAPON_BOW";
            case "Great Sword" -> "WEAPON_GREATSWORD";
            default            -> "WEAPON_SWORD";
        };
    }

    private static String armorType(GameObject obj) {
        return switch (obj.getName()) {
            case "Leather Armor" -> "ARMOR_LEATHER";
            case "Chain Armor"   -> "ARMOR_CHAIN";
            default              -> "ARMOR_LEATHER";
        };
    }

    private static GameObject typeToObject(String type, Vec2 pos) {
        return switch (type) {
            case "CRATE"           -> GameObjects.crate(pos);
            case "BREAK_CRATE"     -> GameObjects.breakableCrate(pos);
            case "COLUMN"          -> GameObjects.column(pos);
            case "CHEST"           -> GameObjects.chest(pos);
            case "KEY"             -> GameObjects.key(pos);
            case "GEM"             -> GameObjects.gem(pos);
            case "RING"            -> GameObjects.ring(pos);
            case "BOOK"            -> GameObjects.spellBook(pos);
            case "POTION_RED"      -> GameObjects.redPotion(pos);
            case "POTION_BLUE"     -> GameObjects.bluePotion(pos);
            case "POTION_GREEN"    -> GameObjects.greenPotion(pos);
            case "WEAPON_SWORD"    -> GameObjects.sword(pos);
            case "WEAPON_DAGGER"   -> GameObjects.dagger(pos);
            case "WEAPON_AXE"      -> GameObjects.axe(pos);
            case "WEAPON_BOW"      -> GameObjects.bow(pos);
            case "WEAPON_GREATSWORD"-> GameObjects.greatSword(pos);
            case "ARMOR_LEATHER"   -> GameObjects.leatherArmor(pos);
            case "ARMOR_CHAIN"     -> GameObjects.chainArmor(pos);
            case "SEARCH_WALL"     -> GameObjects.searchableWall(pos, null);
            default                -> null;
        };
    }

    // ── Tiny JSON helpers (no external libs per spec §6.4) ──────────────────

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        int q1 = json.indexOf("\"", colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private static int parseInt(String json, String key, int def) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return def;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return def;
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '-') num.append(c);
            else if (!num.isEmpty()) break;
        }
        try { return num.isEmpty() ? def : Integer.parseInt(num.toString()); }
        catch (NumberFormatException e) { return def; }
    }

    /** Extract the JSON array/object block for the first occurrence of key. */
    private static String extractBlock(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        // Find opening bracket
        int open = -1;
        char openChar = '[', closeChar = ']';
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[' || c == '{') { open = i; openChar = c; closeChar = (c=='[') ? ']' : '}'; break; }
        }
        if (open < 0) return null;
        // Balance brackets
        int depth = 1;
        for (int i = open + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == openChar) depth++;
            else if (c == closeChar) { depth--; if (depth == 0) return json.substring(open + 1, i); }
        }
        return null;
    }

    private static String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static String esc(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}
