package utils;

public class Grid {
	
	// 2D array 
    private Item[][] grid;
    private final int WIDTH = 10;
    private final int HEIGHT = 10;

    public Grid() {
        this.grid = new Item[WIDTH][HEIGHT];
    }

    public boolean isWalkable(int x, int y) {
        // Map Boundary Check
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return false;
        }

        Item objectAtTile = grid[x][y];

        // If the tile is empty, the hero can walk there
        if (objectAtTile == null) {
            return true;
        }

        // Detect object type by referencing the Item class field
        if (objectAtTile.getType().equalsIgnoreCase("STATIC")) {
            return false; 
        }

        return true;
    }
}
