package com.kurawler.engine;

/**
 * Immutable integer grid coordinate (col, row).
 */
public record Vec2(int col, int row) {

    public Vec2 add(int dc, int dr) { return new Vec2(col + dc, row + dr); }
    public Vec2 add(Vec2 other)     { return new Vec2(col + other.col, row + other.row); }

    /** Euclidean distance to another cell (used for enemy detection radius). */
    public double distanceTo(Vec2 other) {
        int dc = col - other.col;
        int dr = row - other.row;
        return Math.sqrt(dc * dc + dr * dr);
    }

    /** True when this coordinate falls within a grid of given dimensions. */
    public boolean inBounds(int cols, int rows) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    @Override
    public String toString() { return "(" + col + "," + row + ")"; }
}
