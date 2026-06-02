package com.kurawler.engine;

/** Immutable integer grid coordinate (col, row). */
public record Vec2(int col, int row) {

    public Vec2 add(int dc, int dr) {
        return new Vec2(col + dc, row + dr);
    }

    public Vec2 add(Vec2 o) {
        return new Vec2(col + o.col, row + o.row);
    }

    public double distanceTo(Vec2 o) {
        int dc = col - o.col, dr = row - o.row;
        return Math.sqrt(dc * dc + dr * dr);
    }

    public boolean inBounds(int cols, int rows) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    @Override
    public String toString() {
        return "(" + col + "," + row + ")";
    }
}
