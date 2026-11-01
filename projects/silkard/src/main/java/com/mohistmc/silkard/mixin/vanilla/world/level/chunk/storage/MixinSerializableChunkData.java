package com.mohistmc.silkard.mixin.vanilla.world.level.chunk.storage;

import com.mohistmc.silkard.injected.world.level.chunk.ContextChunkAccess;
import com.mohistmc.silkard.injected.world.level.levelgen.structure.ContextStructureStart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(SerializableChunkData.class)
public abstract class MixinSerializableChunkData {

    // CraftBukkit start - store chunk persistent data on the record instance
    @Unique private @Nullable Tag silkard$persistentDataContainer;

    @Inject(method = "parse", at = @At("RETURN"))
    private static void silkard$captureParsed(LevelHeightAccessor levelHeight, PalettedContainerFactory factory, CompoundTag tag, CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        ((MixinSerializableChunkData) (Object) data).silkard$persistentDataContainer = tag.get("ChunkBukkitValues");
    }

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void silkard$captureSaved(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        // SPIGOT-6814: always save the PDC to account for 1.17 to 1.18 chunk upgrading
        if (!((ContextChunkAccess) chunk).silkard_persistentDataContainer().isEmpty()) {
            SerializableChunkData data = cir.getReturnValue();
            ((MixinSerializableChunkData) (Object) data).silkard$persistentDataContainer = ((ContextChunkAccess) chunk).silkard_persistentDataContainer().toTagCompound();
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void silkard$loadPersistentData(ServerLevel level, PoiManager poiManager, RegionStorageInfo storageInfo, ChunkPos chunkPos, CallbackInfoReturnable<ProtoChunk> cir) {
        // SPIGOT-6814: already loaded here to account for 1.17 to 1.18 chunk upgrading
        Tag base = this.silkard$persistentDataContainer;
        if (base instanceof CompoundTag tag) {
            ((ContextChunkAccess) cir.getReturnValue()).silkard_persistentDataContainer().putAll(tag);
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void silkard$savePersistentData(CallbackInfoReturnable<CompoundTag> cir) {
        Tag base = this.silkard$persistentDataContainer;
        if (base != null) {
            cir.getReturnValue().put("ChunkBukkitValues", base);
        }
    }

    @Redirect(method = "unpackStructureStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructureStart;loadStaticStart(Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePieceSerializationContext;Lnet/minecraft/nbt/CompoundTag;J)Lnet/minecraft/world/level/levelgen/structure/StructureStart;"))
    private static StructureStart silkard$loadStructurePersistentData(StructurePieceSerializationContext context, CompoundTag tag, long seed) {
        StructureStart start = StructureStart.loadStaticStart(context, tag, seed);
        if (start != null) {
            Tag base = tag.get("StructureBukkitValues");
            if (base instanceof CompoundTag structureTag) {
                ((ContextStructureStart) start).silkard_persistentDataContainer().putAll(structureTag);
            }
        }
        return start;
    }
    // CraftBukkit end
}
