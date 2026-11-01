package com.mohistmc.silkard.mixin.vanilla.world.level.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.bukkit.craftbukkit.CraftGameEvent;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(VibrationSystem.Listener.class)
public class MixinVibrationSystem$Listener {

    // CraftBukkit start - BlockReceiveGameEvent
    @WrapOperation(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$User;canReceiveVibration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)Z"))
    private boolean silkard$receiveGameEvent(VibrationSystem.User instance, ServerLevel serverLevel, BlockPos blockPos, Holder<GameEvent> gameEventHolder, GameEvent.Context context, Operation<Boolean> original) {
        boolean canReceive = original.call(instance, serverLevel, blockPos, gameEventHolder, context);
        BlockReceiveGameEvent bukkitEvent = new BlockReceiveGameEvent(
                CraftGameEvent.minecraftToBukkit(gameEventHolder.value()),
                CraftBlock.at(serverLevel, blockPos),
                context.sourceEntity() == null ? null : context.sourceEntity().getBukkitEntity());
        bukkitEvent.setCancelled(!canReceive);
        serverLevel.getCraftServer().getPluginManager().callEvent(bukkitEvent);
        return !bukkitEvent.isCancelled();
    }
    // CraftBukkit end
}