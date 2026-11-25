package com.mohistmc.silkard.injected.world;

import com.mohistmc.silkard.bukkit.inventory.InventoryOwner;
import com.mohistmc.silkard.util.ContextStateException;
import java.util.Collections;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryHolder;

public interface ContextContainer {

    int MAX_STACK = 99;

    default java.util.List<ItemStack> getContents() {
        if (this instanceof net.minecraft.world.Container container) {
            int size = container.getContainerSize();
            java.util.List<ItemStack> items = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                items.add(container.getItem(i));
            }
            return items;
        }
        return Collections.emptyList();
    }

    default void onOpen(CraftHumanEntity who) {
        throw new ContextStateException("Not implemented");
    }

    default void onClose(CraftHumanEntity who) {
        throw new ContextStateException("Not implemented");
    }

    default java.util.List<org.bukkit.entity.HumanEntity> getViewers() {
        return Collections.emptyList();
    }

    /**
     * Returns the Bukkit InventoryHolder for this Container.
     * For BlockEntity containers, delegates to {@link InventoryOwner}.
     * Fabric mod containers that have a real Bukkit owner can override directly.
     */
    default InventoryHolder getOwner() {
        if (this instanceof BlockEntity blockEntity) {
            try {
                return InventoryOwner.get(blockEntity);
            } catch (RuntimeException ignored) {
            }
        }
        throw new ContextStateException("Not implemented");
    }

    default void setOwner(InventoryHolder owner) {
        throw new ContextStateException("Not implemented");
    }

    default void setMaxStackSize(int size) {
        throw new ContextStateException("Not implemented");
    }

    default org.bukkit.Location getLocation() {
        throw new ContextStateException("Not implemented");
    }
}
