package com.kurawler.engine;

/**
 * The fundamental passability categories of a map cell.
 *
 * FLOOR   – empty; hero and enemies can walk here
 * WALL    – impassable static structure
 * STATIC  – impassable object (column, crate, chest …) that is NOT a wall tile
 * ITEM    – passable; hero can stand on the same tile
 */
public enum TileType {
    FLOOR,
    WALL,
    STATIC,
    ITEM
}
