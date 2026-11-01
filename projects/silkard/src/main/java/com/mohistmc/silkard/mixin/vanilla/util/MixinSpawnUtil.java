package com.mohistmc.silkard.mixin.vanilla.util;

import com.llamalad7.mixinextras.sugar.Local;
import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.level.ContextServerLevelAccessor;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/28
 */
@Mixin(SpawnUtil.class)
public class MixinSpawnUtil {

    // CraftBukkit start
    @Redirect(method = "trySpawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void silkard$addFreshEntity(ServerLevel level, Entity entity) {
        ((ContextServerLevelAccessor) level).addFreshEntityWithPassengers(entity, com.mohistmc.silkard.injected.util.ContextSpawnUtil.consumeSpawnReason(CreatureSpawnEvent.SpawnReason.DEFAULT));
    }

    @Inject(method = "trySpawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
    private static void silkard$spawnCancelled(EntityType<?> entityType, EntitySpawnReason spawnReason, ServerLevel level, BlockPos start, int spawnAttempts, int spawnRangeXZ, int spawnRangeY, SpawnUtil.Strategy strategy, boolean checkCollisions, CallbackInfoReturnable<Optional<Mob>> cir, @Local Mob mob) {
        if (mob.isRemoved()) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Redirect(method = "trySpawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private static void silkard$discard(Mob mob) {
        ((ContextEntity) mob).discard(null);
    }
    // CraftBukkit end
}
