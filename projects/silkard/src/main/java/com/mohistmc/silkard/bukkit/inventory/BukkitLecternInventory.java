package com.mohistmc.silkard.bukkit.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bukkit.Location;
import org.bukkit.block.Lectern;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author Mgazul
 * @date 2026/5/13 20:53
 */
public class BukkitLecternInventory implements Container {

    public LecternBlockEntity lecternBlockEntity;

    public BukkitLecternInventory(LecternBlockEntity lecternBlockEntity) {
        this.lecternBlockEntity = lecternBlockEntity;
    }

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = 1;

    @Override
    public List<ItemStack> getContents() {
        return Arrays.asList(lecternBlockEntity.getBook());
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public void setMaxStackSize(int i) {
        maxStack = i;
    }

    @Override
    public Location getLocation() {
        if (lecternBlockEntity.getLevel() == null) return null;
        return CraftLocation.toBukkit(lecternBlockEntity.getBlockPos(), lecternBlockEntity.getLevel().getWorld());
    }

    @Override
    public InventoryHolder getOwner() {
        return (Lectern) lecternBlockEntity.silkard_getOwner();
    }

    public LecternBlockEntity getLectern() {
        return lecternBlockEntity;
    }
    // CraftBukkit end
    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return lecternBlockEntity.getBook().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? lecternBlockEntity.getBook() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot == 0) {
            ItemStack result = lecternBlockEntity.getBook().split(count);
            if (lecternBlockEntity.getBook().isEmpty()) {
                lecternBlockEntity.onBookItemRemove();
            }

            return result;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) {
            ItemStack prev = lecternBlockEntity.getBook();
            lecternBlockEntity.setBook(ItemStack.EMPTY);
            lecternBlockEntity.onBookItemRemove();
            return prev;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (slot == 0) {
            this.lecternBlockEntity.setBook(itemStack);
            if (this.lecternBlockEntity.getLevel() != null) {
                LecternBlock.resetBookState(null, this.lecternBlockEntity.getLevel(), this.lecternBlockEntity.getBlockPos(), this.lecternBlockEntity.getBlockState(), this.lecternBlockEntity.hasBook());
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    @Override
    public void setChanged() {
        lecternBlockEntity.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(lecternBlockEntity, player) && lecternBlockEntity.hasBook();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return false;
    }

    @Override
    public void clearContent() {
    }
}
