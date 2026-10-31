package com.mohistmc.silkard.mixin.vanilla.world.entity.monster.zombie;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextMob;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import com.mohistmc.silkard.injected.world.level.ContextLevelWriter;
import com.mohistmc.silkard.injected.world.level.ContextServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ServerLevelAccessor;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(Zombie.class)
public abstract class MixinZombie {

    // CraftBukkit start - infection transform reason
    @Redirect(method = "convertVillagerToZombieVillager", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob silkard$convertVillager(Villager villager, EntityType<ZombieVillager> entityType, ConversionParams params, ConversionParams.AfterConversion<ZombieVillager> afterConversion) {
        return ((ContextMob) (Object) villager).convertTo(entityType, params, afterConversion, EntityTransformEvent.TransformReason.INFECTION, CreatureSpawnEvent.SpawnReason.INFECTION);
    }
    // CraftBukkit end

    // CraftBukkit start - reinforcement target reason and spawn reason
    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/Zombie;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void silkard$setTarget(Zombie self, LivingEntity target) {
        ((ContextMob) (Object) self).setTarget(target, EntityTargetEvent.TargetReason.REINFORCEMENT_TARGET, true);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void silkard$addReinforcement(ServerLevel level, Entity entity) {
        ((ContextServerLevelAccessor) (Object) level).addFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.REINFORCEMENTS);
    }
    // CraftBukkit end

    // CraftBukkit start - EntityCombustByEntityEvent
    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void silkard$ignite(Entity target, float duration) {
        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(((ContextEntity) (Object) this).getBukkitEntity(),
                ((ContextEntity) (Object) target).getBukkitEntity(), duration);
        ((ContextLevel) (Object) ((Entity) (Object) this).level()).getCraftServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            ((ContextEntity) (Object) target).igniteForSeconds(event.getDuration(), false);
        }
    }
    // CraftBukkit end

    // CraftBukkit start - jockey chicken spawn reason
    @Redirect(method = "finalizeSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean silkard$addJockey(ServerLevelAccessor level, Entity entity) {
        return ((ContextLevelWriter) (Object) level).addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.MOUNT);
    }
    // CraftBukkit end
}
