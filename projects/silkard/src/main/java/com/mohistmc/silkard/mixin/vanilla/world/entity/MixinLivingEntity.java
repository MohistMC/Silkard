package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.attribute.CraftAttributeMap;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/4/2 17:00
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements ContextLivingEntity {

    // @formatter:off
    @Shadow @Final private AttributeMap attributes;
    @Shadow @Nullable public abstract AttributeInstance getAttribute(Holder<Attribute> holder);
    // @formatter:on


    // CraftBukkit start
    public int expToDrop;
    public ArrayList<ItemStack> drops = new ArrayList<org.bukkit.inventory.ItemStack>();
    public org.bukkit.craftbukkit.attribute.CraftAttributeMap craftAttributes;
    public boolean collides = true;
    public Set<UUID> collidableExemptions = new HashSet<>();
    public boolean bukkitPickUpLoot;
    public int invulnerableDuration = 20;

    public MixinLivingEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void silkard_initBukkit(EntityType<?> entityType, Level level, CallbackInfo ci) {
        this.collides = true;
        this.craftAttributes = new CraftAttributeMap(this.attributes);
        // CraftBukkit - setHealth(getMaxHealth()) inlined and simplified to skip the instanceof check for EntityPlayer, as getBukkitEntity() is not initialized in constructor
        this.entityData.set(LivingEntity.DATA_HEALTH_ID, (float) this.getAttribute(Attributes.MAX_HEALTH).getValue());
    }

    @Override
    public float getBukkitYaw() {
        return getYHeadRot();
    }
    // CraftBukkit end

    @Override
    public CraftAttributeMap silkard_craftAttributes() {
        return craftAttributes;
    }

    @Override
    public void silkard_craftAttributes(CraftAttributeMap craftAttributes) {
        this.craftAttributes = craftAttributes;
    }

    @Override
    public boolean silkard_collides() {
        return collides;
    }

    @Override
    public void silkard_collides(boolean collides) {
        this.collides = collides;
    }

    @Override
    public Set<UUID> silkard_collidableExemptions() {
        return collidableExemptions;
    }

    @Override
    public void silkard_collidableExemptions(Set<UUID> collidableExemptions) {
        this.collidableExemptions = collidableExemptions;
    }

    @Override
    public boolean silkard_bukkitPickUpLoot() {
        return bukkitPickUpLoot;
    }

    @Override
    public void silkard_bukkitPickUpLoot(boolean bukkitPickUpLoot) {
        this.bukkitPickUpLoot = bukkitPickUpLoot;
    }

    @Override
    public int invulnerableDuration() {
       return invulnerableDuration;
    }

    @Override
    public void invulnerableDuration(int duration) {
        invulnerableDuration = duration;
    }

    // CraftBukkit start - LivingEntity.patch
    @Unique
    private int silkard$expToDropField;

    @Override
    public int silkard$expToDrop() {
        return this.silkard$expToDropField;
    }

    @Override
    public void silkard$expToDrop(int expToDrop) {
        this.silkard$expToDropField = expToDrop;
    }

    @Override
    public int getExpReward(net.minecraft.server.level.ServerLevel level, @Nullable Entity entity) {
        if (!this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && (Boolean) level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS))) {
            return ((LivingEntity) (Object) this).getExperienceReward(level, entity);
        }
        return 0;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - award the event-modified experience amount
     */
    @Overwrite
    protected void dropExperience(net.minecraft.server.level.ServerLevel level, @Nullable Entity killer) {
        if (!(((Object) this) instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) {
            net.minecraft.world.entity.ExperienceOrb.award(level, this.position(), this.silkard$expToDropField);
            this.silkard$expToDropField = 0;
        }
    }

    @Override
    public void heal(float amount, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason regainReason) {
        float current = this.getHealth();
        if (current > 0.0F) {
            org.bukkit.event.entity.EntityRegainHealthEvent event = new org.bukkit.event.entity.EntityRegainHealthEvent(this.getBukkitEntity(), amount, regainReason);
            if (((ContextEntity) (Object) this).silkard_valid()) {
                ((Entity) (Object) this).level().getCraftServer().getPluginManager().callEvent(event);
            }
            if (!event.isCancelled()) {
                this.setHealth((float) (this.getHealth() + event.getAmount()));
            }
        }
    }

    @Override
    public void setArrowCount(int count, boolean reset) {
        org.bukkit.event.entity.ArrowBodyCountChangeEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callArrowBodyCountChangeEvent((LivingEntity) (Object) this, this.getArrowCount(), count, reset);
        if (event.isCancelled()) {
            return;
        }
        ((LivingEntity) (Object) this).getEntityData().set(LivingEntity.DATA_ARROW_COUNT_ID, event.getNewAmount());
    }

    @Override
    public boolean removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        return ((LivingEntity) (Object) this).removeAllEffects();
    }

    @Override
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        return this.addEffect(effect, null, cause);
    }

    @Override
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, @Nullable Entity source, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        org.bukkit.event.entity.EntityPotionEffectEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effect, null, cause);
        if (event.isCancelled()) {
            return false;
        }
        return ((LivingEntity) (Object) this).addEffect(effect, source);
    }

    @Override
    public boolean removeEffect(Holder<net.minecraft.world.effect.MobEffect> effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        org.bukkit.event.entity.EntityPotionEffectEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, ((LivingEntity) (Object) this).getEffect(effect), null, cause);
        if (event.isCancelled()) {
            return false;
        }
        return ((LivingEntity) (Object) this).removeEffect(effect);
    }

    @Override
    public net.minecraft.world.effect.MobEffectInstance removeEffectNoUpdate(Holder<net.minecraft.world.effect.MobEffect> effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        org.bukkit.event.entity.EntityPotionEffectEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, ((LivingEntity) (Object) this).getEffect(effect), null, cause);
        if (event.isCancelled()) {
            return null;
        }
        return ((LivingEntity) (Object) this).removeEffectNoUpdate(effect);
    }

    @Override
    public void onEquipItem(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack oldStack, net.minecraft.world.item.ItemStack stack, boolean silent) {
        ((LivingEntity) (Object) this).onEquipItem(slot, oldStack, stack);
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack itemStack, boolean silent) {
        this.onEquipItem(slot, this.equipment.set(slot, itemStack), itemStack, silent);
    }

    @Shadow @Final private net.minecraft.world.entity.EntityEquipment equipment;

    public void detectEquipmentUpdatesPublic() {
        this.detectEquipmentUpdates();
    }

    @Shadow
    private void detectEquipmentUpdates() { throw new AssertionError(); }

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    public abstract int getArrowCount();

    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    @Shadow
    public abstract boolean wasExperienceConsumed();

    @Shadow
    public abstract boolean isAlwaysExperienceDropper();

    @Shadow
    public abstract boolean shouldDropExperience();

    @Shadow
    protected abstract net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source);

    @Shadow
    protected abstract net.minecraft.sounds.SoundEvent getDeathSound();

    @Shadow
    private net.minecraft.sounds.SoundEvent getFallDamageSound(int fallHeight) { throw new AssertionError(); }

    @Override
    public net.minecraft.sounds.SoundEvent getHurtSound0(net.minecraft.world.damagesource.DamageSource source) {
        return this.getHurtSound(source);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getDeathSound0() {
        return this.getDeathSound();
    }

    @Override
    public net.minecraft.sounds.SoundEvent getFallDamageSound0(int fallHeight) {
        return this.getFallDamageSound(fallHeight);
    }

    @Override
    public boolean canCollideWithBukkit(Entity entity) {
        return this.isPushable() && this.collides != this.collidableExemptions.contains(entity.getUUID());
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - respect the collides flag
     */
    @Overwrite
    public boolean isPickable() {
        return !this.isRemoved() && this.collides;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - respect the collides flag
     */
    @Overwrite
    public boolean isPushable() {
        return ((LivingEntity) (Object) this).isAlive() && !((LivingEntity) (Object) this).isSpectator() && !((LivingEntity) (Object) this).onClimbable() && this.collides;
    }

    @Inject(method = "stopFallFlying", at = @At("HEAD"), cancellable = true)
    private void silkard$toggleGlideOff(CallbackInfo ci) {
        if (org.bukkit.craftbukkit.event.CraftEventFactory.callToggleGlideEvent((LivingEntity) (Object) this, false).isCancelled()) {
            ci.cancel();
        }
    }
    // CraftBukkit end
}
