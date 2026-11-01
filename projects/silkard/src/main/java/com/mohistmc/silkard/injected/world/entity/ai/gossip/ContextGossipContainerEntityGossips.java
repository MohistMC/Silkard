package com.mohistmc.silkard.injected.world.entity.ai.gossip;

import com.mohistmc.silkard.util.ContextStateException;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * @author Mgazul
 * @date 2026/5/13 21:54
 */
public interface ContextGossipContainerEntityGossips {

    default int unweightedValue(Predicate<GossipType> predicate) {
        throw new ContextStateException("Not implemented");
    }

    default void decay(Villager villager, UUID uuid) {
        throw new ContextStateException("Not implemented");
    }

    default int getRawValue(GossipType type) {
        throw new ContextStateException("Not implemented");
    }

    default void setRawValue(GossipType type, int value) {
        throw new ContextStateException("Not implemented");
    }
}
