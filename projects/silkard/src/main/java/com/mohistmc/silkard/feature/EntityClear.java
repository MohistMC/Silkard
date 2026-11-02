package com.mohistmc.silkard.feature;

import com.mohistmc.tools.NamedThreadFactory;
import com.mohistmc.silkard.SilkardConfig;
import com.mohistmc.silkard.bukkit.BukkitUtils;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;

/**
 * @author Mgazul
 * {@code @date} 2023/7/25 23:56:03
 */
public class EntityClear {

    public static final ScheduledExecutorService ENTITYCLEAR_ITEM = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Item"));
    public static final ScheduledExecutorService ENTITYCLEAR_MONSTER = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Monster"));

    public static void start() {
        if (SilkardConfig.clear_item) {
            ENTITYCLEAR_ITEM.scheduleAtFixedRate(() -> {
                MinecraftServer server = BukkitUtils.getServer();
                if (server == null || server.hasStopped()) {
                    return;
                }
                run_item();
            }, 1000 * 60, 1000L * SilkardConfig.clear_item_time, TimeUnit.MILLISECONDS);
        }
        if (SilkardConfig.clear_monster) {
            ENTITYCLEAR_MONSTER.scheduleAtFixedRate(() -> {
                MinecraftServer server = BukkitUtils.getServer();
                if (server == null || server.hasStopped()) {
                    return;
                }
                run_monster();
            }, 1000 * 60, 1000L * SilkardConfig.clear_monster_time, TimeUnit.MILLISECONDS);
        }
    }

    public static void stop() {
        ENTITYCLEAR_ITEM.shutdown();
        ENTITYCLEAR_MONSTER.shutdown();
    }

    public static void run_item() {
        Set<String> whitelist = new java.util.HashSet<>(SilkardConfig.clear_item_whitelist);
        int size_item = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    if (!whitelist.contains(item.getItemStack().getType().name())) {
                        entity.remove();
                        size_item++;
                    }
                }
            }
        }
        if (!SilkardConfig.clear_item_msg.isEmpty()) {
            Bukkit.broadcastMessage(SilkardConfig.clear_item_msg.replace("%size%", String.valueOf(size_item)));
        }
    }

    public static void run_monster() {
        Set<String> whitelist = new java.util.HashSet<>(SilkardConfig.clear_monster_whitelist);
        int size_monster = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster monster) {
                    if (!whitelist.contains(monster.getType().name()) && monster.getCustomName() == null) {
                        entity.remove();
                        size_monster++;
                    }
                }
            }
        }
        if (!SilkardConfig.clear_monster_msg.isEmpty()) {
            Bukkit.broadcastMessage(SilkardConfig.clear_monster_msg.replace("%size%", String.valueOf(size_monster)));
        }
    }
}
