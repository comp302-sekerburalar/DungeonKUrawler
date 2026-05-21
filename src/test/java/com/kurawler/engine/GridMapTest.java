package com.kurawler.engine;

import org.junit.jupiter.api.Test;
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
        // A robust ADT should fail its invariant validation checks 
        // if an operation breaks core bounds (like a size configuration <= 0)
        assertThrows(AssertionError.class, () -> {
            GridMap brokenMap = new GridMap(-5, 10);
            brokenMap.repOk();
        }, "repOk should throw an AssertionError if column sizes are negative");
    }

    
}