package com.mohistmc.silkard.mixin.vanilla.world.level.entity;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.level.entity.ContextPersistentEntitySectionManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinPersistentEntitySectionManager implements ContextPersistentEntitySectionManager {

    @Shadow @Final private EntitySectionStorage<Entity> sectionStorage;
    @Shadow @Final private Long2ObjectMap chunkLoadStatuses;
    @Shadow @Final private EntityPersistentStorage permanentStorage;

    @Shadow public abstract void saveAll();

    // true only while a chunk unload is storing its entities (CraftBukkit fires the unload event there)
    @Unique private boolean silkard$unloadEvent;

    @Shadow
    private boolean storeChunkSections(long chunkPos, Consumer consumer) {
        throw new AssertionError();
    }

    // CraftBukkit start - add method to get all entities in chunk
    @Override
    public List<Entity> getEntities(ChunkPos chunkCoordIntPair) {
        return this.sectionStorage.getExistingSectionsInChunk(chunkCoordIntPair.pack())
                .flatMap(EntitySection<Entity>::getEntities)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPending(long pair) {
        Object status = this.chunkLoadStatuses.get(pair);
        // ChunkLoadStatus is package private, compare by enum name
        return status instanceof Enum<?> constant && "PENDING".equals(constant.name());
    }
    // CraftBukkit end

    // CraftBukkit start - add boolean for event call
    @Override
    public boolean storeChunkSections(long chunkPos, Consumer consumer, boolean callEvent) {
        boolean previous = this.silkard$unloadEvent;
        this.silkard$unloadEvent = callEvent;
        try {
            return this.storeChunkSections(chunkPos, consumer);
        } finally {
            this.silkard$unloadEvent = previous;
        }
    }

    @Redirect(method = "processChunkUnload(J)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;storeChunkSections(JLjava/util/function/Consumer;)Z"))
    private boolean silkard$processChunkUnloadStore(PersistentEntitySectionManager self, long chunkKey, Consumer visitor) {
        return this.storeChunkSections(chunkKey, visitor, true);
    }

    @Redirect(method = "storeChunkSections(JLjava/util/function/Consumer;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityPersistentStorage;storeEntities(Lnet/minecraft/world/level/entity/ChunkEntities;)V"))
    private void silkard$entitiesUnloadEvent(EntityPersistentStorage storage, ChunkEntities chunkEntities) {
        if (this.silkard$unloadEvent) {
            this.silkard$unloadEvent = false;
            ChunkPos pos = chunkEntities.getPos();
            CraftEventFactory.callEntitiesUnloadEvent(this.silkard$level(), pos, this.getEntities(pos));
        }
        storage.storeEntities(chunkEntities);
    }
    // CraftBukkit end

    // CraftBukkit - Mark entity as in world
    @Inject(method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntitySection;getStatus()Lnet/minecraft/world/level/entity/Visibility;", ordinal = 0))
    private void silkard$markInWorld(EntityAccess entity, boolean loaded, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Entity actualEntity) {
            ((ContextEntity) actualEntity).silkard_inWorld(true);
        }
    }

    // CraftBukkit - add Bukkit remove cause
    @Redirect(method = "unloadEntity(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityAccess;setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void silkard$unloadEntityRemoved(EntityAccess entity, Entity.RemovalReason reason) {
        entity.setRemoved(reason, EntityRemoveEvent.Cause.UNLOAD);
    }

    // CraftBukkit start - call entity load event
    @Redirect(method = "processPendingLoads()V", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private Object silkard$chunkLoaded(Long2ObjectMap map, long key, Object value) {
        Object previous = map.put(key, value);
        ChunkPos pos = ChunkPos.unpack(key);
        CraftEventFactory.callEntitiesLoadEvent(this.silkard$level(), pos, this.getEntities(pos));
        return previous;
    }
    // CraftBukkit end

    // CraftBukkit start - add save boolean
    @Override
    public void close(boolean save) throws IOException {
        if (save) {
            this.saveAll();
        }
        this.permanentStorage.close();
    }
    // CraftBukkit end

    @Unique
    private ServerLevel silkard$level() {
        return ((EntityStorage) this.permanentStorage).level;
    }
}
