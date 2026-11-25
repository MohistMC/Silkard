package com.mohistmc.silkard.mixin.vanilla.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// craftSlots / resultSlots 由 AbstractCraftingMenu 声明（protected），@Shadow 命中不了
// 继承成员，改为直接 extends AbstractCraftingMenu 取值。
@Mixin(CraftingMenu.class)
public abstract class MixinCraftingMenu extends AbstractCraftingMenu {

    @Shadow @Final private Player player;

    // CraftBukkit start
    @Unique
    private CraftInventoryView bukkitEntity = null;

    protected MixinCraftingMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId, 0, 0);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        bukkitEntity = new CraftInventoryView(this.player.getBukkitEntity(), inventory, (CraftingMenu) (Object) this);
        return bukkitEntity;
    }
    // CraftBukkit end

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void silkard$initOwner(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        ((com.mohistmc.silkard.injected.world.inventory.ContextAbstractContainerMenu) (Object) this).containerOwner(this.player);
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    public void silkard$stillValid(net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!silkard$checkReachable()) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static void silkard$slotChangedCraftingGrid$setItem(ResultContainer resultSlots, int slot, ItemStack itemstack, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) CraftingContainer craftSlots) {
        // CraftBukkit - callPreCraftEvent
        itemstack = org.bukkit.craftbukkit.event.CraftEventFactory.callPreCraftEvent(craftSlots, resultSlots, itemstack, null, java.util.Optional.empty());
        resultSlots.setItem(slot, itemstack);
    }
}
