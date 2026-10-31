package com.mohistmc.silkard.injected.world.entity;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface ContextEntityType<T extends Entity> {

    default Entity spawn(ServerLevel level, BlockPos spawnPos, EntitySpawnReason spawnReason, CreatureSpawnEvent.SpawnReason bukkitSpawnReason) {
        throw new ContextStateException("Not implemented");
    }
}
