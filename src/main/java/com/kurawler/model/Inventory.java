package com.kurawler.model;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
    // 2x4 inventory (8 slots)
    private final int CAPACITY = 8;
    private List<Item> items;

    public Inventory() {
        this.items = new ArrayList<>();
    }

    public boolean canAddItem(Item item) {
        if (items.size() < CAPACITY) {
            items.add(item);
            return true;
        }
        return false; // Inventory full
    }

    public List<Item> getItems() {
        return items;
    }
}