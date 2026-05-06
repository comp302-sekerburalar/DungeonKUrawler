package com.kurawler.game.entity;

import com.kurawler.engine.GridMap;
import com.kurawler.engine.Vec2;

import java.util.List;
import java.util.Random;

/**
 * A dungeon enemy (Knight or Sorcerer).
 *
 * AI rules (spec §2.5.1):
 *   distance > DETECTION_RADIUS  → ROAMING  (random walk)
 *   distance ≤ DETECTION_RADIUS  → CHASING  (move toward hero)
 */
public class Enemy {

    public enum Type { KNIGHT, SORCERER }

    /** Euclidean distance threshold (spec: 5, rounded up). */
    public static final double DETECTION_RADIUS = 5.0;

    private static final Random RNG = new Random();

    private final String id;       // unique ID for rendering
    private final Type   type;
    private Vec2         pos;
    private CharacterStats stats;
    private EnemyState   state;

    public Enemy(String id, Type type, Vec2 spawnPos) {
        this.id    = id;
        this.type  = type;
        this.pos   = spawnPos;
        this.state = EnemyState.ROAMING;

        // spec §2.5.1 / §2.5.2 starting stats
        if (type == Type.KNIGHT) {
            stats = new CharacterStats(20, 0, 10, 1, 100);
        } else {
            stats = new CharacterStats(10, 60, 5, 0, 100);
        }
    }

    // ---------- AI tick ----------

    /**
     * Called once per game tick. Updates AI state and moves the enemy one cell.
     * Logs detection transitions to stdout (spec requirement).
     *
     * @return a log message describing what happened ("Roaming" or "Chasing …")
     */
    public String tick(Vec2 heroPos, GridMap map) {
        double dist = pos.distanceTo(heroPos);

        EnemyState newState = dist <= DETECTION_RADIUS ? EnemyState.CHASING : EnemyState.ROAMING;

        if (newState != state) {
            state = newState;
            String msg = "[" + type + " " + id + "] → " + state + " (dist=" + String.format("%.1f", dist) + ")";
            System.out.println(msg);
        }

        // Move
        Vec2 next = chooseNextCell(heroPos, map);
        if (next != null && map.isPassable(next)) {
            pos = next;
        }

        return type + " " + id + ": " + state + " dist=" + String.format("%.1f", dist);
    }

    private Vec2 chooseNextCell(Vec2 heroPos, GridMap map) {
        if (state == EnemyState.CHASING) {
            return stepToward(heroPos, map);
        } else {
            return randomStep(map);
        }
    }

    /** Move one cell orthogonally toward the hero (simple greedy). */
    private Vec2 stepToward(Vec2 target, GridMap map) {
        int bestScore = Integer.MAX_VALUE;
        Vec2 best     = null;
        int[] dCols   = {-1, 1, 0, 0};
        int[] dRows   = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            Vec2 candidate = pos.add(dCols[i], dRows[i]);
            if (!map.isPassable(candidate)) continue;
            // Manhattan distance as heuristic
            int score = Math.abs(candidate.col() - target.col()) +
                        Math.abs(candidate.row() - target.row());
            if (score < bestScore) { bestScore = score; best = candidate; }
        }
        return best;
    }

    /** Random orthogonal step to a passable neighbour. */
    private Vec2 randomStep(GridMap map) {
        int[] dCols = {-1, 1, 0, 0};
        int[] dRows = {0, 0, -1, 1};
        List<Vec2> options = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Vec2 c = pos.add(dCols[i], dRows[i]);
            if (map.isPassable(c)) options.add(c);
        }
        if (options.isEmpty()) return null;
        return options.get(RNG.nextInt(options.size()));
    }

    // ---------- Getters ----------

    public String     getId()    { return id; }
    public Type       getType()  { return type; }
    public Vec2       getPos()   { return pos; }
    public EnemyState getState() { return state; }
    public int        getStat(StatType t) { return stats.get(t); }
    public boolean    isAlive()  { return stats.get(StatType.HP) > 0; }

    @Override
    public String toString() { return type + "[" + id + "@" + pos + " " + state + "]"; }
}
