package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.world.entity.ContextEntityType;
import com.mohistmc.silkard.injected.world.level.ContextServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(EntityType.class)
public abstract class MixinEntityType implements ContextEntityType {

    @Unique
    private static final Logger SILKARD$LOGGER = LoggerFactory.getLogger(EntityType.class);

    @Shadow
    public abstract <T extends Entity> T create(ServerLevel level, PostSpawnProcessor<T> postSpawnConfig, BlockPos spawnPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp);

    // SPIGOT-5665
    @Redirect(method = "lambda$appendCustomEntityStackConfig$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;updateCustomEntityTag(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/component/TypedEntityData;)V"))
    private static void silkard$updateCustomEntityTag(Level level, LivingEntity user, Entity entity, TypedEntityData<EntityType<?>> typedEntityData) {
        try {
            EntityType.updateCustomEntityTag(level, user, entity, typedEntityData);
        } catch (Throwable throwable) {
            SILKARD$LOGGER.warn("Error loading spawn egg NBT", throwable);
        }
    }

    // CraftBukkit start - bukkit spawn reason overloads
    public Entity spawn(ServerLevel level, BlockPos spawnPos, EntitySpawnReason spawnReason, CreatureSpawnEvent.SpawnReason bukkitSpawnReason) {
        return this.spawn(level, (PostSpawnProcessor<?>) null, spawnPos, spawnReason, false, false, bukkitSpawnReason);
    }

    public Entity spawn(ServerLevel level, ItemStack itemStack, LivingEntity user, BlockPos spawnPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp, CreatureSpawnEvent.SpawnReason bukkitSpawnReason) {
        PostSpawnProcessor<?> postSpawnProcessor = itemStack != null ? EntityType.createDefaultStackConfig(level, itemStack, user) : PostSpawnProcessor.nop();
        return this.spawn(level, postSpawnProcessor, spawnPos, spawnReason, tryMoveDown, movedUp, bukkitSpawnReason);
    }

    public Entity spawn(ServerLevel level, PostSpawnProcessor<?> postSpawnConfig, BlockPos spawnPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp, CreatureSpawnEvent.SpawnReason bukkitSpawnReason) {
        Entity entity = this.create(level, postSpawnConfig, spawnPos, spawnReason, tryMoveDown, movedUp);
        if (entity != null) {
            ((ContextServerLevelAccessor) (Object) level).addFreshEntityWithPassengers(entity, bukkitSpawnReason);
            if (entity.isRemoved()) {
                return null;
            }
            if (entity instanceof Mob mob) {
                mob.playAmbientSound();
            }
        }
        return entity;
    }
    // CraftBukkit end
}
