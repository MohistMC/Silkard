package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerExpCooldownChangeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(ExperienceOrb.class)
public abstract class MixinExperienceOrb {

    @Shadow
    private Player followingPlayer;

    // CraftBukkit start - split into components
    @Overwrite
    private void followNearbyPlayer() {
        Entity self = (Entity) (Object) this;
        Player prevTarget = this.followingPlayer;
        if (this.followingPlayer == null || this.followingPlayer.isSpectator() || this.followingPlayer.distanceToSqr(self) > 64.0) {
            Player nearestPlayer = self.level().getNearestPlayer(self, 8.0);
            if (nearestPlayer != null && !nearestPlayer.isSpectator() && !nearestPlayer.isDeadOrDying()) {
                this.followingPlayer = nearestPlayer;
            } else {
                this.followingPlayer = null;
            }
        }

        boolean cancelled = false;
        if (this.followingPlayer != prevTarget) {
            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent((ExperienceOrb) (Object) this, this.followingPlayer,
                    (this.followingPlayer != null) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.FORGOT_TARGET);
            LivingEntity target = (event.getTarget() == null) ? null : ((CraftLivingEntity) event.getTarget()).getHandle();
            cancelled = event.isCancelled();

            if (cancelled) {
                this.followingPlayer = prevTarget;
            } else {
                this.followingPlayer = (target instanceof Player) ? (Player) target : null;
            }
        }

        if (this.followingPlayer != null && !cancelled) {
            Vec3 delta = new Vec3(
                    this.followingPlayer.getX() - self.getX(),
                    this.followingPlayer.getY() + (double) this.followingPlayer.getEyeHeight() / 2.0 - self.getY(),
                    this.followingPlayer.getZ() - self.getZ()
            );
            double length = delta.lengthSqr();
            double power = 1.0 - Math.sqrt(length) / 8.0;
            self.setDeltaMovement(self.getDeltaMovement().add(delta.normalize().scale(power * power * 0.1)));
        }
    }
    // CraftBukkit end

    // CraftBukkit start - PlayerItemMendEvent
    @Overwrite
    private int repairPlayerItems(final ServerPlayer player, final int amount) {
        Optional<EnchantedItemInUse> selected = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        if (selected.isPresent()) {
            ItemStack itemStack = selected.get().itemStack();
            int toRepairFromXpAmount = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), itemStack, amount);
            int repair = Math.min(toRepairFromXpAmount, itemStack.getDamageValue());

            PlayerItemMendEvent event = CraftEventFactory.callPlayerItemMendEvent(player,
                    (ExperienceOrb) (Object) this, itemStack, selected.get().inSlot(), repair);
            repair = event.getRepairAmount();
            if (event.isCancelled()) {
                return amount;
            }

            itemStack.setDamageValue(itemStack.getDamageValue() - repair);
            if (repair > 0) {
                int remaining = amount - repair * amount / toRepairFromXpAmount;
                if (remaining > 0) {
                    ((ExperienceOrb) (Object) this).setValue(remaining);
                    return this.repairPlayerItems(player, remaining);
                }
            }

            return 0;
        } else {
            return amount;
        }
    }
    // CraftBukkit end

    // CraftBukkit start - add Bukkit remove cause
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void silkard$discardTick(ExperienceOrb self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "merge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void silkard$discardMerge(ExperienceOrb self) {
        ((ContextEntity) (Object) self).discard(EntityRemoveEvent.Cause.MERGE);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void silkard$discardDeath(ExperienceOrb self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DEATH);
    }

    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void silkard$discardPickup(ExperienceOrb self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.PICKUP);
    }
    // CraftBukkit end

    // CraftBukkit start - xp cooldown and amount events
    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"))
    private void silkard$xpCooldown(Player player, CallbackInfo ci) {
        player.takeXpDelay = CraftEventFactory.callPlayerXpCooldownEvent((ServerPlayer) player, 2, PlayerExpCooldownChangeEvent.ChangeReason.PICKUP_ORB).getNewCooldown();
    }

    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void silkard$expChange(Player player, int amount) {
        player.giveExperiencePoints(CraftEventFactory.callPlayerExpChangeEvent((ServerPlayer) player, amount).getAmount());
    }
    // CraftBukkit end

    // CraftBukkit start - large xp amounts
    @Overwrite
    public static int getExperienceValue(final int maxValue) {
        if (maxValue > 162670129) return maxValue - 100000;
        if (maxValue > 81335063) return 81335063;
        if (maxValue > 40667527) return 40667527;
        if (maxValue > 20333759) return 20333759;
        if (maxValue > 10166857) return 10166857;
        if (maxValue > 5083423) return 5083423;
        if (maxValue > 2541701) return 2541701;
        if (maxValue > 1270849) return 1270849;
        if (maxValue > 635413) return 635413;
        if (maxValue > 317701) return 317701;
        if (maxValue > 158849) return 158849;
        if (maxValue > 79423) return 79423;
        if (maxValue > 39709) return 39709;
        if (maxValue > 19853) return 19853;
        if (maxValue > 9923) return 9923;
        if (maxValue > 4957) return 4957;

        return maxValue >= 2477 ? 2477 : (maxValue >= 1237 ? 1237 : (maxValue >= 617 ? 617 : (maxValue >= 307 ? 307 : (maxValue >= 149 ? 149 : (maxValue >= 73 ? 73 : (maxValue >= 37 ? 37 : (maxValue >= 17 ? 17 : (maxValue >= 7 ? 7 : (maxValue >= 3 ? 3 : 1)))))))));
    }
    // CraftBukkit end
}
