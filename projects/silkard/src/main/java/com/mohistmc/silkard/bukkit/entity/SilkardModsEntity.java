package com.mohistmc.silkard.bukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;

public class SilkardModsEntity extends CraftEntity {

    public SilkardModsEntity(CraftServer server, net.minecraft.world.entity.Entity entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsEntity{" + this.getType() + '}';
    }
}
