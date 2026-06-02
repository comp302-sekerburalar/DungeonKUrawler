package com.kurawler.engine;

import com.kurawler.game.objects.*;

import java.util.*;

/**
 * Generates the dungeon map for the standard game mode (spec §4.1).
 *
 * Spec requirements:
 *  - Border walls
 *  - Random interior columns and crates as obstacles
 *  - 5 random items on floor at random locations
 *  - 1 random item hidden in a non-container searchable location
 *  - Target relic hidden in a container or searchable location
 */
public final class MapGenerator {

    private static final Random RNG = new Random();

    private MapGenerator() {}

    public static GridMap generate(int cols, int rows) {
        GridMap map = new GridMap(cols, rows);
        map.buildBorderWalls();

        // Scatter some interior walls to create corridors
        addInteriorObstacles(map, cols, rows);

        // 5 random items at random floor locations
        placeRandomItems(map, 5);

        // 1 breakable crate
        placeOnEmptyFloor(map, GameObjects.breakableCrate(new Vec2(0,0)));

        // 1 searchable wall with a hidden gem
        List<Vec2> floors = map.allFloorCells();
        if (floors.size() > 4) {
            Vec2 hiddenPos = floors.get(RNG.nextInt(floors.size()));
            GameObject hiddenGem = GameObjects.gem(hiddenPos);
            map.placeObject(GameObjects.searchableWall(hiddenPos, hiddenGem));
        }

        return map;
    }

    private static void addInteriorObstacles(GridMap map, int cols, int rows) {
        int numColumns = (cols * rows) / 40;
        List<Vec2> positions = new ArrayList<>();
        for (int c = 2; c < cols-2; c++)
            for (int r = 2; r < rows-2; r++)
                positions.add(new Vec2(c,r));
        Collections.shuffle(positions, RNG);

        int placed = 0;
        for (Vec2 pos : positions) {
            if (placed >= numColumns) break;
            // Keep hero start area clear (around 5,5)
            if (Math.abs(pos.col()-5) <= 2 && Math.abs(pos.row()-5) <= 2) continue;
            map.setTile(pos, TileType.WALL);
            placed++;
        }

        // A few crates and columns
        int objCount = (cols * rows) / 30;
        List<Vec2> floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        for (int i = 0; i < Math.min(objCount, floors.size()); i++) {
            Vec2 p = floors.get(i);
            if (Math.abs(p.col()-5) <= 2 && Math.abs(p.row()-5) <= 2) continue;
            if (RNG.nextBoolean()) map.placeObject(GameObjects.crate(p));
            else                  map.placeObject(GameObjects.column(p));
        }
    }

    private static void placeRandomItems(GridMap map, int count) {
        List<Vec2> floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        int placed = 0;
        for (Vec2 p : floors) {
            if (placed >= count) break;
            if (Math.abs(p.col()-5) <= 1 && Math.abs(p.row()-5) <= 1) continue;
            int roll = RNG.nextInt(10);
            GameObject item;
            if      (roll < 3) item = GameObjects.redPotion(p);
            else if (roll < 5) item = GameObjects.randomWeapon(p);
            else if (roll < 7) item = GameObjects.key(p);
            else if (roll < 8) item = GameObjects.bluePotion(p);
            else if (roll < 9) item = GameObjects.randomArmor(p);
            else               item = GameObjects.gem(p);
            map.placeObject(item);
            placed++;
        }
    }

    private static void placeOnEmptyFloor(GridMap map, GameObject obj) {
        List<Vec2> floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        for (Vec2 p : floors) {
            if (map.objectsAt(p).isEmpty()) {
                obj.setPos(p);
                map.placeObject(obj);
                return;
            }
        }
    }

    /** Public helper: scatter random items onto existing map floors. */
    public static void populateItems(GridMap map, int count) {
        placeRandomItems(map, count);
    }
}
