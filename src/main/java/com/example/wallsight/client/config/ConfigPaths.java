package com.example.wallsight.client.config;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** The only place that asks Fabric where the config directory is. */
public final class ConfigPaths {
    private ConfigPaths() {}

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("wallsight.json");
    }
}
