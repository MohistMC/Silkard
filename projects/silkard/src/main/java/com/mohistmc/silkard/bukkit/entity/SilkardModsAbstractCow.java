package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.animal.cow.AbstractCow;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractCow;

public class SilkardModsAbstractCow extends CraftAbstractCow {

    public SilkardModsAbstractCow(CraftServer server, AbstractCow entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsAbstractCow{" + getType() + '}';
    }
}
