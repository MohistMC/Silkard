package com.mohistmc.silkard.api;

import com.mohistmc.silkard.feature.ban.BanConfig;
import com.mohistmc.silkard.feature.ban.BanType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SpawnEggItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemAPI {

    public static final Logger LOGGER = LogManager.getLogger("ItemAPI");

    public static net.minecraft.world.item.ItemStack toNMSItem(Material material) {
        return CraftItemStack.asNMSCopy(new ItemStack(material));
    }

    public static ItemStack base64(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) ois.readObject();
                return ItemStack.deserialize(map);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to decode ItemStack from base64", e);
            return new ItemStack(Material.AIR);
        }
    }

    public static String base64(ItemStack stack) {
        try {
            Map<String, Object> map = stack.serialize();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(map);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void name(ItemStack itemStack, String name) {
        ItemMeta im = itemStack.getItemMeta();
        im.setDisplayName(name);
        itemStack.setItemMeta(im);
    }

    public static void rarity(ItemStack itemStack, ItemRarity rarity) {
        ItemMeta im = itemStack.getItemMeta();
        im.setRarity(rarity);
        itemStack.setItemMeta(im);
    }

    public static void lore(ItemStack itemStack, List<String> lores) {
        ItemMeta im = itemStack.getItemMeta();
        im.setLore(lores);
        itemStack.setItemMeta(im);
    }

    public static void customModelData(ItemStack itemStack, int customModelData) {
        ItemMeta im = itemStack.getItemMeta();
        im.setCustomModelData(customModelData);
        itemStack.setItemMeta(im);
    }

    public static boolean isBan(ItemStack itemStack) {
        if (itemStack == null) return false;
        var list = BanConfig.getListByType(BanType.ITEM);
        if (list.isEmpty()) return false;
        return list.contains(itemStack.getType().getKey().toString());
    }

    public static Material getEggMaterial(net.minecraft.world.entity.EntityType<?> entitytype) {
        try {
            if (entitytype == net.minecraft.world.entity.EntityTypes.PLAYER) {
                return Material.PLAYER_HEAD;
            }
            var getMaterial = SpawnEggItem.byId(entitytype);
            if (getMaterial.isPresent()) {
                return CraftItemStack.asBukkitCopy(getMaterial.get().value().getDefaultInstance()).getType();
            } else {
                var key = net.minecraft.world.entity.EntityType.getKey(entitytype);
                if (BuiltInRegistries.ITEM.get(key).isEmpty()) {
                    return Material.SPAWNER;
                }
                Material material = get(key);
                return material.isAir() ? Material.SPAWNER : material;
            }
        } catch (Exception e) {
            return Material.SPAWNER;
        }
    }

    public static Enchantment getEnchantmentByName(String name) {
        try {
            return Enchantment.getByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public static Enchantment getEnchantmentByKey(String key) {
        try {
            return Enchantment.getByKey(NamespacedKey.fromString(key));
        } catch (Exception e) {
            return getEnchantmentByName(key);
        }
    }

    public static Material get(Identifier key) {
        var item = BuiltInRegistries.ITEM.get(key);
        return item.map(i -> CraftItemStack.asBukkitCopy(i.value().getDefaultInstance()).getType()).orElse(Material.AIR);
    }

    public static void attribute(ItemStack itemStack, Attribute attribute, double value, EquipmentSlot slot) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        UUID uuid = UUID.randomUUID();
        String name = attribute.name().toLowerCase().replace("_", " ");
        AttributeModifier modifier = new AttributeModifier(uuid, "silkard." + name, value, AttributeModifier.Operation.ADD_NUMBER, slot);
        meta.addAttributeModifier(attribute, modifier);
        itemStack.setItemMeta(meta);
    }

    public static boolean removeAttribute(ItemStack itemStack, Attribute attribute) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        if (!meta.hasAttributeModifiers() || meta.getAttributeModifiers(attribute) == null) return false;
        meta.removeAttributeModifier(attribute);
        itemStack.setItemMeta(meta);
        return true;
    }
}
