package com.example.wallsight.client.combat;

import com.example.wallsight.client.config.WallSightConfig;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Unchanged from 1.0.2: behaviour and numbers are identical. */
public final class KnockbackGuard {
    private static final double PROJECTILE_RANGE_SQ = 64.0;
    private static final long EXCLUSION_WINDOW_TICKS = 10L;
    private static final List<EntityType<?>> EXCLUDED_PROJECTILES = List.of(
            EntityType.WIND_CHARGE, EntityType.BREEZE_WIND_CHARGE, EntityType.FIREBALL, EntityType.SMALL_FIREBALL);
    private static long tickCount;
    private static long lastExcludedTick = -11L;

    private KnockbackGuard() {}

    public static void tick(Minecraft minecraft) {
        ++tickCount;
        WallSightConfig config = WallSightConfig.get();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (!config.masterEnabled || !config.noKnockback || player == null || level == null) {
            return;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (!EXCLUDED_PROJECTILES.contains(entity.getType())
                    || !(entity.position().distanceToSqr(player.position()) <= PROJECTILE_RANGE_SQ)) {
                continue;
            }
            lastExcludedTick = tickCount;
            return;
        }
    }

    public static boolean shouldSuppress() {
        WallSightConfig config = WallSightConfig.get();
        return config.masterEnabled && config.noKnockback && tickCount - lastExcludedTick > EXCLUSION_WINDOW_TICKS;
    }

    public static ClientboundExplodePacket withoutPlayerKnockback(ClientboundExplodePacket packet) {
        if (packet.playerKnockback().isEmpty() || !shouldSuppress()) {
            return packet;
        }
        return new ClientboundExplodePacket(
                packet.center(),
                packet.radius(),
                packet.blockCount(),
                Optional.empty(),
                packet.explosionParticle(),
                packet.explosionSound(),
                packet.blockParticles());
    }
}
