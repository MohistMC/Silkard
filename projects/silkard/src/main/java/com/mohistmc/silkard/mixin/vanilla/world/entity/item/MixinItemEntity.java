package com.mohistmc.silkard.mixin.vanilla.world.entity.item;

import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.player.ContextInventory;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    @Unique
    private int silkard$lastTick;
    @Unique
    private int silkard$elapsedTicks;
    @Unique
    private int silkard$ageBefore;
    @Unique
    private int silkard$pickupDelayBefore;

    // CraftBukkit start - use wall time for pickup and despawn timers
    @Inject(method = "tick", at = @At("HEAD"))
    private void silkard$wallTimeStart(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        int current = this.silkard$currentTick();
        if (this.silkard$lastTick == 0) {
            this.silkard$lastTick = current - 1;
        }
        this.silkard$elapsedTicks = current - this.silkard$lastTick;
        this.silkard$lastTick = current;
        this.silkard$ageBefore = self.age;
        this.silkard$pickupDelayBefore = self.pickupDelay;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void silkard$wallTimeEnd(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (this.silkard$pickupDelayBefore > 0 && this.silkard$pickupDelayBefore != 32767) {
            self.pickupDelay = this.silkard$pickupDelayBefore - this.silkard$elapsedTicks;
        }
        if (this.silkard$ageBefore != -32768) {
            self.age = this.silkard$ageBefore + this.silkard$elapsedTicks;
        }
    }
    // CraftBukkit end

    // CraftBukkit start - add Bukkit remove cause
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void silkard$discardTick(ItemEntity self) {
        ItemEntity entity = (ItemEntity) (Object) this;
        if (!entity.getItem().isEmpty()) {
            // CraftBukkit start - fire ItemDespawnEvent
            if (CraftEventFactory.callItemDespawnEvent(entity).isCancelled()) {
                entity.age = 0;
                return;
            }
            // CraftBukkit end
        }
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void silkard$discardLoad(ItemEntity self) {
        ((ContextEntity) (Object) this).discard(null);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void silkard$discardDeath(ItemEntity self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DEATH);
    }

    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void silkard$discardPickup(ItemEntity self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.PICKUP);
    }
    // CraftBukkit end

    // CraftBukkit start - fire ItemMergeEvent
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private static void silkard$itemMerge(ItemEntity toItem, ItemStack toStack, ItemEntity fromItem, ItemStack fromStack, CallbackInfo ci) {
        if (!CraftEventFactory.callItemMergeEvent(fromItem, toItem)) {
            ci.cancel();
        }
    }

    @Redirect(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private static void silkard$discardMerge(ItemEntity self) {
        ((ContextEntity) (Object) self).discard(EntityRemoveEvent.Cause.MERGE);
    }
    // CraftBukkit end

    // CraftBukkit start - fire EntityDamageEvent for non living entities
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;markHurt()V"), cancellable = true)
    private void silkard$damageEvent(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemEntity) (Object) this, source, damage)) {
            cir.setReturnValue(false);
        }
    }
    // CraftBukkit end

    // CraftBukkit start - fire PlayerPickupItemEvent
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void silkard$playerPickupItemEvent(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = ((Entity) (Object) this).level();
        if (level.isClientSide()) {
            return;
        }

        ItemStack itemstack = self.getItem();
        int count = itemstack.getCount();
        int canHold = ((ContextInventory) player.getInventory()).canHold(itemstack);
        int remaining = count - canHold;

        if (self.pickupDelay <= 0 && canHold > 0) {
            itemstack.setCount(canHold);

            PlayerPickupItemEvent playerEvent = new PlayerPickupItemEvent((org.bukkit.entity.Player) ((ContextEntity) (Object) player).getBukkitEntity(),
                    (org.bukkit.entity.Item) ((ContextEntity) (Object) this).getBukkitEntity(), remaining);
            playerEvent.setCancelled(!playerEvent.getPlayer().getCanPickupItems());
            ((ContextLevel) (Object) level).getCraftServer().getPluginManager().callEvent(playerEvent);
            if (playerEvent.isCancelled()) {
                itemstack.setCount(count);
                ci.cancel();
                return;
            }

            EntityPickupItemEvent entityEvent = new EntityPickupItemEvent((org.bukkit.entity.Player) ((ContextEntity) (Object) player).getBukkitEntity(),
                    (org.bukkit.entity.Item) ((ContextEntity) (Object) this).getBukkitEntity(), remaining);
            entityEvent.setCancelled(!entityEvent.getEntity().getCanPickupItems());
            ((ContextLevel) (Object) level).getCraftServer().getPluginManager().callEvent(entityEvent);
            if (entityEvent.isCancelled()) {
                itemstack.setCount(count);
                ci.cancel();
                return;
            }

            ItemStack current = self.getItem();
            if (!itemstack.equals(current)) {
                self.setItem(current);
            } else {
                itemstack.setCount(canHold + remaining);
            }

            self.pickupDelay = 0;
        } else if (self.pickupDelay == 0) {
            self.pickupDelay = -1;
        }
    }
    // CraftBukkit end

    private int silkard$currentTick() {
        Level level = ((Entity) (Object) this).level();
        if (level == null) {
            return 0;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return 0;
        }
        return ((ContextMinecraftServer) (Object) server).silkard$currentTick();
    }
}
