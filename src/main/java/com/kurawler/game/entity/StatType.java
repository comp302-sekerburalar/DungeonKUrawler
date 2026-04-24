package com.kurawler.game.entity;

/** The five character stats tracked by the engine (spec §2.4.1). */
public enum StatType {
    HP     ("HP"),
    MANA   ("Mana"),
    STR    ("STR"),
    DEF    ("DEF"),
    ENERGY ("Energy");
    

    private final String display;
    StatType(String display) { this.display = display; }
    public String displayName() { return display; }
}
