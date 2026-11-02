package com.mohistmc.silkard.feature.ban.bans;

import com.mohistmc.silkard.SilkardConfig;
import com.mohistmc.silkard.api.EntityAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.bukkit.NamespacedKey;

/**
 * @author Mgazul
 * {@code @date} 2023/7/27 13:35:20
 */
public class BanEntity {

    public static boolean check(Entity entity) {
        if (!SilkardConfig.ban_entity_enable) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof ArmorStand) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (entity.getBukkitEntity().hasMetadata("npc")) return false;
        return banVanilla(entity) || EntityAPI.isBan(entity);
    }

    /**
     * Checks if a vanilla entity should be banned
     *
     * @param entity The entity to check
     * @return true if the entity should be banned, false otherwise
     */
    public static boolean banVanilla(Entity entity) {
        if (!SilkardConfig.no_vanilla_entity_enable) return false;
        var list = SilkardConfig.no_vanilla_entity_whitelist;
        if (list.isEmpty()) return false;
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) && !list.contains(key.toString());
    }
}
