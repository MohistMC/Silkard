package com.mohistmc.silkard.injected.world.entity.animal.bee;

import com.mohistmc.silkard.util.ContextStateException;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
public interface ContextBeePollinateGoal {

    default void silkard$stopPollinating() {
        throw new ContextStateException("Not implemented");
    }
}
