package com.mohistmc.silkard.mixin.vanilla.world.level.chunk;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import com.mohistmc.silkard.injected.world.level.chunk.ContextChunkAccess;
import com.mohistmc.silkard.injected.world.level.chunk.ContextLevelChunk;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk implements ContextLevelChunk {

    @Shadow
    @Final
    private Level level;

    // CraftBukkit start
    @Unique
    public boolean mustNotSave;

    @Unique
    public boolean needsDecoration;
    // CraftBukkit end

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("TAIL"))
    private void silkard$copyChunkData(ServerLevel level, ProtoChunk protoChunk, LevelChunk.PostLoadProcessor postLoad, CallbackInfo ci) {
        this.needsDecoration = true;
        // SPIGOT-6814: keep the persistent data of the proto chunk
        ((ContextChunkAccess) (Object) this).silkard_setPersistentDataContainer(((ContextChunkAccess) protoChunk).silkard_persistentDataContainer());
    }

    // CraftBukkit - Don't place while processing the BlockPlaceEvent, unless it's a BlockContainer. Prevents blocks such as TNT from activating when cancelled.
    @Redirect(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"))
    private void silkard$skipOnPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (((ContextLevel) this.level).silkard$captureBlockStates() && !(state.getBlock() instanceof BaseEntityBlock)) {
            return;
        }
        state.onPlace(level, pos, oldState, movedByPiston);
    }

    @ModifyExpressionValue(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object silkard$capturedBlockEntity(Object original, BlockPos pos, LevelChunk.EntityCreationType type) {
        BlockEntity captured = ((ContextLevel) this.level).silkard$capturedTileEntities().get(pos);
        return captured != null ? captured : original;
    }

    // SPIGOT-5561: also remove the pending block entity
    @Inject(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void silkard$removePendingBlockEntity(BlockPos pos, CallbackInfo ci) {
        ((ContextChunkAccess) (Object) this).silkard$removePendingBlockEntity(pos);
    }

    public boolean isUnsaved() {
        return ((ContextChunkAccess) (Object) this).silkard$rawUnsaved() && !this.mustNotSave;
    }

    @Override
    public void loadCallback() {
        org.bukkit.Server server = ((ContextLevel) this.level).getCraftServer();
        if (server != null) {
            // If it's a new world, the first few chunks are generated inside the World constructor.
            // No CraftWorld/CraftServer exists at that point.
            org.bukkit.Chunk bukkitChunk = new CraftChunk((LevelChunk) (Object) this);
            server.getPluginManager().callEvent(new ChunkLoadEvent(bukkitChunk, this.needsDecoration));

            if (this.needsDecoration) {
                this.needsDecoration = false;
                long seed = ((ServerLevel) this.level).getSeed();
                Random random = new Random();
                random.setSeed(seed);
                long xRand = random.nextLong() / 2L * 2L + 1L;
                long zRand = random.nextLong() / 2L * 2L + 1L;
                random.setSeed((long) ((ChunkAccess) (Object) this).getPos().x() * xRand + (long) ((ChunkAccess) (Object) this).getPos().z() * zRand ^ seed);

                org.bukkit.World world = ((ContextLevel) this.level).getWorld();
                if (world != null) {
                    ((ContextLevel) this.level).silkard$setPopulating(true);
                    try {
                        for (org.bukkit.generator.BlockPopulator populator : world.getPopulators()) {
                            populator.populate(world, random, bukkitChunk);
                        }
                    } finally {
                        ((ContextLevel) this.level).silkard$setPopulating(false);
                    }
                }
                server.getPluginManager().callEvent(new ChunkPopulateEvent(bukkitChunk));
            }
        }
    }

    @Override
    public void unloadCallback() {
        org.bukkit.Server server = ((ContextLevel) this.level).getCraftServer();
        org.bukkit.Chunk bukkitChunk = new CraftChunk((LevelChunk) (Object) this);
        ChunkUnloadEvent unloadEvent = new ChunkUnloadEvent(bukkitChunk, this.isUnsaved());
        server.getPluginManager().callEvent(unloadEvent);
        // note: saving can be prevented, but not forced if no saving is actually required
        this.mustNotSave = !unloadEvent.isSaveChunk();
    }
}
