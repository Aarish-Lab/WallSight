package com.example.wallsight.client.input;

import com.example.wallsight.client.ui.WallSightScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Unchanged from 1.0.2 apart from the screen constructor: Right Shift opens the settings. */
public final class WallSightKeys {
    /** GLFW_KEY_RIGHT_SHIFT */
    private static final int DEFAULT_KEY = 344;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("wallsight", "general"));
    private static final KeyMapping OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.wallsight.open_settings", InputConstants.Type.KEYSYM, DEFAULT_KEY, CATEGORY));

    private WallSightKeys() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WallSightKeys::poll);
    }

    private static void poll(Minecraft minecraft) {
        if (OPEN_SETTINGS.consumeClick()) {
            minecraft.setScreen(new WallSightScreen());
        }
    }
}
