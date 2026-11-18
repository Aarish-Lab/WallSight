package com.example.wallsight.client;

import com.example.wallsight.client.ui.WallSightScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Loaded only when Mod Menu is installed; adds the "configure" button for WallSight. */
public final class WallSightModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new WallSightScreen(parent);
    }
}
