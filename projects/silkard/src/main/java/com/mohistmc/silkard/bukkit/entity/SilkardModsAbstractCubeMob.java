package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractCubeMob;

public class SilkardModsAbstractCubeMob extends CraftAbstractCubeMob {

    public SilkardModsAbstractCubeMob(CraftServer server, AbstractCubeMob entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsAbstractCubeMob{" + getType() + '}';
    }
}
