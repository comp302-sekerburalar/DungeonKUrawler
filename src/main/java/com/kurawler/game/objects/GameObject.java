package com.kurawler.game.objects;

import com.kurawler.engine.Vec2;
import com.kurawler.game.action.Action;

import java.util.*;

public abstract class GameObject {

    private final String name;
    private Vec2 pos;
    private final boolean blocksMovement;
    private final List<Action> actions = new ArrayList<>();

    // Sprite sheet coordinates (tile col/row in the sheet, -1 = draw
    // programmatically)
    private final int spriteSheetCol;
    private final int spriteSheetRow;
    private final String spriteSheet; // e.g. "items_x2", "weapons_x2"

    protected GameObject(String name, Vec2 pos, boolean blocksMovement,
            String spriteSheet, int ssCol, int ssRow) {
        this.name = name;
        this.pos = pos;
        this.blocksMovement = blocksMovement;
        this.spriteSheet = spriteSheet;
        this.spriteSheetCol = ssCol;
        this.spriteSheetRow = ssRow;
    }

    // ── identity ──
    public String getName() {
        return name;
    }

    public boolean blocksMovement() {
        return blocksMovement;
    }

    // ── sprite ──
    public String getSpriteSheet() {
        return spriteSheet;
    }

    public int getSpriteCol() {
        return spriteSheetCol;
    }

    public int getSpriteRow() {
        return spriteSheetRow;
    }

    public abstract String renderTag();

    // ── position ──
    public Vec2 getPos() {
        return pos;
    }

    public void setPos(Vec2 pos) {
        this.pos = pos;
    }

    // ── actions ──
    public void addAction(Action a) {
        actions.add(a);
    }

    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public boolean hasActions() {
        return !actions.isEmpty();
    }

    public void clearActions() {
        actions.clear();
    }

    @Override
    public String toString() {
        return name + "@" + pos;
    }
}
