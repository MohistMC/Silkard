package com.mohistmc.silkard.injected.world.entity.player;

import com.mohistmc.silkard.util.ContextStateException;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * @author Mgazul
 * @date 2026/5/13 21:38
 */
public interface ContextPlayerBedSleepingProblem {

    default PlayerBedEnterEvent.BedEnterResult bukkit() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setBukkit(PlayerBedEnterEvent.BedEnterResult result) {
        throw new ContextStateException("Not implemented");
    }
}
