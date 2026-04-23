package utils;

import java.util.List;

import com.kurawler.model.Item;




public class Grid{
	
	
	protected Tile[][] grid;
    protected final int WIDTH = 100;
    protected final int HEIGHT = 100;
  
    public Grid() {
        grid = new Tile[WIDTH][HEIGHT];

       
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = new Tile();
            }
        }
    }
    
    public boolean isInside(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }
    
    public boolean isWalkable(int x, int y) {
        if (!isInside(x, y)) return false;

        return grid[x][y].isWalkable();
    }
    
    public Tile getTile(int x, int y) {
        if (!isInside(x, y)) return null;
        return grid[x][y];
    }
    
    public void placeItem(int x, int y, Item item) {
        if (isInside(x, y)) {
            grid[x][y].setItem(item);
        }
    }
    public void removeItem(int x, int y) {
        if (isInside(x, y)) {
            grid[x][y].removeItem();
        }
    }
    
}
