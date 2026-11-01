package com.mohistmc.silkard.mixin.vanilla.world.entity.player;

import com.mohistmc.silkard.injected.world.entity.player.ContextPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
@Mixin(Player.class)
public abstract class MixinPlayer extends Avatar implements ContextPlayer {

    // CraftBukkit start
    @Unique
    public boolean fauxSleeping;

    @Unique
    public int oldLevel = -1;
    // CraftBukkit end

    @Unique private CraftEntity silkard$playerBukkitEntity;

    // CraftBukkit - attack context, captured for the deflected projectile damage event
    @Unique private DamageSource silkard$attackDamageSource;
    @Unique private float silkard$attackMagicBoost;
    @Unique private EquipmentSlot silkard$attackSlot;

    @Shadow
    protected abstract boolean wantsToStopRiding();

    @Shadow
    protected abstract void removeEntitiesOnShoulder();

    @Shadow
    protected abstract float getEnchantedDamage(Entity entity, float baseDamage, DamageSource damageSource);

    public MixinPlayer(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean silkard$affectsSpawning() {
        return true;
    }

    @Override
    public void silkard$setAffectsSpawning(boolean affectsSpawning) {
    }

    @Override
    public boolean silkard$fauxSleeping() {
        return fauxSleeping;
    }

    @Override
    public void silkard$fauxSleeping(boolean fauxSleeping) {
        this.fauxSleeping = fauxSleeping;
    }

    @Override
    public int silkard$oldLevel() {
        return oldLevel;
    }

    @Override
    public void silkard$setOldLevel(int oldLevel) {
        this.oldLevel = oldLevel;
    }

    @Override
    public void causeFoodExhaustion(float amount, EntityExhaustionEvent.ExhaustionReason reason) {
        if (((Player) (Object) this).getAbilities().invulnerable) {
            return;
        }
        if (this.level().isClientSide()) {
            return;
        }
        EntityExhaustionEvent event = CraftEventFactory.callPlayerExhaustionEvent((Player) (Object) this, reason, amount);
        if (!event.isCancelled()) {
            ((Player) (Object) this).getFoodData().addExhaustion(event.getExhaustion());
        }
    }

    // CraftBukkit - runtime NMS has no concrete getBukkitEntity() on Player (only the ContextPlayer stub); provide the real lazily-created entity.
    public CraftHumanEntity getBukkitEntity() {
        if (this.silkard$playerBukkitEntity == null) {
            this.silkard$playerBukkitEntity = CraftEntity.getEntity(((Entity) (Object) this).level().getCraftServer(), (Entity) (Object) this);
        }
        return (CraftHumanEntity) this.silkard$playerBukkitEntity;
    }

    // CraftBukkit start - EntityExhaustionEvent
    @Overwrite
    public void causeFoodExhaustion(float amount) {
        this.causeFoodExhaustion(amount, EntityExhaustionEvent.ExhaustionReason.UNKNOWN);
    }

    @Redirect(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void silkard$attackFoodExhaustion(Player player, float amount) {
        player.causeFoodExhaustion(amount, EntityExhaustionEvent.ExhaustionReason.ATTACK);
    }

    @Redirect(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void silkard$damagedFoodExhaustion(Player player, float amount) {
        player.causeFoodExhaustion(amount, EntityExhaustionEvent.ExhaustionReason.DAMAGED);
    }
    // CraftBukkit end

    // CraftBukkit start - resync the inventory when the attack did not land
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V", ordinal = 1))
    private void silkard$resyncInventory(CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayer serverPlayer) {
            ((CraftPlayer) serverPlayer.getBukkitEntity()).updateInventory();
        }
    }
    // CraftBukkit end

    // CraftBukkit start - damage event for deflected projectiles
    @Inject(method = "deflectProjectile(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void silkard$handleDeflectedProjectile(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (this.level().isClientSide() || this.silkard$attackDamageSource == null) {
            return;
        }
        if (!entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) || !(entity instanceof Projectile)) {
            return;
        }
        if (CraftEventFactory.handleNonLivingEntityDamageEvent(entity, this.silkard$attackDamageSource, this.silkard$attackMagicBoost, false)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float silkard$captureAttackDamage(Player player, Entity entity, float baseDamage, DamageSource damageSource) {
        float enchanted = this.getEnchantedDamage(entity, baseDamage, damageSource);
        this.silkard$attackDamageSource = damageSource;
        this.silkard$attackMagicBoost = player.getAttackStrengthScale(0.5F) * (enchanted - baseDamage);
        return enchanted;
    }

    @Redirect(method = "stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack silkard$captureAttackSlot(Player player, EquipmentSlot slot) {
        this.silkard$attackSlot = slot;
        return player.getItemBySlot(slot);
    }

    @Redirect(method = "stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float silkard$captureStabDamage(Player player, Entity entity, float baseDamage, DamageSource damageSource) {
        float enchanted = this.getEnchantedDamage(entity, baseDamage, damageSource);
        float boost = enchanted - baseDamage;
        boolean sameSlot = player.isUsingItem() && player.getUsedItemHand().asEquipmentSlot() == this.silkard$attackSlot;
        if (!sameSlot) {
            boost *= player.getAttackStrengthScale(0.5F);
        }
        this.silkard$attackDamageSource = damageSource;
        this.silkard$attackMagicBoost = boost;
        return enchanted;
    }
    // CraftBukkit end

    // CraftBukkit start - PlayerVelocityEvent
    @Overwrite
    public void causeExtraKnockback(Entity entity, float knockbackAmount, Vec3 oldMovement, DamageSource damageSource, float totalDamage, boolean flag) {
        if (knockbackAmount > 0.0F) {
            if (entity instanceof LivingEntity livingentity) {
                livingentity.knockback(knockbackAmount, Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), -Mth.cos(this.getYRot() * ((float) Math.PI / 180F)), damageSource, totalDamage, flag);
            } else {
                entity.push(-Mth.sin(this.getYRot() * ((float) Math.PI / 180F)) * knockbackAmount, 0.1D, Mth.cos(this.getYRot() * ((float) Math.PI / 180F)) * knockbackAmount);
            }
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            this.setSprinting(false);
        }

        if (entity instanceof ServerPlayer && entity.syncVelocity) {
            boolean cancelled = false;
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) entity.getBukkitEntity();
            org.bukkit.util.Vector velocity = CraftVector.toBukkit(oldMovement);

            PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
            this.level().getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                cancelled = true;
            } else if (!velocity.equals(event.getVelocity())) {
                player.setVelocity(event.getVelocity());
            }

            if (!cancelled) {
                ((ServerPlayer) entity).connection.send(new ClientboundSetEntityMotionPacket(entity));
                entity.syncVelocity = false;
                entity.setDeltaMovement(oldMovement);
            }
        }
    }
    // CraftBukkit end

    // CraftBukkit start - do not filter zero damage, move the shoulder entity removal after the damage
    @Overwrite
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (((Player) (Object) this).isInvulnerableTo(level, source)) {
            return false;
        }
        if (((Player) (Object) this).getAbilities().invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        this.noActionTime = 0;
        if (this.isDeadOrDying()) {
            return false;
        }
        if (source.scalesWithDifficulty()) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                return false;
            }
            if (level.getDifficulty() == Difficulty.EASY) {
                damage = Math.min(damage / 2.0F + 1.0F, damage);
            }
            if (level.getDifficulty() == Difficulty.HARD) {
                damage = damage * 3.0F / 2.0F;
            }
        }
        boolean damaged = super.hurtServer(level, source, damage);
        if (damaged) {
            this.removeEntitiesOnShoulder();
        }
        return damaged;
    }
    // CraftBukkit end

    // CraftBukkit start - SPIGOT-7316: no longer passenger, dismount and return
    @Overwrite
    public void rideTick() {
        if (!this.level().isClientSide() && this.wantsToStopRiding() && this.isPassenger()) {
            this.stopRiding();
            return;
        }
        if (!this.isPassenger()) {
            this.setShiftKeyDown(false);
            return;
        }
        super.rideTick();
    }
    // CraftBukkit end

    // CraftBukkit start - EntityToggleGlideEvent
    @Overwrite
    public void startFallFlying() {
        if (!this.level().isClientSide() && CraftEventFactory.callToggleGlideEvent(this, true).isCancelled()) {
            // SPIGOT-5542: must toggle like below
            this.setSharedFlag(7, true);
            this.setSharedFlag(7, false);
            return;
        }
        this.setSharedFlag(7, true);
    }
    // CraftBukkit end

    // CraftBukkit start - check the other player's scoreboard team through the Bukkit API
    @Overwrite
    public boolean canHarmPlayer(Player target) {
        org.bukkit.scoreboard.Team team;
        if (target instanceof ServerPlayer) {
            CraftPlayer thatPlayer = (CraftPlayer) ((ServerPlayer) (Object) target).getBukkitEntity();
            team = thatPlayer.getScoreboard().getPlayerTeam(thatPlayer);
            if (team == null || team.allowFriendlyFire()) {
                return true;
            }
        } else {
            OfflinePlayer thisPlayer = target.level().getCraftServer().getOfflinePlayer(target.getScoreboardName());
            team = target.level().getCraftServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(thisPlayer);
            if (team == null || team.allowFriendlyFire()) {
                return true;
            }
        }

        if (((Object) this) instanceof ServerPlayer) {
            return !team.hasPlayer(((ServerPlayer) (Object) this).getBukkitEntity());
        }
        return !team.hasPlayer(this.level().getCraftServer().getOfflinePlayer(((Player) (Object) this).getScoreboardName()));
    }
    // CraftBukkit end

    // CraftBukkit start - EntityPotionEffectEvent with the TURTLE_HELMET cause
    @Overwrite
    private void turtleHelmetTick() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true);
        if (this.level().isClientSide()) {
            this.addEffect(effect);
            return;
        }
        MobEffectInstance current = this.getEffect(MobEffects.WATER_BREATHING);
        boolean override = current != null && new MobEffectInstance(current).update(effect);
        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent(this, current, effect, EntityPotionEffectEvent.Cause.TURTLE_HELMET, override);
        if (!event.isCancelled()) {
            this.addEffect(effect);
        }
    }
    // CraftBukkit end
}
