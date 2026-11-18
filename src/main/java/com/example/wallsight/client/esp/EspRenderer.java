package com.example.wallsight.client.esp;

import com.example.wallsight.client.config.WallSightConfig;
import com.example.wallsight.client.logic.ColorTarget;
import com.example.wallsight.client.logic.EspMath;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Same detection and drawing as 1.0.2; the only change is that colours come from the config instead
 * of constants. Entity classification: any {@link Player} other than yourself, any {@link Enemy}
 * (hostile mob), and chest / ender chest block entities, all within the configured radius.
 */
public final class EspRenderer {
    private static final float LINE_WIDTH = 2.0f;

    private EspRenderer() {}

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> draw());
    }

    private static void draw() {
        WallSightConfig config = WallSightConfig.get();
        if (!config.masterEnabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        Vec3 origin = player.position();
        double radiusSq = EspMath.radiusSq(config.espRadius);
        if (config.highlightPlayers || config.highlightHostile) {
            highlightEntities(level, player, origin, radiusSq, config);
        }
        if (config.highlightContainers) {
            highlightContainers(level, player, origin, radiusSq, config);
        }
    }

    private static void highlightEntities(
            ClientLevel level, LocalPlayer self, Vec3 origin, double radiusSq, WallSightConfig config) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == self
                    || !EspMath.within(
                            entity.getX() - origin.x(), entity.getY() - origin.y(), entity.getZ() - origin.z(), radiusSq)) {
                continue;
            }
            int color;
            if (config.highlightPlayers && entity instanceof Player) {
                color = config.colorArgb(ColorTarget.PLAYERS);
            } else if (config.highlightHostile && entity instanceof Enemy) {
                color = config.colorArgb(ColorTarget.HOSTILE);
            } else {
                continue;
            }
            Gizmos.cuboid(entity.getBoundingBox(), GizmoStyle.stroke(color, LINE_WIDTH)).setAlwaysOnTop();
        }
    }

    private static void highlightContainers(
            ClientLevel level, LocalPlayer player, Vec3 origin, double radiusSq, WallSightConfig config) {
        int range = EspMath.chunkRadius(config.espRadius);
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;
        int color = config.colorArgb(ColorTarget.CONTAINERS);
        for (int cx = playerChunkX - range; cx <= playerChunkX + range; ++cx) {
            for (int cz = playerChunkZ - range; cz <= playerChunkZ + range; ++cz) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ChestBlockEntity) && !(blockEntity instanceof EnderChestBlockEntity)) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    if (!EspMath.within(
                            pos.getX() + 0.5 - origin.x(), pos.getY() + 0.5 - origin.y(), pos.getZ() + 0.5 - origin.z(), radiusSq)) {
                        continue;
                    }
                    Gizmos.cuboid(pos, GizmoStyle.stroke(color, LINE_WIDTH)).setAlwaysOnTop();
                }
            }
        }
    }
}
