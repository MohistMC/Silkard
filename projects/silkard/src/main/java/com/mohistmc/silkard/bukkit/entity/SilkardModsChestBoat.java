package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftChestBoat;

public class SilkardModsChestBoat extends CraftChestBoat {

    public SilkardModsChestBoat(CraftServer server, AbstractChestBoat entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsChestBoat{" + getType() + '}';
    }
}
