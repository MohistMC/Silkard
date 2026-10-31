package com.mohistmc.silkard.mixin.vanilla.world.entity.projectile.arrow;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(AbstractArrow.class)
public abstract class MixinAbstractArrow {

    @Shadow
    protected abstract boolean tryPickup(Player player);

    @Shadow
    protected abstract ItemStack getPickupItem();

    // CraftBukkit - add Bukkit remove cause
    @Redirect(method = "tickDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;discard()V"))
    private void silkard$discardDespawn(AbstractArrow self) {
        self.discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    // CraftBukkit - add Bukkit remove cause
    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;discard()V"))
    private void silkard$discardHit(AbstractArrow self) {
        self.discard(EntityRemoveEvent.Cause.HIT);
    }

    // CraftBukkit - EntityCombustByEntityEvent
    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void silkard$combust(Entity entity, float duration) {
        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(((Entity) (Object) this).getBukkitEntity(), entity.getBukkitEntity(), duration);
        entity.level().getCraftServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            entity.igniteForSeconds(event.getDuration());
        }
    }

    // CraftBukkit - PlayerPickupArrowEvent
    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;tryPickup(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean silkard$pickup(AbstractArrow self, Player player) {
        ItemStack itemstack = this.getPickupItem();
        if (self.pickup == AbstractArrow.Pickup.ALLOWED && !itemstack.isEmpty() && player.getInventory().getFreeSlot() > 0) {
            ItemEntity item = new ItemEntity(self.level(), self.getX(), self.getY(), self.getZ(), itemstack);
            PlayerPickupArrowEvent event = new PlayerPickupArrowEvent((org.bukkit.entity.Player) player.getBukkitEntity(), new CraftItem(self.level().getCraftServer(), item), (org.bukkit.entity.AbstractArrow) self.getBukkitEntity());
            self.level().getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
        }
        return this.tryPickup(player);
    }
}
