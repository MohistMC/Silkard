package com.mohistmc.silkard.injected.world.clock;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import org.bukkit.event.world.TimeSkipEvent;

/**
 * @author Mgazul
 * @date 2026/5/13 22:00
 */
public interface ContextServerClockManager {

    default void silkard$init(ServerLevel level) {
        throw new ContextStateException("Not implemented");
    }

    default void setTotalTicks(Holder<WorldClock> clock, long totalTicks, TimeSkipEvent.SkipReason reason) {
        throw new ContextStateException("Not implemented");
    }
}
