package com.mohistmc.silkard.mixin.vanilla.world.level.chunk.storage;

import com.mohistmc.silkard.injected.world.level.chunk.storage.ContextSimpleRegionStorage;
import java.util.concurrent.ExecutionException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(SimpleRegionStorage.class)
public abstract class MixinSimpleRegionStorage implements ContextSimpleRegionStorage {

    // CraftBukkit start - mark old chunks as light populated so the light engine does not regenerate them
    @Override
    public CompoundTag upgradeChunkTag(CompoundTag chunkTag, int defaultVersion, @Nullable CompoundTag dataFixContextTag, int targetVersion, ChunkPos pos, @Nullable LevelAccessor levelaccessor) {
        int k = NbtUtils.getDataVersion(chunkTag, defaultVersion);
        if (k >= targetVersion) {
            return chunkTag;
        }
        if (k < 1466 && pos != null) {
            CompoundTag level = chunkTag.getCompoundOrEmpty("Level");
            if (level.getBooleanOr("TerrainPopulated", false) && !level.getBooleanOr("LightPopulated", false)) {
                ServerChunkCache cps = (levelaccessor == null) ? null : ((ServerLevel) levelaccessor).getChunkSource();
                if (this.silkard$check(cps, pos.x() - 1, pos.z()) && this.silkard$check(cps, pos.x() - 1, pos.z() - 1) && this.silkard$check(cps, pos.x(), pos.z() - 1)) {
                    level.putBoolean("LightPopulated", true);
                }
            }
        }
        return ((SimpleRegionStorage) (Object) this).upgradeChunkTag(chunkTag, defaultVersion, dataFixContextTag, targetVersion);
    }

    @Unique
    private boolean silkard$check(ServerChunkCache cps, int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        if (cps != null && cps.hasChunk(x, z)) {
            return true;
        }

        CompoundTag nbt;
        try {
            nbt = ((SimpleRegionStorage) (Object) this).read(pos).get().orElse(null);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
        if (nbt != null) {
            CompoundTag level = nbt.getCompoundOrEmpty("Level");
            if (level.getBooleanOr("TerrainPopulated", false)) {
                return true;
            }

            ChunkStatus status = ChunkStatus.byName(level.getStringOr("Status", ""));
            if (status != null && status.isOrAfter(ChunkStatus.FEATURES)) {
                return true;
            }
        }

        return false;
    }
    // CraftBukkit end
}
