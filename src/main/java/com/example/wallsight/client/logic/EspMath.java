package com.example.wallsight.client.logic;

/** The arithmetic of the ESP range check, kept free of Minecraft types so it can be tested. */
public final class EspMath {
    public static final int MIN_RADIUS = 8;
    public static final int MAX_RADIUS = 128;

    private EspMath() {}

    public static int clampRadius(int radius) {
        return Math.clamp((long) radius, MIN_RADIUS, MAX_RADIUS);
    }

    public static double radiusSq(int radius) {
        double r = clampRadius(radius);
        return r * r;
    }

    /** Inclusive range test on squared distance (same comparison as the original 1.0.2 jar). */
    public static boolean within(double dx, double dy, double dz, double radiusSq) {
        return dx * dx + dy * dy + dz * dz <= radiusSq;
    }

    /** How many chunks around the player must be scanned for containers. */
    public static int chunkRadius(int radius) {
        return radius + 15 >> 4;
    }
}
