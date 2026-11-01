package com.mohistmc.silkard.injected.server.dedicated;

import com.mohistmc.silkard.util.ContextStateException;

public interface ContextDedicatedServerProperties {

    default boolean silkard$debug() {
        throw new ContextStateException("Not implemented");
    }
}
