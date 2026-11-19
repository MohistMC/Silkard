package com.mohistmc.silkard.mixin.vanilla.world.inventory;

import com.mohistmc.silkard.injected.world.inventory.ContextContainerLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(ContainerLevelAccess.class)
public interface MixinContainerLevelAccess extends ContextContainerLevelAccess {

    // CraftBukkit start
    @Override
    default Level getWorld() {
        return null;
    }

    @Override
    default BlockPos getPosition() {
        return null;
    }

    @Override
    default Location getLocation() {
        Level world = this.getWorld();
        BlockPos pos = this.getPosition();
        if (world == null || pos == null) {
            return null;
        }
        return new Location(world.getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }
    // CraftBukkit end
}