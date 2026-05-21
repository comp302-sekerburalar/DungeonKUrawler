package com.kurawler.engine;

import com.kurawler.game.objects.GameObject;

import java.util.*;

// -------------------------------------------------------------------------
    // ADT Specifications
    // -------------------------------------------------------------------------
    // OVERVIEW: GridMap represents a mutable, 2-dimensional dungeon floor grid of 
    // size (cols x rows). It maintains structural terrain types (FLOOR/WALL) and 
    // tracks positions of various interactive GameObjects scattered across cells.
    //
    // ABSTRACTION FUNCTION (AF):
    // AF(cols, rows, baseTiles, objects) = A dungeon map with width `cols` and height `rows`.
    // For any valid coordinate position p (where 0 <= p.col < cols and 0 <= p.row < rows):
    //   - The terrain type at p is given by baseTiles[p.col][p.row]
    //   - The items/entities present at p are given by the list corresponding to objects.get(p) 
    //     (or an empty list if the coordinate key is missing).
    //
    // REPRESENTATION INVARIANT (RI):
    // 1. cols > 0 and rows > 0.
    // 2. baseTiles != null, and its dimensions must precisely match cols x rows.
    // 3. baseTiles continuous cells must not contain null elements (every element is FLOOR or WALL).
    // 4. objects != null.
    // 5. For every Vec2 key 'pos' in the objects map:
    //      - pos.inBounds(cols, rows) must be true.
    //      - objects.get(pos) != null and must not be empty.
    //      - Every GameObject obj in objects.get(pos) must satisfy obj.getPos().equals(pos).

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

    /**
     * Checks if the representation invariant holds true.
     * @throws AssertionError if the representation invariant is violated.
     */
    public void repOk() {
        assert cols > 0 : "RI Violated: cols must be positive";
        assert rows > 0 : "RI Violated: rows must be positive";
        assert baseTiles != null : "RI Violated: baseTiles array cannot be null";
        assert baseTiles.length == cols : "RI Violated: baseTiles width must match cols";
        assert baseTiles[0].length == rows : "RI Violated: baseTiles height must match rows";

        // Check array bounds and contents
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                assert baseTiles[c][r] != null : "RI Violated: Tile type cannot be null";
            }
        }

        assert objects != null : "RI Violated: objects map cannot be null";

        // Check internal map consistency
        for (Map.Entry<Vec2, List<GameObject>> entry : objects.entrySet()) {
            Vec2 pos = entry.getKey();
            List<GameObject> list = entry.getValue();

            assert pos.inBounds(cols, rows) : "RI Violated: Object key position is out of bounds";
            assert list != null : "RI Violated: Object list cannot be null";
            assert !list.isEmpty() : "RI Violated: Position key exists but points to an empty list";

            for (GameObject obj : list) {
                assert obj != null : "RI Violated: GameObject in list cannot be null";
                assert obj.getPos().equals(pos) : "RI Violated: GameObject position field mismatches map key coordinate";
            }
        }
    }

}
