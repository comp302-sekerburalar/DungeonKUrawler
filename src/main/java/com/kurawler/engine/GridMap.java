package com.kurawler.engine;

import com.kurawler.game.objects.GameObject;

import java.util.*;

public class GridMap {

    private final int cols, rows;
    private final TileType[][] baseTiles;
    private final Map<Vec2, List<GameObject>> objects = new HashMap<>();

    public GridMap(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        baseTiles = new TileType[cols][rows];
        for (int c = 0; c < cols; c++)
            for (int r = 0; r < rows; r++)
                baseTiles[c][r] = TileType.FLOOR;
    }

    // ── tile ──
    public void setTile(Vec2 p, TileType t) {
        baseTiles[p.col()][p.row()] = t;
    }

    public TileType getTile(Vec2 p) {
        return baseTiles[p.col()][p.row()];
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    // ── objects ──
    public void placeObject(GameObject obj) {
        objects.computeIfAbsent(obj.getPos(), k -> new ArrayList<>()).add(obj);
    }

    public void removeObject(GameObject obj) {
        List<GameObject> list = objects.get(obj.getPos());
        if (list != null) {
            list.remove(obj);
            if (list.isEmpty())
                objects.remove(obj.getPos());
        }
    }

    public void moveObject(GameObject obj, Vec2 newPos) {
        removeObject(obj);
        obj.setPos(newPos);
        placeObject(obj);
    }

    public List<GameObject> objectsAt(Vec2 pos) {
        return objects.getOrDefault(pos, Collections.emptyList());
    }

    public Collection<List<GameObject>> allObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }

    public void clearAll() {
        objects.clear();
    }

    // ── collision ──
    public boolean isPassable(Vec2 pos) {
        if (!pos.inBounds(cols, rows))
            return false;
        if (baseTiles[pos.col()][pos.row()] == TileType.WALL)
            return false;
        for (GameObject obj : objectsAt(pos))
            if (obj.blocksMovement())
                return false;
        return true;
    }

    // ── adjacency ──
    public boolean isAdjacent(Vec2 origin, Vec2 target) {
        return Math.abs(origin.col() - target.col()) <= 1 &&
                Math.abs(origin.row() - target.row()) <= 1;
    }

    public List<Vec2> neighbours4(Vec2 pos) {
        int[] dc = { -1, 1, 0, 0 }, dr = { 0, 0, -1, 1 };
        List<Vec2> r = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            Vec2 n = pos.add(dc[i], dr[i]);
            if (n.inBounds(cols, rows))
                r.add(n);
        }
        return r;
    }

    // ── utilities ──
    public void buildBorderWalls() {
        for (int c = 0; c < cols; c++) {
            baseTiles[c][0] = TileType.WALL;
            baseTiles[c][rows - 1] = TileType.WALL;
        }
        for (int r = 0; r < rows; r++) {
            baseTiles[0][r] = TileType.WALL;
            baseTiles[cols - 1][r] = TileType.WALL;
        }
    }

    /**
     * Floor cells along the inner edge (next to a wall) — used for enemy spawning.
     */
    public List<Vec2> edgeFloorCells() {
        List<Vec2> result = new ArrayList<>();
        for (int c = 1; c < cols - 1; c++) {
            for (int r = 1; r < rows - 1; r++) {
                if (baseTiles[c][r] != TileType.FLOOR)
                    continue;
                boolean adj = false;
                for (Vec2 n : neighbours4(new Vec2(c, r)))
                    if (baseTiles[n.col()][n.row()] == TileType.WALL) {
                        adj = true;
                        break;
                    }
                if (adj)
                    result.add(new Vec2(c, r));
            }
        }
        return result;
    }

    /** All passable floor cells (used for random placement). */
    public List<Vec2> allFloorCells() {
        List<Vec2> result = new ArrayList<>();
        for (int c = 1; c < cols - 1; c++)
            for (int r = 1; r < rows - 1; r++)
                if (isPassable(new Vec2(c, r)))
                    result.add(new Vec2(c, r));
        return result;
    }
}
