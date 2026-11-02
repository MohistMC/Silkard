package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractWindCharge;

public class SilkardModsWindCharge extends CraftAbstractWindCharge {

    public SilkardModsWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
