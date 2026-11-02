package com.mohistmc.silkard.fabric;

import com.mohistmc.silkard.util.DynamicEnumUtil;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftMenuType;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers for modded menus and their Bukkit inventory types.
 *
 * @author Mgazul
 */
public final class FabricMenuAPI {

    private static final Map<Identifier, InventoryType> MENU_INVENTORY_TYPES = new ConcurrentHashMap<>();

    private FabricMenuAPI() {
    }

    public static boolean isModded(@Nullable AbstractContainerMenu menu) {
        Identifier key = getMenuTypeKey(menu);
        return key != null && isModdedMenuType(key);
    }

    public static @Nullable String getModId(@Nullable AbstractContainerMenu menu) {
        Identifier key = getMenuTypeKey(menu);
        return key == null || !isModdedMenuType(key) ? null : key.getNamespace();
    }

    public static @Nullable String getModId(@Nullable InventoryView view) {
        return getModId(getMenu(view));
    }

    public static @Nullable String getModId(@Nullable InventoryOpenEvent event) {
        return event == null ? null : getModId(event.getView());
    }

    public static @Nullable InventoryType ensureInventoryType(@Nullable Identifier menuTypeId, net.minecraft.world.inventory.@Nullable MenuType<?> menuType) {
        if (menuTypeId == null || menuType == null) {
            return null;
        }
        InventoryType existing = MENU_INVENTORY_TYPES.get(menuTypeId);
        if (existing != null) {
            return existing;
        }
        synchronized (MENU_INVENTORY_TYPES) {
            existing = MENU_INVENTORY_TYPES.get(menuTypeId);
            if (existing != null) {
                return existing;
            }
            MenuType bukkitMenuType;
            try {
                bukkitMenuType = CraftMenuType.minecraftToBukkit(menuType);
            } catch (Throwable ignored) {
                return null;
            }
            String name = nextInventoryTypeName(menuTypeId);
            InventoryType inventoryType = DynamicEnumUtil.addEnum(
                    InventoryType.class,
                    name,
                    new Class<?>[]{Integer.TYPE, String.class, MenuType.class, Boolean.TYPE},
                    new Object[]{0, menuTypeId.toString(), bukkitMenuType, false}
            );
            if (inventoryType == null) {
                return null;
            }
            MENU_INVENTORY_TYPES.put(menuTypeId, inventoryType);
            return inventoryType;
        }
    }

    public static @Nullable InventoryType getInventoryType(@Nullable AbstractContainerMenu menu) {
        Identifier key = getMenuTypeKey(menu);
        if (key == null) {
            return null;
        }
        InventoryType inventoryType = MENU_INVENTORY_TYPES.get(key);
        if (inventoryType == null && isModdedMenuType(key)) {
            inventoryType = ensureInventoryType(key, menu.getType());
        }
        return inventoryType;
    }

    public static @Nullable InventoryType getInventoryType(@Nullable InventoryView view) {
        return getInventoryType(getMenu(view));
    }

    public static @Nullable InventoryType getInventoryType(@Nullable InventoryOpenEvent event) {
        return event == null ? null : getInventoryType(event.getView());
    }

    public static @Nullable Identifier getMenuTypeKey(@Nullable AbstractContainerMenu menu) {
        if (menu == null) {
            return null;
        }
        try {
            return BuiltInRegistries.MENU.getKey(menu.getType());
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static @Nullable AbstractContainerMenu getMenu(@Nullable InventoryView view) {
        return view instanceof CraftInventoryView<?, ?> craftView ? craftView.getHandle() : null;
    }

    public static @Nullable AbstractContainerMenu getMenu(@Nullable InventoryOpenEvent event) {
        return event == null ? null : getMenu(event.getView());
    }

    private static boolean isModdedMenuType(Identifier id) {
        String namespace = id.getNamespace();
        return !"minecraft".equals(namespace)
                && !"fabric".equals(namespace)
                && !"silkard".equals(namespace)
                && FabricLoader.getInstance().isModLoaded(namespace);
    }

    private static String nextInventoryTypeName(Identifier menuTypeId) {
        Set<String> existing = new HashSet<>();
        for (InventoryType type : InventoryType.values()) {
            existing.add(type.name());
        }
        String name = DynamicEnumUtil.normalizeName(menuTypeId.toString());
        if (!existing.contains(name)) {
            return name;
        }
        int suffix = 2;
        while (existing.contains(name + "_" + suffix)) {
            suffix++;
        }
        return name + "_" + suffix;
    }
}
