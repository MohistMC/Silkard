package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftBoat;

public class SilkardModsBoat extends CraftBoat {

    public SilkardModsBoat(CraftServer server, AbstractBoat entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsBoat{" + getType() + '}';
    }
}
