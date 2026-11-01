package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.world.entity.ContextConversionType;
import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextMob;
import com.mohistmc.silkard.injected.world.level.ContextLevelWriter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/4/2 20:01
 */
@Mixin(Mob.class)
public abstract class MixinMob implements ContextMob {

    @Unique
    public boolean aware = true; // CraftBukkit

    @Shadow
    @Nullable
    private LivingEntity target;

    @Shadow
    public abstract @Nullable LivingEntity getTarget();

    @Shadow
    protected abstract @Nullable LivingEntity asValidTarget(@Nullable LivingEntity target);

    @Shadow
    protected abstract SoundEvent getAmbientSound();

    @Shadow
    protected abstract boolean canHoldItem(ItemStack stack);

    @Unique
    private @Nullable ItemEntity silkard$pickupItemEntity;

    // CraftBukkit - delegate for CraftMob
    @Override
    public SoundEvent getAmbientSound0() {
        return this.getAmbientSound();
    }

    @Override
    public boolean silkard_aware() {
        return aware;
    }

    @Override
    public void silkard_aware(boolean aware){
        this.aware = aware;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - delegate to event overload
     */
    @Overwrite
    public void setTarget(@Nullable LivingEntity target) {
        // CraftBukkit start - fire event
        this.setTarget(target, EntityTargetEvent.TargetReason.UNKNOWN, true);
    }

    // CraftBukkit start
    @Override
    public boolean setTarget(@Nullable LivingEntity target, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        return ((ContextMob) (Object) this).silkard$superSetTarget(target, reason, fireEvent);
    }

    @Override
    public boolean silkard$superSetTarget(@Nullable LivingEntity target, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        Mob handle = (Mob) (Object) this;
        target = this.asValidTarget(target);
        if (getTarget() == target) return false;
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && getTarget() != null && target == null) {
                reason = getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                handle.level().getCraftServer().getLogger().log(java.util.logging.Level.WARNING, "Unknown target reason, please report on the issue tracker", new Exception());
            }
            CraftLivingEntity ctarget = null;
            if (target != null) {
                ctarget = (CraftLivingEntity) target.getBukkitEntity();
            }
            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(handle.getBukkitEntity(), ctarget, reason);
            handle.level().getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            if (event.getTarget() != null) {
                target = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                target = null;
            }
        }
        this.target = target;
        return true;
    }

    // CraftBukkit start - conversion events (4-arg convertTo kept vanilla for Fabric @Inject anchors)
    @Override
    public <T extends Mob> @Nullable T convertTo(EntityType<T> entityType, ConversionParams params, ConversionParams.AfterConversion<T> afterConversion, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason spawnReason) {
        return ((ContextMob) (Object) this).convertTo(entityType, params, EntitySpawnReason.CONVERSION, afterConversion, transformReason, spawnReason);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Mob> @Nullable T convertTo(EntityType<T> entityType, ConversionParams params, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason bukkitSpawnReason) {
        if (((Mob) (Object) this).isRemoved()) {
            return null;
        }
        T converted = (T) entityType.create(((Mob) (Object) this).level(), spawnReason);
        if (converted == null) {
            return null;
        }
        params.type().convert((Mob) (Object) this, converted, params);
        afterConversion.finalizeConversion(converted);
        if (transformReason == null) {
            return converted;
        }
        if (CraftEventFactory.callEntityTransformEvent((LivingEntity) (Object) this, converted, transformReason).isCancelled()) {
            return null;
        }
        ((ContextConversionType) params.type()).postConvert((Mob) (Object) this, converted, params);
        if (((Mob) (Object) this).level() instanceof ServerLevel serverLevel) {
            ((ContextLevelWriter) serverLevel).addFreshEntity(converted, bukkitSpawnReason);
        }
        if (params.type().shouldDiscardAfterConversion()) {
            ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.TRANSFORMATION);
        }
        return converted;
    }
    // CraftBukkit end

    // CraftBukkit start - aware flag + despawn/pickup/unleash events
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void silkard$saveAware(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("Bukkit.Aware", this.aware);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void silkard$readAware(ValueInput input, CallbackInfo ci) {
        this.aware = input.getBooleanOr("Bukkit.Aware", this.aware);
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void silkard$awareGuard(CallbackInfo ci) {
        if (!this.aware) {
            ci.cancel();
        }
    }

    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "discard()V"))
    private void silkard$despawnDiscard(Mob self) {
        ((ContextEntity) self).discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;equipItemIfPossible(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack silkard$pickupEquip(Mob self, ServerLevel level, ItemStack stack) {
        return this.equipItemIfPossible(level, stack, this.silkard$pickupItemEntity);
    }

    @Redirect(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void silkard$pickupDiscard(ItemEntity itemEntity) {
        ((ContextEntity) itemEntity).discard(EntityRemoveEvent.Cause.PICKUP);
    }

    @Redirect(method = "equipItemIfPossible", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;canHoldItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean silkard$canHoldItem(Mob self, ItemStack stack) {
        boolean can = self.canHoldItem(stack);
        if (this.silkard$pickupItemEntity != null) {
            return !CraftEventFactory.callEntityPickupItemEvent(self, this.silkard$pickupItemEntity, 0, !can).isCancelled();
        }
        return can;
    }

    @Redirect(method = "equipItemIfPossible", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity silkard$forceDrops(Mob self, ServerLevel level, ItemStack stack) {
        ((ContextEntity) self).silkard$forceDrops(true);
        ItemEntity dropped = ((Entity) self).spawnAtLocation(level, stack);
        ((ContextEntity) self).silkard$forceDrops(false);
        return dropped;
    }

    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack itemStack, @Nullable ItemEntity itemEntity) {
        this.silkard$pickupItemEntity = itemEntity;
        try {
            return ((Mob) (Object) this).equipItemIfPossible(level, itemStack);
        } finally {
            this.silkard$pickupItemEntity = null;
        }
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "dropLeash()V", shift = At.Shift.BEFORE))
    private void silkard$unleashOnRide(Entity entity, boolean force, boolean sendEventAndTriggers, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        org.bukkit.Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(((Mob) (Object) this).getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }
    // CraftBukkit end
}
