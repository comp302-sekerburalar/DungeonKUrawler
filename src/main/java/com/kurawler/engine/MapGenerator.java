package com.kurawler.engine;

import com.kurawler.game.objects.*;

import java.util.*;

/**
 * Generates the dungeon map for the standard game mode.
 *
 * Spec §4.1 guarantees:
 * - Border walls + interior obstacles
 * - 5 random items on the floor
 * - At least 1 searchable cracked wall
 * - At least 1 searchable wooden box
 * - 1 RelicChest (the guaranteed win container — holds the target relic)
 *
 * The target relic is hidden in one of these containers randomly:
 * a) a RelicChest (open or break to get relic)
 * b) a SearchableWall (search to reveal relic)
 * c) a SearchableBox (search to reveal relic)
 */
public final class MapGenerator {

    private static final Random RNG = new Random();

    private MapGenerator() {
    }

    // ── Standard random map (called by GameEngine with no existing map) ───────

    public static GridMap generate(int cols, int rows) {
        GridMap map = new GridMap(cols, rows);
        map.buildBorderWalls();
        addInteriorObstacles(map, cols, rows);
        placeRandomItems(map, 5);
        return map; // containers are added by GameEngine after relic is chosen
    }

    /**
     * Place all relic-hiding containers onto the map.
     * Called by GameEngine after it has chosen targetRelicName.
     *
     * @param map         the live map
     * @param relicItem   the actual relic GameObject to hide
     * @param hideInChest true=hide in RelicChest, false=hide in SearchableWall
     */
    public static void placeRelicContainers(GridMap map, GameObject relicItem, boolean hideInChest) {
        List<Vec2> floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);

        // Always place 1-3 searchable cracked walls (not holding the relic)
        int wallCount = 1 + RNG.nextInt(3);
        int placed = 0;
        for (Vec2 p : floors) {
            if (placed >= wallCount)
                break;
            if (isHeroSpawn(p) || !map.objectsAt(p).isEmpty())
                continue;
            map.placeObject(GameObjects.searchableWall(p, null)); // empty decoy
            placed++;
        }

        // Always place 1-2 searchable wooden boxes (not holding the relic)
        floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        int boxCount = 1 + RNG.nextInt(2);
        int boxPlaced = 0;
        for (Vec2 p : floors) {
            if (boxPlaced >= boxCount)
                break;
            if (isHeroSpawn(p) || !map.objectsAt(p).isEmpty())
                continue;
            map.placeObject(GameObjects.searchableBox(p, null)); // empty decoy
            boxPlaced++;
        }

        // Place the 1 breakable crate (does not hold relic)
        floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        for (Vec2 p : floors) {
            if (isHeroSpawn(p) || !map.objectsAt(p).isEmpty())
                continue;
            map.placeObject(GameObjects.breakableCrate(p));
            break;
        }

        // Place the relic container
        floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        for (Vec2 p : floors) {
            if (isHeroSpawn(p) || !map.objectsAt(p).isEmpty())
                continue;
            if (hideInChest) {
                map.placeObject(GameObjects.relicChest(p, relicItem));
            } else {
                // 50% in a new searchable wall, 50% in a new searchable box
                if (RNG.nextBoolean()) {
                    map.placeObject(GameObjects.searchableWall(p, relicItem));
                } else {
                    map.placeObject(GameObjects.searchableBox(p, relicItem));
                }
            }
            break;
        }
    }

    // ── Interior obstacles ────────────────────────────────────────────────────

    private static void addInteriorObstacles(GridMap map, int cols, int rows) {
        int numWalls = (cols * rows) / 40;
        List<Vec2> positions = new ArrayList<>();
        for (int c = 2; c < cols - 2; c++)
            for (int r = 2; r < rows - 2; r++)
                positions.add(new Vec2(c, r));
        Collections.shuffle(positions, RNG);

        int placed = 0;
        for (Vec2 pos : positions) {
            if (placed >= numWalls)
                break;
            if (isHeroSpawn(pos))
                continue;
            map.setTile(pos, TileType.WALL);
            placed++;
        }

        int objCount = (cols * rows) / 30;
        List<Vec2> floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        for (int i = 0; i < Math.min(objCount, floors.size()); i++) {
            Vec2 p = floors.get(i);
            if (isHeroSpawn(p))
                continue;
            if (RNG.nextBoolean())
                map.placeObject(GameObjects.crate(p));
            else
                map.placeObject(GameObjects.column(p));
        }
    }

    static void placeRandomItems(GridMap map, int count) {
        List<Vec2> floors = map.allFloorCells();
        Collections.shuffle(floors, RNG);
        int placed = 0;
        for (Vec2 p : floors) {
            if (placed >= count)
                break;
            if (isHeroSpawn(p) || !map.objectsAt(p).isEmpty())
                continue;
            int roll = RNG.nextInt(10);
            GameObject item;
            if (roll < 3)
                item = GameObjects.redPotion(p);
            else if (roll < 5)
                item = GameObjects.randomWeapon(p);
            else if (roll < 7)
                item = GameObjects.key(p);
            else if (roll < 8)
                item = GameObjects.bluePotion(p);
            else if (roll < 9)
                item = GameObjects.randomArmor(p);
            else
                item = GameObjects.gem(p);
            map.placeObject(item);
            placed++;
        }
    }

    private static boolean isHeroSpawn(Vec2 p) {
        return Math.abs(p.col() - 5) <= 2 && Math.abs(p.row() - 5) <= 2;
    }

    /** Public helper used by WaveEngine. */
    public static void populateItems(GridMap map, int count) {
        placeRandomItems(map, count);
    }
}
