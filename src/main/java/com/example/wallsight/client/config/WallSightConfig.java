package com.example.wallsight.client.config;

import com.example.wallsight.client.logic.ColorTarget;
import com.example.wallsight.client.logic.EspMath;
import com.example.wallsight.client.logic.HexColor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings, stored as JSON. The field names of 1.0.2 are unchanged, so an existing
 * {@code wallsight.json} still loads; the colour fields are new and default when missing.
 */
public final class WallSightConfig {
    public static final int MIN_RADIUS = EspMath.MIN_RADIUS;
    public static final int MAX_RADIUS = EspMath.MAX_RADIUS;

    private static final Logger LOGGER = LoggerFactory.getLogger("WallSight");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WallSightConfig instance = new WallSightConfig();

    public boolean masterEnabled = false;
    public boolean noKnockback = true;
    public boolean highlightPlayers = true;
    public boolean highlightHostile = true;
    public boolean highlightContainers = true;
    public int espRadius = 128;
    public String playerColor = HexColor.format(ColorTarget.PLAYERS.defaultRgb);
    public String hostileColor = HexColor.format(ColorTarget.HOSTILE.defaultRgb);
    public String containerColor = HexColor.format(ColorTarget.CONTAINERS.defaultRgb);

    /** Parsed copy of the three colour strings so the renderer never parses text per frame. */
    private transient int[] rgbCache;

    public static WallSightConfig get() {
        return instance;
    }

    /** Replaces the live config (used by tests). */
    public static void install(WallSightConfig config) {
        instance = config;
    }

    public int colorRgb(ColorTarget target) {
        if (rgbCache == null) {
            sanitize();
        }
        return rgbCache[target.ordinal()];
    }

    public int colorArgb(ColorTarget target) {
        return 0xFF000000 | colorRgb(target);
    }

    public void setColor(ColorTarget target, int rgb) {
        String hex = HexColor.format(rgb);
        switch (target) {
            case PLAYERS -> playerColor = hex;
            case HOSTILE -> hostileColor = hex;
            case CONTAINERS -> containerColor = hex;
        }
        sanitize();
    }

    /** Forces every value into its valid range and rebuilds the colour cache. */
    public void sanitize() {
        espRadius = EspMath.clampRadius(espRadius);
        int[] cache = new int[ColorTarget.values().length];
        for (ColorTarget t : ColorTarget.values()) {
            String text = switch (t) {
                case PLAYERS -> playerColor;
                case HOSTILE -> hostileColor;
                case CONTAINERS -> containerColor;
            };
            int rgb = HexColor.parse(text);
            cache[t.ordinal()] = rgb == HexColor.INVALID ? t.defaultRgb : rgb;
        }
        playerColor = HexColor.format(cache[ColorTarget.PLAYERS.ordinal()]);
        hostileColor = HexColor.format(cache[ColorTarget.HOSTILE.ordinal()]);
        containerColor = HexColor.format(cache[ColorTarget.CONTAINERS.ordinal()]);
        rgbCache = cache;
    }

    /** @throws JsonParseException if the text is not a usable config */
    public static WallSightConfig parse(String json) {
        WallSightConfig config = GSON.fromJson(json, WallSightConfig.class);
        if (config == null) {
            throw new JsonParseException("empty config");
        }
        config.sanitize();
        return config;
    }

    public String toJson() {
        sanitize();
        return GSON.toJson(this);
    }

    /** Loads {@code file} into the live config; a missing or broken file leaves the defaults. */
    public static void load(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            instance = parse(Files.readString(file));
        } catch (IOException | RuntimeException e) { // JsonParseException is a RuntimeException
            LOGGER.warn("Could not read {}, using defaults", file, e);
        }
    }

    /** Writes via a temp file and a rename, so a crash cannot leave a half-written config. */
    public void save(Path file) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, "wallsight", ".tmp");
            try {
                Files.writeString(tmp, toJson(), StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", file, e);
        }
    }
}
