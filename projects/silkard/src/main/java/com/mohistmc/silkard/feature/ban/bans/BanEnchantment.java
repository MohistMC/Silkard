package com.mohistmc.silkard.feature.ban.bans;

import com.mohistmc.silkard.SilkardConfig;
import com.mohistmc.silkard.api.EnchantmentAPI;
import com.mohistmc.silkard.feature.ban.BanConfig;
import com.mohistmc.silkard.feature.ban.BanType;
import net.minecraft.core.Holder;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul
 * {@code @date} 2023/7/27 15:18:21
 */
public class BanEnchantment {

    public static boolean check(Enchantment enchantment) {
        if (!SilkardConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        return list.contains(enchantment.getKey().toString());
    }

    public static boolean check(Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        if (!SilkardConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        return list.contains(enchantment.getRegisteredName());
    }

    public static boolean check(ItemStack itemStack) {
        if (!SilkardConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(itemStack)) {
                if (list.contains(enchantment.getKey().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean check(net.minecraft.world.item.ItemStack itemStack) {
        if (!SilkardConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(CraftItemStack.asBukkitCopy(itemStack))) {
                if (list.contains(enchantment.getKey().toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
