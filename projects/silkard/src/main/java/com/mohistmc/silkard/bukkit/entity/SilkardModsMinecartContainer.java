package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecartContainer;

public class SilkardModsMinecartContainer extends CraftMinecartContainer {

    public SilkardModsMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsMinecartContainer{" + getType() + '}';
    }
}
