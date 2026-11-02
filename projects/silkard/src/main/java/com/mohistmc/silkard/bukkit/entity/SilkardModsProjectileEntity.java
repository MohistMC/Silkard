package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftProjectile;

public class SilkardModsProjectileEntity extends CraftProjectile {

    public SilkardModsProjectileEntity(CraftServer server, Projectile entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsProjectileEntity{" + getType() + '}';
    }
}
