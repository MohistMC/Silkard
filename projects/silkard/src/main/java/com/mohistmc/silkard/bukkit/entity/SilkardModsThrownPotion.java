package com.mohistmc.silkard.bukkit.entity;

import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftThrownPotion;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class SilkardModsThrownPotion extends CraftThrownPotion {

    public SilkardModsThrownPotion(CraftServer server, AbstractThrownPotion entity) {
        super(server, entity);
    }

    public PotionMeta getPotionMeta() {
        return (PotionMeta) CraftItemStack.getItemMeta(this.getHandle().getItem());
    }

    @Override
    public String toString() {
        return "SilkardModsThrownPotion{" + getType() + '}';
    }
}
