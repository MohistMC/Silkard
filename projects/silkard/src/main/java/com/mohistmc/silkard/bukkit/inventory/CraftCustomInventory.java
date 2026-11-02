package com.mohistmc.silkard.bukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class CraftCustomInventory implements InventoryHolder {

    private final CraftInventory container;

    public CraftCustomInventory(Container inventory) {
        this.container = new CraftInventory(inventory);
    }

    public static List<HumanEntity> getViewers(Container inventory) {
        try {
            return inventory.getViewers();
        } catch (AbstractMethodError e) {
            return new ArrayList<>();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.container;
    }
}
