package com.example.wallsight.client.logic;

import java.util.Locale;

/** Strict {@code #RRGGBB} parsing: an optional leading '#', then exactly six ASCII hex digits. */
public final class HexColor {
    public static final int INVALID = -1;

    private HexColor() {}

    /** @return 0..0xFFFFFF, or {@link #INVALID} */
    public static int parse(String text) {
        if (text == null) {
            return INVALID;
        }
        String t = text.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        if (t.length() != 6) {
            return INVALID;
        }
        int value = 0;
        for (int i = 0; i < 6; i++) {
            char c = t.charAt(i);
            int digit = c < 128 ? Character.digit(c, 16) : -1; // ASCII only: no fullwidth/Arabic digits
            if (digit < 0) {
                return INVALID;
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    public static boolean isValid(String text) {
        return parse(text) != INVALID;
    }

    /** Canonical form, e.g. {@code #55FFFF}. */
    public static String format(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
