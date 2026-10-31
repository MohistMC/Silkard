package com.mohistmc.silkard.injected.world.level.portal;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public interface ContextPortalShape {

    default boolean createPortalBlocks(LevelAccessor level, Entity entity) {
        throw new ContextStateException("Not implemented");
    }
}
