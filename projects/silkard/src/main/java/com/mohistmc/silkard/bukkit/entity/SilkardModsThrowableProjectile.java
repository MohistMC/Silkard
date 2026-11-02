package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftThrowableProjectile;

public class SilkardModsThrowableProjectile extends CraftThrowableProjectile {

    public SilkardModsThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "SilkardModsThrowableProjectile{" + getType() + '}';
    }
}
