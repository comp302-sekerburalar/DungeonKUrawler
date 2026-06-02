package com.kurawler.engine;

import org.junit.jupiter.api.Test;

import com.kurawler.game.objects.GameObject;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class GridMapTest {

    @Test
    public void testInitializationState() {
        GridMap map = new GridMap(10, 10);
        assertEquals(10, map.getCols());
        assertEquals(10, map.getRows());
        assertEquals(TileType.FLOOR, map.getTile(new Vec2(5, 5)));
        assertTrue(map.allObjects().isEmpty());
        
        // Verifies the initial safe state passes representation invariant rules
        assertDoesNotThrow(() -> map.repOk());
    }

    @Test
    public void testBuildBorderWallsEnforcement() {
        GridMap map = new GridMap(5, 5);
        map.buildBorderWalls();
        
        assertEquals(TileType.WALL, map.getTile(new Vec2(0, 0)));
        assertEquals(TileType.WALL, map.getTile(new Vec2(4, 4)));
        assertEquals(TileType.FLOOR, map.getTile(new Vec2(2, 2))); 
        
        // Verifies the map is structurally sound after mutator modifications
        assertDoesNotThrow(() -> map.repOk());
    }

    @Test
    public void testAdjacencyCalculations() {
        GridMap map = new GridMap(5, 5);
        Vec2 origin = new Vec2(2, 2);
        
        assertTrue(map.isAdjacent(origin, new Vec2(1, 1))); 
        assertTrue(map.isAdjacent(origin, new Vec2(2, 3))); 
        assertFalse(map.isAdjacent(origin, new Vec2(4, 2))); 
    }

    @Test
    public void testNeighbours8Boundaries() {
        GridMap map = new GridMap(5, 5);
        List<Vec2> centerNeighbours = map.neighbours8(new Vec2(2, 2));
        assertEquals(8, centerNeighbours.size());
        
        List<Vec2> cornerNeighbours = map.neighbours8(new Vec2(0, 0));
        assertEquals(3, cornerNeighbours.size()); 
    }

    @Test
    public void testRepOkCatchesInvalidDimensions() {
        // Since Java throws a NegativeArraySizeException during instantiation 
        // when dimensions are negative, we catch that to prove bad sizes are blocked!
        assertThrows(NegativeArraySizeException.class, () -> {
            new GridMap(-5, 10);
        }, "The constructor should throw an exception if column sizes are negative");
    }


    /** 
 * Tests for IsPassable method and its 4 test cases:
 * 1. Out-of-bounds positions should return false.
 * 2. Tiles that are walls should return false.
 * 3. Tiles that are blocked by objects should return false.
 * 4. Tiles that are floor and have no blocking objects should return true.
 * 
 */



/**
 * Test that out-of-bounds positions are not passable.
 */
@Test
public void testIsPassableOutOfBounds() {
    GridMap map = new GridMap(5, 5);

    assertFalse(map.isPassable(new Vec2(-1, 2)));
    assertFalse(map.isPassable(new Vec2(10, 10)));
}

/**
 * Test that wall tiles are not passable.
 */
@Test
public void testIsPassableWallTile() {
    GridMap map = new GridMap(5, 5);

    Vec2 pos = new Vec2(2, 2);
    map.setTile(pos, TileType.WALL);

    assertFalse(map.isPassable(pos));
}

/**
 * Test that tiles occupied by blocking objects are not passable.
 */
@Test
public void testIsPassableBlockedByObject() {
    GridMap map = new GridMap(5, 5);

    Vec2 pos = new Vec2(2, 2);

    // Create a fake blocking object for testing purposes
    GameObject rock = new GameObject("rock", pos, true) {
        @Override
        public String renderTag() {
            return "ROCK";
        }
    };

    map.placeObject(rock);

    assertFalse(map.isPassable(pos));
}


/**
 * Test that empty floor tiles are passable.
 */
@Test
public void testIsPassableReturnsTrueOnEmptyFloor() {
    GridMap map = new GridMap(5, 5);

    Vec2 pos = new Vec2(2, 2);

    assertTrue(map.isPassable(pos));
}


    


}

