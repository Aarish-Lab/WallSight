package com.example.wallsight.client.mixin;

import com.example.wallsight.client.combat.KnockbackGuard;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Unchanged from 1.0.2: same two injection points, same targets. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @WrapOperation(
            method = "handleSetEntityMotion(Lnet/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V"))
    private void wallsight$keepLocalVelocity(Entity entity, Vec3 motion, Operation<Void> operation) {
        if (entity == Minecraft.getInstance().player && KnockbackGuard.shouldSuppress()) {
            return;
        }
        operation.call(entity, motion);
    }

    @ModifyVariable(
            method = "handleExplosion(Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private ClientboundExplodePacket wallsight$stripLocalExplosionKnockback(ClientboundExplodePacket packet) {
        return KnockbackGuard.withoutPlayerKnockback(packet);
    }
}
