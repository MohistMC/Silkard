package com.mohistmc.silkard.injected.world.entity.vehicle.minecart;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

public interface ContextMinecartBehavior {

    default AbstractMinecart silkard$minecart() {
        throw new ContextStateException("Not implemented");
    }
}
