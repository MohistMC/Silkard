package com.mohistmc.silkard.injected.world.clock;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
public interface ContextServerClockInstance {

    default void silkard$totalTicks(long totalTicks) {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$partialTick(float partialTick) {
        throw new ContextStateException("Not implemented");
    }

    default ClockNetworkState silkard$packNetworkState(Holder<WorldClock> clock, ServerPlayer player) {
        throw new ContextStateException("Not implemented");
    }
}
