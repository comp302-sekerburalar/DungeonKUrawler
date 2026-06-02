package com.kurawler.engine;

import com.kurawler.game.entity.Enemy;

/**
 * A flying projectile launched by a Sorcerer.
 * Moves one cell per AI tick along a straight line.
 * Destroyed on impact with wall/static or hero.
 */
public class Projectile {

    private Vec2 pos;
    private final Vec2 direction;   // unit step (±1,0) or (0,±1) or diagonal
    private final Enemy source;
    private boolean active = true;

    public Projectile(Vec2 origin, Vec2 target, Enemy source) {
        this.pos    = origin;
        this.source = source;
        // Compute step direction
        int dc = Integer.signum(target.col() - origin.col());
        int dr = Integer.signum(target.row() - origin.row());
        this.direction = new Vec2(dc, dr);
    }

    /** Advance one cell. Returns false if now blocked or off-map. */
    public boolean advance(GridMap map) {
        Vec2 next = pos.add(direction);
        if (!next.inBounds(map.getCols(), map.getRows())) { active = false; return false; }
        if (!map.isPassable(next) && map.getTile(next) == com.kurawler.engine.TileType.WALL) {
            active = false; return false;
        }
        // Also block on static objects
        if (!map.objectsAt(next).isEmpty() &&
            map.objectsAt(next).stream().anyMatch(o -> o.blocksMovement())) {
            active = false; return false;
        }
        pos = next;
        return true;
    }

    public Vec2    getPos()    { return pos; }
    public boolean isActive()  { return active; }
    public void    destroy()   { active = false; }
    public Enemy   getSource() { return source; }
}
