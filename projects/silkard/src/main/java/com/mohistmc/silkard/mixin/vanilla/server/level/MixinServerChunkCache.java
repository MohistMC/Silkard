package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.level.ContextChunkMap;
import com.mohistmc.silkard.injected.server.level.ContextServerChunkCache;
import com.mohistmc.silkard.injected.world.level.ContextSpawnState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.entity.SpawnCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCache implements ContextServerChunkCache {

    // CraftBukkit start - properly implement isChunkLoaded
    @org.spongepowered.asm.mixin.Shadow
    @org.spongepowered.asm.mixin.Final
    private net.minecraft.server.level.ChunkMap chunkMap;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Thread mainThread;

    @Shadow
    @Final
    private long[] lastChunkPos;

    @Shadow
    @Final
    private net.minecraft.world.level.chunk.status.ChunkStatus[] lastChunkStatus;

    @Shadow
    @Final
    private net.minecraft.world.level.chunk.ChunkAccess[] lastChunk;

    @Shadow
    @Final
    private net.minecraft.server.level.ServerChunkCache.MainThreadExecutor mainThreadProcessor;

    @Shadow
    private net.minecraft.server.level.ChunkHolder getVisibleChunkIfPresent(long key) {
        throw new AssertionError();
    }

    @Shadow
    private boolean chunkAbsent(net.minecraft.server.level.ChunkHolder holder, int targetTicketLevel) {
        throw new AssertionError();
    }

    @Shadow
    private java.util.concurrent.CompletableFuture<net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess>> getChunkFutureMainThread(int x, int z, net.minecraft.world.level.chunk.status.ChunkStatus targetStatus, boolean loadOrGenerate) {
        throw new AssertionError();
    }

    @Shadow
    private void storeInCache(long pos, net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.world.level.chunk.status.ChunkStatus status) {
        throw new AssertionError();
    }

    @Shadow
    @Final
    private net.minecraft.server.level.ThreadedLevelLightEngine lightEngine;

    @Shadow
    @Final
    private net.minecraft.world.level.storage.SavedDataStorage savedDataStorage;

    @Shadow
    @Final
    private net.minecraft.world.level.TicketStorage ticketStorage;

    @Shadow
    public abstract void save(boolean save);

    @Shadow
    private void clearCache() {
        throw new AssertionError();
    }

    @Shadow
    boolean runDistanceManagerUpdates() {
        throw new AssertionError();
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        net.minecraft.server.level.ChunkHolder chunk = this.chunkMap.getUpdatingChunkIfPresent(net.minecraft.world.level.ChunkPos.pack(chunkX, chunkZ));
        if (chunk == null) {
            return false;
        }
        return chunk.getChunkIfPresent(net.minecraft.world.level.chunk.status.ChunkStatus.FULL) != null; // CraftBukkit - 26.3 GenerationChunkHolder
    }
    // CraftBukkit end

    // CraftBukkit - run chunk callbacks in pollTask finally
    @org.spongepowered.asm.mixin.injection.Inject(method = "pollTask", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void silkard$runChunkCallbacks(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        ((com.mohistmc.silkard.injected.server.level.ContextChunkMap) this.chunkMap).silkard$callbackExecutor().run();
    }

    // CraftBukkit start - close without saving
    @Override
    public void close(boolean save) throws IOException {
        if (save) {
            this.save(true);
        }
        this.savedDataStorage.close();
        this.lightEngine.close();
        this.chunkMap.close();
    }
    // CraftBukkit end

    // CraftBukkit start - purge unloading chunks
    @Override
    public void purgeUnload() {
        ProfilerFiller profilerfiller = Profiler.get();

        profilerfiller.push("purge");
        this.ticketStorage.purgeStaleTickets(this.chunkMap);
        this.runDistanceManagerUpdates();
        profilerfiller.popPush("unload");
        ((ContextChunkMap) (Object) this.chunkMap).silkard$tick(() -> true);
        profilerfiller.pop();
        this.clearCache();    }
    // CraftBukkit end

    // CraftBukkit start - per-world spawn limits
    @Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getFilteredSpawningCategories(Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZ)Ljava/util/List;"))
    private List<MobCategory> silkard$filteredSpawningCategories(NaturalSpawner.SpawnState state, boolean spawnEnemies, boolean spawnPersistent) {
        ServerLevel serverlevel = this.level;
        LevelData leveldata = serverlevel.getLevelData();
        List<MobCategory> list = new ArrayList<>();

        long animalTicks = serverlevel.silkard$ticksPerSpawnCategory().getLong(SpawnCategory.ANIMAL);
        boolean spawnPersistentNow = animalTicks != 0L && leveldata.getGameTime() % animalTicks == 0L;

        for (MobCategory mobcategory : MobCategory.values()) {
            if (mobcategory == MobCategory.MISC) {
                continue;
            }

            boolean spawnThisTick = true;
            int limit = mobcategory.getMaxInstancesPerChunk();
            SpawnCategory spawnCategory = CraftSpawnCategory.toBukkit(mobcategory);
            if (CraftSpawnCategory.isValidForLimits(spawnCategory)) {
                long ticks = serverlevel.silkard$ticksPerSpawnCategory().getLong(spawnCategory);
                spawnThisTick = ticks != 0L && leveldata.getGameTime() % ticks == 0L;
                limit = serverlevel.getWorld().getSpawnLimit(spawnCategory);
            }

            if (!spawnThisTick || limit == 0) {
                continue;
            }

            if ((spawnEnemies || mobcategory.isFriendly()) && (spawnPersistentNow || !mobcategory.isPersistent()) && ((ContextSpawnState) state).canSpawnForCategory(mobcategory, limit)) {
                list.add(mobcategory);
            }
        }

        return list;
    }
    // CraftBukkit end

    // CraftBukkit - the cached chunk can become accessible in the meantime
    @org.spongepowered.asm.mixin.injection.Inject(method = "getChunk", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void silkard$getChunk(int x, int z, net.minecraft.world.level.chunk.status.ChunkStatus targetStatus, boolean loadOrGenerate, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.level.chunk.ChunkAccess> cir) {
        if (Thread.currentThread() != this.mainThread) {
            return;
        }
        Profiler.get().incrementCounter("getChunk");
        long pos = net.minecraft.world.level.ChunkPos.pack(x, z);

        for (int i = 0; i < 4; i++) {
            if (pos == this.lastChunkPos[i] && targetStatus == this.lastChunkStatus[i]) {
                net.minecraft.world.level.chunk.ChunkAccess chunkAccess = this.lastChunk[i];
                if (chunkAccess != null) {
                    cir.setReturnValue(chunkAccess);
                    return;
                }
            }
        }

        Profiler.get().incrementCounter("getChunkCacheMiss");
        java.util.concurrent.CompletableFuture<net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess>> future = this.getChunkFutureMainThread(x, z, targetStatus, loadOrGenerate);
        this.mainThreadProcessor.managedBlock(future::isDone);
        net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess> result = future.join();
        net.minecraft.world.level.chunk.ChunkAccess chunk = result.orElse(null);
        if (chunk == null && loadOrGenerate) {
            throw net.minecraft.util.Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + result.getError()));
        }
        this.storeInCache(pos, chunk, targetStatus);
        cir.setReturnValue(chunk);
    }

    // CraftBukkit - don't add a new ticket for a currently unloading chunk
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "getChunkFutureMainThread", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), argsOnly = true)
    private boolean silkard$skipUnloadingTicket(boolean loadOrGenerate, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true, ordinal = 0) int x, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true, ordinal = 1) int z) {
        net.minecraft.server.level.ChunkHolder holder = this.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.pack(x, z));
        boolean currentlyUnloading = false;
        if (holder != null) {
            net.minecraft.server.level.FullChunkStatus oldState = net.minecraft.server.level.ChunkLevel.fullStatus(holder.oldTicketLevel);
            net.minecraft.server.level.FullChunkStatus currentState = net.minecraft.server.level.ChunkLevel.fullStatus(holder.getTicketLevel());
            currentlyUnloading = oldState.isOrAfter(net.minecraft.server.level.FullChunkStatus.FULL) && !currentState.isOrAfter(net.minecraft.server.level.FullChunkStatus.FULL);
        }
        return loadOrGenerate && !currentlyUnloading;
    }

    // CraftBukkit - use oldTicketLevel for isLoaded checks
    @org.spongepowered.asm.mixin.injection.Redirect(method = "chunkAbsent", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTicketLevel()I"))
    private int silkard$oldTicketLevel(net.minecraft.server.level.ChunkHolder holder) {
        return holder.oldTicketLevel;
    }

    // CraftBukkit - disable mob spawning when no players are online
    @org.spongepowered.asm.mixin.injection.Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object silkard$mobSpawningRule(net.minecraft.world.level.gamerules.GameRules rules, net.minecraft.world.level.gamerules.GameRule rule) {
        Object value = rules.get(rule);
        if (rule == net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS) {
            return (Boolean) value && !this.level.players().isEmpty();
        }
        return value;
    }
}
