package com.mohistmc.silkard.injected.world.level.block.state;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface ContextBlockBehaviour {

    default void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, UseOnContext context) {
        throw new ContextStateException("Not implemented");
    }
}
