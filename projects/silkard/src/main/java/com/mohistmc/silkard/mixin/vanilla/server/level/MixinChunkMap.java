package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.level.ContextChunkMap;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(ChunkMap.class)
public abstract class MixinChunkMap implements ContextChunkMap {

    @Shadow
    protected void tick(BooleanSupplier supplier) {
        throw new AssertionError();
    }

    // CraftBukkit start - recursion-safe executor for Chunk loadCallback() and unloadCallback()
    public final com.mohistmc.silkard.bukkit.BukkitCallbackExecutor silkard$callbackExecutorField = new com.mohistmc.silkard.bukkit.BukkitCallbackExecutor();

    @Override
    public com.mohistmc.silkard.bukkit.BukkitCallbackExecutor silkard$callbackExecutor() {
        return silkard$callbackExecutorField;
    }
    // CraftBukkit end

    // CraftBukkit start - expose the protected chunk tick for ServerChunkCache#purgeUnload
    @Override
    public void silkard$tick(BooleanSupplier supplier) {
        this.tick(supplier);
    }
    // CraftBukkit end

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private net.minecraft.server.level.ServerLevel level;

    @Shadow
    protected abstract net.minecraft.world.level.chunk.ChunkGenerator generator();

    // CraftBukkit - use the level stem key and pass chunk context to the data fixer
    @org.spongepowered.asm.mixin.Overwrite
    private net.minecraft.nbt.CompoundTag upgradeChunkTag(net.minecraft.nbt.CompoundTag tag) {
        return ((com.mohistmc.silkard.injected.world.level.chunk.storage.ContextSimpleRegionStorage) (Object) this)
                .upgradeChunkTag(tag, -1, silkard$dataFixContextTag(), net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version(), null, this.level);
    }

    @Unique
    private net.minecraft.nbt.CompoundTag silkard$dataFixContextTag() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("dimension", this.level.getTypeKey().identifier().toString());
        this.generator().getTypeNameForDataFixer().ifPresent(id -> tag.putString("generator", id.toString()));
        return tag;
    }
}
