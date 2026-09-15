package com.mohistmc.silkard.injected.world.entity.raid;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.world.entity.raid.Raider;

public interface ContextRaid {

    default boolean isInProgress() {
        throw new ContextStateException("Not implemented");
    }

    default java.util.Collection<Raider> getRaiders() {
        throw new ContextStateException("Not implemented");
    }

    default void setTotalWaves(int waves) {
        throw new ContextStateException("Not implemented");
    }

}
