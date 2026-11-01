package com.mohistmc.silkard.injected.world.level.chunk.storage;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

public interface ContextSimpleRegionStorage {

    // CraftBukkit start - extra overload that also fixes the LightPopulated flag of old chunks
    default CompoundTag upgradeChunkTag(CompoundTag chunkTag, int defaultVersion, @Nullable CompoundTag dataFixContextTag, int targetVersion, ChunkPos pos, @Nullable LevelAccessor levelaccessor) {
        throw new ContextStateException("Not implemented");
    }
    // CraftBukkit end
}
