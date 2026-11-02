package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractArrow;

public class SilkardModsAbstractArrow extends CraftAbstractArrow {

    public SilkardModsAbstractArrow(CraftServer server, AbstractArrow entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsAbstractArrow{" + getType() + '}';
    }
}
