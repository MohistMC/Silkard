package com.mohistmc.silkard.injected.server.network.config;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.server.level.ServerPlayer;

public interface ContextPrepareSpawnTask {

    default ServerPlayer silkard$player() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setPlayer(ServerPlayer player) {
        throw new ContextStateException("Not implemented");
    }
}
