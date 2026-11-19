package com.mohistmc.silkard.mixin.vanilla.world.inventory;

import com.mohistmc.silkard.injected.world.inventory.ContextAnvilMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import org.bukkit.craftbukkit.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.inventory.view.CraftAnvilView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(AnvilMenu.class)
public abstract class MixinAnvilMenu extends ItemCombinerMenu implements ContextAnvilMenu {

    // CraftBukkit start
    @Unique
    private static final int DEFAULT_DENIED_COST = -1;

    @Unique
    public int maximumRepairCost = 40;

    @Unique
    private CraftAnvilView bukkitEntity;

    protected MixinAnvilMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slotDefinition) {
        super(menuType, containerId, inventory, access, slotDefinition);
    }

    @Override
    public CraftAnvilView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        CraftInventoryAnvil inventory = new CraftInventoryAnvil(this.access.getLocation(), this.inputSlots, this.resultSlots);
        this.bukkitEntity = new CraftAnvilView(this.player.getBukkitEntity(), inventory, (AnvilMenu) (Object) this);
        this.bukkitEntity.updateFromLegacy(inventory);
        return this.bukkitEntity;
    }
    // CraftBukkit end

    @Override
    public int silkard$getDeniedCost() {
        return DEFAULT_DENIED_COST;
    }

    @Override
    public int silkard$maximumRepairCost() {
        return maximumRepairCost;
    }

    @Override
    public void silkard$maximumRepairCost(int maximumRepairCost) {
        this.maximumRepairCost = maximumRepairCost;
    }

    @Shadow
    private net.minecraft.world.inventory.DataSlot cost;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void silkard$mayPickup(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue((player.hasInfiniteMaterials() || player.experienceLevel >= this.cost.get()) && this.cost.get() > DEFAULT_DENIED_COST && hasItem);
    }

    @Redirect(method = {"createResult", "onTake"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V"))
    private void silkard$setCost(net.minecraft.world.inventory.DataSlot dataSlot, int value) {
        dataSlot.set(value == 0 ? DEFAULT_DENIED_COST : value);
    }

    @Redirect(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void silkard$setItem(net.minecraft.world.inventory.ResultContainer resultContainer, int slot, net.minecraft.world.item.ItemStack itemStack) {
        org.bukkit.craftbukkit.event.CraftEventFactory.callPrepareAnvilEvent((org.bukkit.inventory.view.AnvilView) ((AnvilMenu) (Object) this).getBukkitView(), itemStack);
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int silkard$modifyMaxRepairCost(int original) {
        return maximumRepairCost;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int silkard$modifyMaxRepairCostMinusOne(int original) {
        return maximumRepairCost - 1;
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void silkard$createResult$return(CallbackInfo ci) {
        // CraftBukkit - SPIGOT-6686, SPIGOT-7931: Always send completed inventory to stay in sync with client
        ((AbstractContainerMenu) (Object) this).sendAllDataToRemote();
    }
}
