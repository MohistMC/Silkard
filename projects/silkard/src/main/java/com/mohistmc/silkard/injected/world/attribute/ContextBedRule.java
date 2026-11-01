package com.mohistmc.silkard.injected.world.attribute;

import com.mohistmc.silkard.util.ContextStateException;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
public interface ContextBedRule {

    default PlayerBedEnterEvent.BedEnterResult bukkit() {
        throw new ContextStateException("Not implemented");
    }
}
