package com.mohistmc.silkard.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ServerAPI {

    public static final Set<String> modlists_Inside = Set.of("minecraft", "fabric", "silkard");
    public static final Set<String> modlists_Client = new HashSet<>();
    public static final Set<String> modlists_Server = new HashSet<>();
    public static final List<String> modlists_All = new ArrayList<>();
    public static final Map<String, String> forgecmdper = new ConcurrentHashMap<>();
    public static final Map<net.minecraft.world.entity.EntityType<?>, String> entityTypeMap = new ConcurrentHashMap<>();
    public static final Map<net.minecraft.world.entity.EntityType<?>, EntityType> entityTypeMap0 = new ConcurrentHashMap<>();

    static {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            modlists_All.add(mod.getMetadata().getId());
        }
    }

    public static Set<String> channels_Incoming() {
        return Bukkit.getMessenger().getIncomingChannels();
    }

    public static Set<String> channels_Outgoing() {
        return Bukkit.getMessenger().getOutgoingChannels();
    }

    public static int getModSize() {
        return FabricLoader.getInstance().getAllMods().size();
    }

    public static Boolean hasMod(String modid) {
        return modlists_All.contains(modid);
    }

    public static Boolean hasPlugin(String pluginname) {
        return Bukkit.getPluginManager().getPlugin(pluginname) != null;
    }

    public static void putBukkitEvents(Listener listener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public static MinecraftServer getNMSServer() {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        return craftServer.getServer();
    }

    public static String getMainLevelName() {
        return ((net.minecraft.server.dedicated.DedicatedServer) getNMSServer()).getProperties().levelName;
    }
}
