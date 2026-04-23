package utils;

import com.kurawler.model.Item;

public class Tile {

    private Item item; // what is on this tile

    public Tile() {
        this.item = null;
    }

    // Check if tile can be walked on
    public boolean isWalkable() {
        if (item == null) return true;

        return !item.isStatic();
    }
    

  
    public boolean isEmpty() {
        return item == null;
    }

   
    public Item getItem() {
        return item;
    }

   
    public void setItem(Item item) {
        this.item = item;
    }

    
    public void removeItem() {
        this.item = null;
    }
}