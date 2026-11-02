package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecart;


public class SilkardModsMinecart extends CraftMinecart {

    public SilkardModsMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsMinecart{" + getType() + '}';
    }
}
