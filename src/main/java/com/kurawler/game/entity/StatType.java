package com.kurawler.game.entity;

public enum StatType {
    HP("HP"), MANA("Mana"), STR("STR"), DEF("DEF"), ENERGY("Energy");

    private final String display;

    StatType(String d) {
        this.display = d;
    }

    public String displayName() {
        return display;
    }
}
