package com.mohistmc.silkard.fabric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime mappings between modded NMS entity types and their Bukkit enum constants.
 *
 * @author Mgazul
 */
public final class FabricEntityRegistry {

    private static final Map<net.minecraft.world.entity.EntityType<?>, String> NMS_TO_NAME = new ConcurrentHashMap<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, org.bukkit.entity.EntityType> NMS_TO_BUKKIT = new ConcurrentHashMap<>();

    private FabricEntityRegistry() {
    }

    public static void register(net.minecraft.world.entity.EntityType<?> handle, String name, org.bukkit.entity.EntityType bukkit) {
        NMS_TO_NAME.put(handle, name);
        NMS_TO_BUKKIT.put(handle, bukkit);
    }

    public static String name(net.minecraft.world.entity.EntityType<?> handle) {
        return NMS_TO_NAME.get(handle);
    }

    public static org.bukkit.entity.EntityType bukkit(net.minecraft.world.entity.EntityType<?> handle) {
        return NMS_TO_BUKKIT.get(handle);
    }
}
