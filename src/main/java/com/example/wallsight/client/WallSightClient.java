package com.example.wallsight.client;

import com.example.wallsight.client.combat.KnockbackGuard;
import com.example.wallsight.client.config.ConfigPaths;
import com.example.wallsight.client.config.WallSightConfig;
import com.example.wallsight.client.esp.EspRenderer;
import com.example.wallsight.client.input.WallSightKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class WallSightClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WallSightConfig.load(ConfigPaths.file());
        WallSightKeys.register();
        EspRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(KnockbackGuard::tick);
    }
}
