package com.mohistmc.silkard.mixin.vanilla.world.inventory;

import com.mohistmc.silkard.injected.world.inventory.ContextContainerLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * CraftBukkit - ContainerLevelAccess.create(level, pos) anonymous implementation.
 *
 * @author Mgazul
 */
@Mixin(targets = "net.minecraft.world.inventory.ContainerLevelAccess$2")
public abstract class MixinContainerLevelAccess$Create implements ContextContainerLevelAccess {

    @Shadow @Final Level val$level;
    @Shadow @Final BlockPos val$pos;

    @Override
    public Level getWorld() {
        return this.val$level;
    }

    @Override
    public BlockPos getPosition() {
        return this.val$pos;
    }
}