package com.mohistmc.silkard.mixin.vanilla.world.entity.decoration;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import com.mohistmc.silkard.injected.world.entity.decoration.ContextArmorStand;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
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
@Mixin(ArmorStand.class)
public class MixinArmorStand implements ContextArmorStand {

    @Shadow
    private int disabledSlots;

    @Shadow
    private boolean invisible;

    @Shadow
    private void brokenByAnything(ServerLevel level, DamageSource source) {
        throw new AssertionError();
    }

    @Unique
    public boolean canMove = true;

    @Override
    public boolean silkard$canMove() {
        return canMove;
    }

    @Override
    public void silkard$setCanMove(boolean canMove) {
        this.canMove = canMove;
    }

    // CraftBukkit - PlayerArmorStandManipulateEvent
    @Inject(method = "swapItem", at = @At("HEAD"), cancellable = true)
    private void silkard$manipulate(Player player, EquipmentSlot slot, ItemStack playerItemStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        ItemStack armorStandItemStack = ((LivingEntity) (Object) this).getItemBySlot(slot);
        if (!armorStandItemStack.isEmpty() && (this.disabledSlots & 1 << slot.getFilterBit(8)) != 0) {
            return;
        }
        if (armorStandItemStack.isEmpty() && (this.disabledSlots & 1 << slot.getFilterBit(16)) != 0) {
            return;
        }

        PlayerArmorStandManipulateEvent event = new PlayerArmorStandManipulateEvent(
                (org.bukkit.entity.Player) player.getBukkitEntity(),
                (org.bukkit.entity.ArmorStand) ((Entity) (Object) this).getBukkitEntity(),
                CraftItemStack.asCraftMirror(playerItemStack),
                CraftItemStack.asCraftMirror(armorStandItemStack),
                CraftEquipmentSlot.getSlot(slot),
                CraftEquipmentSlot.getHand(hand));
        ((ContextLevel) ((Entity) (Object) this).level()).getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    // CraftBukkit - non-living damage event
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void silkard$damage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((Entity) (Object) this, source, damage, true, this.invisible)) {
            cir.setReturnValue(false);
        }
    }

    // CraftBukkit - add the armor stand item to the death event drops
    @Overwrite
    private void brokenByPlayer(ServerLevel level, DamageSource source) {
        net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARMOR_STAND);
        itemStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, ((LivingEntity) (Object) this).getCustomName());
        ((ContextLivingEntity) (Object) this).silkard$drops().add(CraftItemStack.asBukkitCopy(itemStack));
        this.brokenByAnything(level, source);
    }

    // CraftBukkit - EntityDeathEvent + death remove cause
    @Inject(method = "kill", at = @At("HEAD"))
    private void silkard$deathEvent(ServerLevel level, Entity killer, CallbackInfo ci) {
        CraftEventFactory.callEntityDeathEvent((LivingEntity) (Object) this, ((Entity) (Object) this).damageSources().genericKill(), ((ContextLivingEntity) (Object) this).silkard$drops());
    }

    @Redirect(method = "kill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void silkard$removeCause(ArmorStand self, Entity.RemovalReason reason) {
        ((ContextEntity) self).remove(reason, EntityRemoveEvent.Cause.DEATH);
    }

    // CraftBukkit - MC-157395, SPIGOT-5193
    public boolean shouldDropExperience() {
        return true;
    }
}
