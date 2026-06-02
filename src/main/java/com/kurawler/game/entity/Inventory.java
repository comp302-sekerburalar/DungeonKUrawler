package com.kurawler.game.entity;

import com.kurawler.game.objects.GameObject;
import java.util.*;

public class Inventory {

    public static final int COLS = 2, ROWS = 4, CAPACITY = COLS * ROWS;

    private final List<GameObject> slots = new ArrayList<>(CAPACITY);

    public boolean isFull() {
        return slots.size() >= CAPACITY;
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public int size() {
        return slots.size();
    }

    public boolean add(GameObject item) {
        if (isFull())
            return false;
        slots.add(item);
        return true;
    }

    public boolean remove(GameObject item) {
        return slots.remove(item);
    }

    public GameObject get(int index) {
        return (index >= 0 && index < slots.size()) ? slots.get(index) : null;
    }

    public List<GameObject> all() {
        return Collections.unmodifiableList(slots);
    }

    public void clear() {
        slots.clear();
    }
}
