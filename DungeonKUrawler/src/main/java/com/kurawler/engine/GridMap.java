package com.kurawler.engine;

import com.kurawler.game.objects.GameObject;

import java.util.*;

/**
 * Represents the dungeon floor as a 2-D grid.
 *
 * Each cell knows:
 *  - its base TileType (FLOOR / WALL)
 *  - the list of GameObjects placed on it
 *
 * Collision rules (spec §1.1):
 *  - WALL or STATIC object  → hero CANNOT enter
 *  - ITEM on a FLOOR tile   → hero CAN enter (items are passable)
 */
public class GridMap {

    private final int cols;
    private final int rows;
    private final TileType[][] baseTiles;                       // structural tile type
    private final Map<Vec2, List<GameObject>> objects;          // objects on each cell

    public GridMap(int cols, int rows) {
        this.cols      = cols;
        this.rows      = rows;
        this.baseTiles = new TileType[cols][rows];
        this.objects   = new HashMap<>();

        // Default everything to FLOOR
        for (int c = 0; c < cols; c++)
            for (int r = 0; r < rows; r++)
                baseTiles[c][r] = TileType.FLOOR;
    }

    // -------------------------------------------------------------------------
    // Tile accessors
    // -------------------------------------------------------------------------

    public void setTile(Vec2 pos, TileType type) { baseTiles[pos.col()][pos.row()] = type; }
    public TileType getTile(Vec2 pos)            { return baseTiles[pos.col()][pos.row()]; }

    public int getCols() { return cols; }
    public int getRows() { return rows; }

    // -------------------------------------------------------------------------
    // Object management
    // -------------------------------------------------------------------------

    public void placeObject(GameObject obj) {
        objects.computeIfAbsent(obj.getPos(), k -> new ArrayList<>()).add(obj);
    }

    public void removeObject(GameObject obj) {
        List<GameObject> list = objects.get(obj.getPos());
        if (list != null) {
            list.remove(obj);
            if (list.isEmpty()) objects.remove(obj.getPos());
        }
    }

    /** All objects currently on a given cell (may be empty). */
    public List<GameObject> objectsAt(Vec2 pos) {
        return objects.getOrDefault(pos, Collections.emptyList());
    }

    /** All objects on the map (unmodifiable view). */
    public Collection<List<GameObject>> allObjects() { return Collections.unmodifiableCollection(objects.values()); }

    // -------------------------------------------------------------------------
    // Collision query (spec §1.1)
    // -------------------------------------------------------------------------

    /**
     * Returns true when a moving entity (hero or enemy) is ALLOWED to enter pos.
     *
     * Blocked by:
     *  - Out-of-bounds
     *  - WALL base tile
     *  - Any STATIC-type GameObject on the cell
     */

    /**
     * Method spesification for isPassable:
     * Determines if a given position on the grid map is passable for a moving entity (hero or enemy).  
     * Requires:
     * -pos is not null
     * -Map tiles and objects are properly initialized
     * 
     * Modifies:
     * -Nothing 
     * 
     * Effects:
     * Returns false if the position is out-of-bounds, 
     * returns false if the base tile at the position is a WALL,
     * returns false if there is any STATIC-type GameObject on the cell,
     * returns true otherwise (the cell is passable).
     * 
     */
    public boolean isPassable(Vec2 pos) {
        if (!pos.inBounds(cols, rows)) return false;
        if (baseTiles[pos.col()][pos.row()] == TileType.WALL) return false;

        // Check for any blocking (STATIC) objects on this cell
        for (GameObject obj : objectsAt(pos)) {
            if (obj.blocksMovement()) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Neighbour helpers
    // -------------------------------------------------------------------------

    /** The 8 cells surrounding pos (including diagonals), filtered to in-bounds. */
    public List<Vec2> neighbours8(Vec2 pos) {
        List<Vec2> result = new ArrayList<>(8);
        for (int dc = -1; dc <= 1; dc++)
            for (int dr = -1; dr <= 1; dr++)
                if (dc != 0 || dr != 0) {
                    Vec2 n = pos.add(dc, dr);
                    if (n.inBounds(cols, rows)) result.add(n);
                }
        return result;
    }

    /** True if target is within the 3×3 area centred on origin (includes origin). */
    public boolean isAdjacent(Vec2 origin, Vec2 target) {
        return Math.abs(origin.col() - target.col()) <= 1 &&
               Math.abs(origin.row() - target.row()) <= 1;
    }

    // -------------------------------------------------------------------------
    // Map initialisation helpers
    // -------------------------------------------------------------------------

    /** Surround the entire grid with WALL tiles. */
    public void buildBorderWalls() {
        for (int c = 0; c < cols; c++) {
            baseTiles[c][0]        = TileType.WALL;
            baseTiles[c][rows - 1] = TileType.WALL;
        }
        for (int r = 0; r < rows; r++) {
            baseTiles[0][r]        = TileType.WALL;
            baseTiles[cols - 1][r] = TileType.WALL;
        }
    }

    /** Collect all floor cells on the edge (next to a wall) – used for enemy spawning. */
    public List<Vec2> edgeFloorCells() {
        List<Vec2> result = new ArrayList<>();
        for (int c = 1; c < cols - 1; c++) {
            for (int r = 1; r < rows - 1; r++) {
                if (baseTiles[c][r] != TileType.FLOOR) continue;
                // Check if any neighbour is a wall
                boolean adjacentToWall = false;
                for (Vec2 n : neighbours8(new Vec2(c, r))) {
                    if (baseTiles[n.col()][n.row()] == TileType.WALL) { adjacentToWall = true; break; }
                }
                if (adjacentToWall) result.add(new Vec2(c, r));
            }
        }
        return result;
    }
}
