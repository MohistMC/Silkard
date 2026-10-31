package com.mohistmc.silkard.injected.world.entity.vehicle.minecart;

import com.mohistmc.silkard.util.ContextStateException;
import org.bukkit.util.Vector;

public interface ContextAbstractMinecart {

    default boolean silkard_slowWhenEmpty() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard_slowWhenEmpty(boolean slowWhenEmpty) {
        throw new ContextStateException("Not implemented");
    }

    default double silkard_maxSpeed() {
        throw new ContextStateException("Not implemented");
    }

    default Double silkard$maxSpeedOrNull() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard_maxSpeed(double maxSpeed) {
        throw new ContextStateException("Not implemented");
    }

    default void silkard_powRailAccelMult(double powRailAccelMult) {
        throw new ContextStateException("Not implemented");
    }

    default double silkard_powRailAccelMult() {
        throw new ContextStateException("Not implemented");
    }

    default Vector getFlyingVelocityMod() {
        throw new IllegalStateException("Not implemented");
    }

    default void setFlyingVelocityMod(Vector flying) {
        throw new IllegalStateException("Not implemented");
    }

    default Vector getDerailedVelocityMod() {
        throw new IllegalStateException("Not implemented");
    }

    default void setDerailedVelocityMod(Vector derailed) {
        throw new IllegalStateException("Not implemented");
    }
}
