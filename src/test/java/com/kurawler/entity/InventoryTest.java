package com.kurawler.game.entity;

import com.kurawler.engine.Vec2;
import com.kurawler.game.objects.GameObject;
import com.kurawler.game.objects.GameObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryTest {

    @Test
    public void addItemToEmptyInventorySucceeds() {
        /*
         * This test checks the normal behavior of the add method.
         * The inventory starts empty, so adding one item should succeed.
         * The method should return true, the inventory size should become 1,
         * and the added item should be stored at index 0.
         */
        Inventory inventory = new Inventory();
        GameObject key = GameObjects.key(new Vec2(0, 0));

        boolean result = inventory.add(key);

        assertTrue(result);
        assertEquals(1, inventory.size());
        assertEquals(key, inventory.get(0));
    }

    @Test
    public void addItemToFullInventoryFails() {
        /*
         * This test checks the capacity boundary of the add method.
         * First, the inventory is filled up to its maximum capacity.
         * Then, the test tries to add one extra item.
         * Since the inventory is already full, add should return false
         * and the inventory size should stay equal to CAPACITY.
         */
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
        /*
         * This test checks that the inventory keeps items in the same order
         * in which they were added.
         * The key is added first, the gem second, and the potion third.
         * Therefore, they should be stored at indexes 0, 1, and 2 in that order.
         */
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
