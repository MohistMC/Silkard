package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mohistmc.silkard.injected.world.ContextContainer;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(HopperBlockEntity.class)
public abstract class MixinHopperBlockEntity extends RandomizableContainerBlockEntity {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    @Unique public List<HumanEntity> transaction = new ArrayList<>();
    @Unique private int maxStack = ContextContainer.MAX_STACK;
    // @formatter:on

    protected MixinHopperBlockEntity() {
        super(null, null, null);
    }

    // CraftBukkit start - container accessors
    public List<ItemStack> getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
    // CraftBukkit end

    // CraftBukkit - InventoryMoveItemEvent when pushing items out
    @WrapOperation(method = "ejectItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack silkard$push(Container self, Container container, ItemStack stack, Direction direction, Operation<ItemStack> original) {
        Inventory destination;
        if (container instanceof CompoundContainer compound) {
            destination = new CraftInventoryDoubleChest(compound);
        } else if (((ContextContainer) container).getOwner() != null) {
            destination = ((ContextContainer) container).getOwner().getInventory();
        } else {
            destination = new CraftInventory(container);
        }

        Inventory source = ((ContextContainer) self).getOwner() != null ? ((ContextContainer) self).getOwner().getInventory() : new CraftInventory(self);
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(source, CraftItemStack.asCraftMirror(stack), destination, true);
        ((ContextLevel) ((HopperBlockEntity) self).getLevel()).getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return stack;
        }
        return original.call(self, container, CraftItemStack.asNMSCopy(event.getItem()), direction);
    }

    // CraftBukkit - InventoryMoveItemEvent when pulling items in
    @WrapOperation(method = "tryTakeInItemFromSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack silkard$pull(Container container, Container hopper, ItemStack stack, Direction direction, Operation<ItemStack> original) {
        Inventory source;
        if (container instanceof CompoundContainer compound) {
            source = new CraftInventoryDoubleChest(compound);
        } else if (((ContextContainer) container).getOwner() != null) {
            source = ((ContextContainer) container).getOwner().getInventory();
        } else {
            source = new CraftInventory(container);
        }

        Inventory destination = ((ContextContainer) hopper).getOwner() != null ? ((ContextContainer) hopper).getOwner().getInventory() : new CraftInventory(hopper);
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(source, CraftItemStack.asCraftMirror(stack), destination, false);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return stack;
        }
        return original.call(container, hopper, CraftItemStack.asNMSCopy(event.getItem()), direction);
    }

    // CraftBukkit - InventoryPickupItemEvent
    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void silkard$pickupItem(Container container, ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (((ContextContainer) container).getOwner() == null) {
            return;
        }
        InventoryPickupItemEvent event = new InventoryPickupItemEvent(((ContextContainer) container).getOwner().getInventory(), (org.bukkit.entity.Item) entity.getBukkitEntity());
        ((ContextLevel) entity.level()).getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    // CraftBukkit - add Bukkit remove cause
    @Redirect(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private static void silkard$discardPickedUp(ItemEntity entity) {
        entity.discard(EntityRemoveEvent.Cause.PICKUP);
    }

    // CraftBukkit - HopperInventorySearchEvent for the destination container
    @WrapOperation(method = "getAttachedContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/Container;"))
    private static Container silkard$attachedSearch(Level level, BlockPos searchPosition, Operation<Container> original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) BlockPos hopperPos) {
        Container inventory = original.call(level, searchPosition);
        CraftInventory craftInventory = null;
        if (inventory != null) {
            craftInventory = new CraftInventory(inventory);
        }
        HopperInventorySearchEvent event = new HopperInventorySearchEvent(craftInventory, HopperInventorySearchEvent.ContainerType.DESTINATION, CraftBlock.at(level, hopperPos), CraftBlock.at(level, searchPosition));
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.getInventory() == null ? null : ((CraftInventory) event.getInventory()).getInventory();
    }

    // CraftBukkit - HopperInventorySearchEvent for the source container
    @WrapOperation(method = "getSourceContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;DDD)Lnet/minecraft/world/Container;"))
    private static Container silkard$sourceSearch(Level level, BlockPos pos, BlockState state, double x, double y, double z, Operation<Container> original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.level.block.entity.Hopper hopper) {
        Container inventory = original.call(level, pos, state, x, y, z);
        BlockPos hopperPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        CraftInventory craftInventory = null;
        if (inventory != null) {
            craftInventory = new CraftInventory(inventory);
        }
        HopperInventorySearchEvent event = new HopperInventorySearchEvent(craftInventory, HopperInventorySearchEvent.ContainerType.SOURCE, CraftBlock.at(level, hopperPos), CraftBlock.at(level, hopperPos.above()));
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.getInventory() == null ? null : ((CraftInventory) event.getInventory()).getInventory();
    }
}
