package com.mohistmc.silkard.injected.util;

import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
public interface ContextSpawnUtil {

    ThreadLocal<CreatureSpawnEvent.SpawnReason> SPAWN_REASON = new ThreadLocal<>();

    static CreatureSpawnEvent.SpawnReason consumeSpawnReason(CreatureSpawnEvent.SpawnReason fallback) {
        CreatureSpawnEvent.SpawnReason reason = SPAWN_REASON.get();
        SPAWN_REASON.remove();
        return reason == null ? fallback : reason;
    }
}
