package com.example.wallsight.client.logic;

/** The three things the ESP can colour. */
public enum ColorTarget {
    PLAYERS("Players", 0x55FFFF),
    HOSTILE("Hostile mobs", 0xFF5555),
    CONTAINERS("Containers", 0xFFAA00);

    public final String label;
    /** 0xRRGGBB; identical to the colours of the original 1.0.2 jar. */
    public final int defaultRgb;

    ColorTarget(String label, int defaultRgb) {
        this.label = label;
        this.defaultRgb = defaultRgb;
    }
}
