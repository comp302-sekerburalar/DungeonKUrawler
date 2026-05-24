package com.kurawler.game.entity;

import com.kurawler.engine.Vec2;
import com.kurawler.game.objects.GameObject;
import com.kurawler.game.objects.GameObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryTest {

    @Test
    public void addItemToEmptyInventorySucceeds() {
        Inventory inventory = new Inventory();
        GameObject key = GameObjects.key(new Vec2(0, 0));

        boolean result = inventory.add(key);

        assertTrue(result);
        assertEquals(1, inventory.size());
        assertEquals(key, inventory.get(0));
    }

    @Test
    public void addItemToFullInventoryFails() {
        Inventory inventory = new Inventory();

        for (int i = 0; i < Inventory.CAPACITY; i++) {
            inventory.add(GameObjects.gem(new Vec2(i, 0)));
        }

        GameObject extraItem = GameObjects.key(new Vec2(9, 0));
        boolean result = inventory.add(extraItem);

        assertFalse(result);
        assertEquals(Inventory.CAPACITY, inventory.size());
    }

    @Test
    public void addMultipleItemsPreservesOrder() {
        Inventory inventory = new Inventory();

        GameObject key = GameObjects.key(new Vec2(0, 0));
        GameObject gem = GameObjects.gem(new Vec2(1, 0));
        GameObject potion = GameObjects.redPotion(new Vec2(2, 0));

        inventory.add(key);
        inventory.add(gem);
        inventory.add(potion);

        assertEquals(3, inventory.size());
        assertEquals(key, inventory.get(0));
        assertEquals(gem, inventory.get(1));
        assertEquals(potion, inventory.get(2));
    }
}
