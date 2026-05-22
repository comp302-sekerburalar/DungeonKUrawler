package com.kurawler.engine;

import com.kurawler.game.objects.GameObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridMapTest {

    @Test
    void floorTileShouldBePassable() {
        GridMap map = new GridMap(5, 5);
        Vec2 position = new Vec2(2, 2);

        assertTrue(map.isPassable(position));
    }

    @Test
    void wallTileShouldNotBePassable() {
        GridMap map = new GridMap(5, 5);
        Vec2 position = new Vec2(2, 2);

        map.setTile(position, TileType.WALL);

        assertFalse(map.isPassable(position));
    }

    @Test
    void outOfBoundsPositionShouldNotBePassable() {
        GridMap map = new GridMap(5, 5);

        assertFalse(map.isPassable(new Vec2(-1, 0)));
        assertFalse(map.isPassable(new Vec2(0, -1)));
        assertFalse(map.isPassable(new Vec2(5, 0)));
        assertFalse(map.isPassable(new Vec2(0, 5)));
    }

    @Test
    void crateObjectShouldBlockMovement() {
        GridMap map = new GridMap(5, 5);
        Vec2 position = new Vec2(2, 2);

        map.placeObject(GameObjects.crate(position));

        assertFalse(map.isPassable(position));
    }

    @Test
    void keyItemShouldNotBlockMovement() {
        GridMap map = new GridMap(5, 5);
        Vec2 position = new Vec2(2, 2);

        map.placeObject(GameObjects.key(position));

        assertTrue(map.isPassable(position));
    }
}
