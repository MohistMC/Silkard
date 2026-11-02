package com.mohistmc.silkard.bukkit.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Resolves Bukkit InventoryHolder / Inventory from NMS containers.
 */
public class InventoryOwner {

    public static Inventory getInventory(Container inventory) {
        InventoryHolder owner = get(inventory);
        return (owner == null) ? new CraftCustomInventory(inventory).getInventory() : owner.getInventory();
    }

    public static InventoryHolder get(BlockEntity te) {
        return get(te.getLevel(), te.getBlockPos());
    }

    public static InventoryHolder get(Container inventory) {
        try {
            InventoryHolder owner = inventory.getOwner();
            if (owner != null) return owner;
        } catch (RuntimeException ignored) {}
        if (inventory instanceof BlockEntity blockEntity) {
            return get(blockEntity);
        }
        return null;
    }

    public static InventoryHolder get(Level world, BlockPos pos) {
        if (world == null) return null;
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof Container container) {
            try {
                InventoryHolder owner = container.getOwner();
                if (owner != null) return owner;
            } catch (RuntimeException ignored) {}
            return new CraftCustomInventory(container);
        }
        org.bukkit.block.BlockState state = CraftBlock.at(world, pos).getState();
        if (state instanceof InventoryHolder holder) return holder;
        return null;
    }
}
