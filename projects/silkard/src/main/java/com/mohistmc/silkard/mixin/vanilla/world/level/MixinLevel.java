package com.mohistmc.silkard.mixin.vanilla.world.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.entity.SpawnCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(Level.class)
public abstract class MixinLevel implements ContextLevel {

    // CraftBukkit start - Added fields
    @Unique
    public org.bukkit.generator.ChunkGenerator generator;

    @Unique
    public boolean preventPoiUpdated; // SPIGOT-5710

    @Unique
    public boolean captureBlockStates;

    @Unique
    public boolean captureTreeGeneration;

    @Unique
    public Map<BlockPos, CapturedBlockState> capturedBlockStates = new java.util.LinkedHashMap<>();

    @Unique
    public Map<BlockPos, BlockEntity> capturedTileEntities = new java.util.HashMap<>();

    @Unique
    public List<ItemEntity> captureDrops;

    @Unique
    public final Object2LongOpenHashMap<SpawnCategory> ticksPerSpawnCategory = new Object2LongOpenHashMap<>();

    @Unique
    public boolean populating;

    @Unique
    public org.bukkit.generator.BiomeProvider biomeProvider;

    @Unique
    public org.bukkit.World.Environment environment;

    @Unique
    public boolean pvpMode;
    // CraftBukkit end

    @Override
    public boolean silkard$preventPoiUpdated() {
        return preventPoiUpdated;
    }

    @Override
    public void silkard$setPreventPoiUpdated(boolean preventPoiUpdated) {
        this.preventPoiUpdated = preventPoiUpdated;
    }

    @Override
    public org.bukkit.generator.BiomeProvider silkard$biomeProvider() {
        return biomeProvider;
    }

    @Override
    public void silkard$setBiomeProvider(org.bukkit.generator.BiomeProvider biomeProvider) {
        this.biomeProvider = biomeProvider;
    }

    @Override
    public org.bukkit.World.Environment silkard$environment() {
        return environment;
    }

    @Override
    public void silkard$setEnvironment(org.bukkit.World.Environment environment) {
        this.environment = environment;
    }

    @Override
    public org.bukkit.generator.ChunkGenerator silkard$generator() {
        return generator;
    }

    @Override
    public void silkard$setGenerator(org.bukkit.generator.ChunkGenerator generator) {
        this.generator = generator;
    }

    @Override
    public boolean silkard$pvpMode() {
        return pvpMode;
    }

    @Override
    public void silkard$setPvpMode(boolean pvpMode) {
        this.pvpMode = pvpMode;
    }

    @Override
    public boolean silkard$captureBlockStates() {
        return captureBlockStates;
    }

    @Override
    public void silkard$captureBlockStates(boolean captureBlockStates) {
        this.captureBlockStates = captureBlockStates;
    }

    @Override
    public boolean silkard$captureTreeGeneration() {
        return captureTreeGeneration;
    }

    @Override
    public void silkard$captureTreeGeneration(boolean captureTreeGeneration) {
        this.captureTreeGeneration = captureTreeGeneration;
    }

    @Override
    public Map<BlockPos, CapturedBlockState> silkard$capturedBlockStates() {
        return capturedBlockStates;
    }

    @Override
    public void silkard$setCapturedBlockStates(Map<BlockPos, CapturedBlockState> capturedBlockStates) {
        this.capturedBlockStates = capturedBlockStates;
    }

    @Override
    public Map<BlockPos, BlockEntity> silkard$capturedTileEntities() {
        return capturedTileEntities;
    }

    @Override
    public void silkard$setCapturedTileEntities(Map<BlockPos, BlockEntity> capturedTileEntities) {
        this.capturedTileEntities = capturedTileEntities;
    }

    @Override
    public List<ItemEntity> silkard$captureDrops() {
        return captureDrops;
    }

    @Override
    public void silkard$setCaptureDrops(List<ItemEntity> captureDrops) {
        this.captureDrops = captureDrops;
    }

    @Override
    public Object2LongOpenHashMap<SpawnCategory> silkard$ticksPerSpawnCategory() {
        return ticksPerSpawnCategory;
    }

    @Override
    public boolean silkard$populating() {
        return populating;
    }

    @Override
    public void silkard$setPopulating(boolean populating) {
        this.populating = populating;
    }

    @Override
    public CraftWorld getWorld() {
        return null; // Overridden in MixinServerLevel (creates/registers the CraftWorld)
    }

