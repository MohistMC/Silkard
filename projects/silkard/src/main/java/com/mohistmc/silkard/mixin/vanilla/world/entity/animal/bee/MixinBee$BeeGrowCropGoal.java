package com.mohistmc.silkard.mixin.vanilla.world.entity.animal.bee;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeGrowCropGoal")
public abstract class MixinBee$BeeGrowCropGoal {

    @Shadow
    @Final
    private Bee this$0;

    // CraftBukkit start - EntityChangeBlockEvent
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean silkard$entityChangeBlock(Level level, BlockPos pos, BlockState state) {
        return CraftEventFactory.callEntityChangeBlockEvent(this.this$0, pos, state) && level.setBlockAndUpdate(pos, state);
    }
    // CraftBukkit end
}