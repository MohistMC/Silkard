package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(AreaEffectCloud.class)
public abstract class MixinAreaEffectCloud {

    @Shadow @Final private Map<Entity, Integer> victims;
    @Shadow private float potionDurationScale;
    @Shadow public PotionContents potionContents;
    @Shadow public float radiusPerTick;

    @Shadow public abstract float getRadius();

    // CraftBukkit start - AreaEffectCloudApplyEvent
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<LivingEntity> silkard$areaEffectCloudApplyEvent(Level level, Class<LivingEntity> entityClass, AABB box) {
        List<LivingEntity> entities = level.getEntitiesOfClass(entityClass, box);
        if (entities.isEmpty()) {
            return entities;
        }

        List<MobEffectInstance> effects = new ArrayList<>();
        this.potionContents.forEachEffect(effects::add, this.potionDurationScale);

        float radius = this.getRadius();
        if (this.radiusPerTick != 0.0F) {
            radius += this.radiusPerTick;
        }

        List<org.bukkit.entity.LivingEntity> affected = new ArrayList<>();
        for (LivingEntity entity : entities) {
            if (!this.victims.containsKey(entity) && entity.isAffectedByPotions() && !effects.stream().noneMatch(entity::canBeAffected)) {
                double dx = entity.getX() - ((Entity) (Object) this).getX();
                double dz = entity.getZ() - ((Entity) (Object) this).getZ();
                if (dx * dx + dz * dz <= radius * radius) {
                    if (((ContextEntity) (Object) entity).getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                        affected.add(living);
                    }
                }
            }
        }

        AreaEffectCloudApplyEvent event = CraftEventFactory.callAreaEffectCloudApplyEvent((AreaEffectCloud) (Object) this, affected);
        if (event.isCancelled()) {
            return List.of();
        }

        List<LivingEntity> result = new ArrayList<>();
        for (org.bukkit.entity.LivingEntity bukkit : event.getAffectedEntities()) {
            if (bukkit instanceof CraftLivingEntity craft) {
                result.add(craft.getHandle());
            }
        }
        return result;
    }
    // CraftBukkit end

    // CraftBukkit start - add Bukkit effect cause
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean silkard$addEffect(LivingEntity entity, MobEffectInstance effect, Entity source) {
        return ((ContextLivingEntity) (Object) entity).addEffect(effect, source, EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD);
    }
    // CraftBukkit end

    // CraftBukkit start - add Bukkit remove cause
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;discard()V"))
    private void silkard$discard(AreaEffectCloud self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DESPAWN);
    }
    // CraftBukkit end
}
