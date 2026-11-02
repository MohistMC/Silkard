package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractVillager;

public class SilkardModsAbstractVillager extends CraftAbstractVillager {

    public SilkardModsAbstractVillager(CraftServer server, AbstractVillager entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsAbstractVillager{" + getType() + '}';
    }
}