    @Override
    public CraftServer getCraftServer() {
        return (CraftServer) Bukkit.getServer();
    }

    @Override
    public ResourceKey<LevelStem> getTypeKey() {
        return null; // Overridden in MixinServerLevel
    }

    // CraftBukkit start - capture tree generation
    @Inject(method = "<init>(Lnet/minecraft/world/level/storage/WritableLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/core/Holder;ZZJI)V", at = @At("TAIL"))
    private void silkard$initTicksPerSpawn(CallbackInfo ci) {
        for (SpawnCategory spawnCategory : SpawnCategory.values()) {
            if (CraftSpawnCategory.isValidForLimits(spawnCategory)) {
                this.ticksPerSpawnCategory.put(spawnCategory, (long) ((ContextLevel) (Object) this).getCraftServer().getTicksPerSpawns(spawnCategory));
            }
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void silkard$captureTreeBlockState(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        if (captureTreeGeneration) {
            CapturedBlockState capturedBlockState = capturedBlockStates.get(pos);
            if (capturedBlockState == null) {
                capturedBlockState = CapturedBlockState.getTreeBlockState((Level) (Object) this, pos, updateFlags);
                capturedBlockStates.put(pos.immutable(), capturedBlockState);
            }
            capturedBlockState.setData(blockState);
            capturedBlockState.setFlag(updateFlags);
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState silkard$captureBlockState(LevelChunk chunk, BlockPos pos, BlockState blockState, int updateFlags, Operation<BlockState> original) {
        boolean captured = false;
        if (captureBlockStates && !capturedBlockStates.containsKey(pos)) {
            capturedBlockStates.put(pos.immutable(), CapturedBlockState.getBlockState((Level) (Object) this, pos, updateFlags));
            captured = true;
        }
        BlockState oldState = original.call(chunk, pos, blockState, updateFlags);
        if (oldState == null && captured) {
            capturedBlockStates.remove(pos);
        }
        return oldState;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.AFTER), cancellable = true)
    private void silkard$skipUpdatesWhileCapturing(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        if (captureBlockStates) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void notifyAndUpdatePhysics(BlockPos blockpos, LevelChunk levelchunk, BlockState oldBlock, BlockState newBlock, BlockState actualBlock, int i, int j) {
        Level level = (Level) (Object) this;
        if (actualBlock == newBlock) {
            if (oldBlock != actualBlock) {
                level.setBlocksDirty(blockpos, oldBlock, actualBlock);
            }

            if ((i & 2) != 0 && (!level.isClientSide() || (i & 4) == 0) && (level.isClientSide() || levelchunk == null || (levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)))) {
                level.sendBlockUpdated(blockpos, oldBlock, newBlock, i);
            }

            if ((i & 1) != 0) {
                level.updateNeighborsAt(blockpos, oldBlock.getBlock(), null);
                if (!level.isClientSide() && newBlock.hasAnalogOutputSignal()) {
                    level.updateNeighbourForOutputSignal(blockpos, newBlock.getBlock());
                }
            }

            if ((i & 16) == 0 && j > 0) {
                int k = i & -34;
                oldBlock.updateIndirectNeighbourShapes(level, blockpos, k, j - 1);
                newBlock.updateNeighbourShapes(level, blockpos, k, j - 1);
                newBlock.updateIndirectNeighbourShapes(level, blockpos, k, j - 1);
            }

            if (!preventPoiUpdated) {
                level.updatePOIOnBlockStateChange(blockpos, oldBlock, actualBlock);
            }
        }
    }

    @Inject(method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void silkard$capturedGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (captureTreeGeneration) {
            CapturedBlockState previous = capturedBlockStates.get(pos);
            if (previous != null) {
                cir.setReturnValue(previous.getHandle());
            }
        }
    }

    @Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true)
    private void silkard$capturedGetBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        BlockEntity captured = capturedTileEntities.get(pos);
        if (captured != null) {
            cir.setReturnValue(captured);
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        BlockEntity captured = capturedTileEntities.get(pos);
        return captured != null ? captured : ((Level) (Object) this).getBlockEntity(pos);
    }

    @Inject(method = "setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"), cancellable = true)
    private void silkard$capturedSetBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        BlockPos pos = blockEntity.getBlockPos();
        if (captureBlockStates) {
            capturedTileEntities.put(pos.immutable(), blockEntity);
            ci.cancel();
        }
    }
    // CraftBukkit end
}
