package com.mohistmc.silkard.injected.world.entity.animal.bee;

import com.mohistmc.silkard.util.ContextStateException;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
public interface ContextBee {

    default void silkard$setPollinateGoal(ContextBeePollinateGoal goal) {
        throw new ContextStateException("Not implemented");
    }

    default ContextBeePollinateGoal silkard$pollinateGoal() {
        throw new ContextStateException("Not implemented");
    }
}
