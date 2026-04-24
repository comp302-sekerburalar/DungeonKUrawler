package com.kurawler.game.entity;

import com.kurawler.game.objects.GameObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The hero's inventory: 2 columns × 4 rows = 8 slots (spec §2.4.2).
 * Items are not stackable; each occupies exactly one slot.
 */
public class Inventory {

    public static final int COLS     = 2;
    public static final int ROWS     = 4;
    public static final int CAPACITY = COLS * ROWS;   // 8

    /** Ordered list – index 0..7 maps to the visual grid left-to-right, top-to-bottom. */
    private final List<GameObject> slots = new ArrayList<>(CAPACITY);

    public boolean isFull()  { return slots.size() >= CAPACITY; }
    public boolean isEmpty() { return slots.isEmpty(); }
    public int     size()    { return slots.size(); }

    /** Add an item; returns false if inventory is full. */
    public boolean add(GameObject item) {
        if (isFull()) return false;
        slots.add(item);
        return true;
    }

    /** Remove an item by reference. */
    public boolean remove(GameObject item) { return slots.remove(item); }

    /** Item at visual slot index (0-based, row-major). Null if empty. */
    public GameObject get(int index) {
        if (index < 0 || index >= slots.size()) return null;
        return slots.get(index);
    }

    public List<GameObject> all() { return Collections.unmodifiableList(slots); }
}
