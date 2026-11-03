package com.mohistmc.silkard.feature;

import com.mohistmc.silkard.api.gui.GuiListener;
import com.mohistmc.silkard.commands.DumpCommand;
import com.mohistmc.silkard.commands.HatCommand;
import com.mohistmc.silkard.commands.HideAllCommand;
import com.mohistmc.silkard.commands.HideCommand;
import com.mohistmc.silkard.commands.InfoCommand;
import com.mohistmc.silkard.commands.ItemsCommand;
import com.mohistmc.silkard.commands.OpenInvCommand;
import com.mohistmc.silkard.commands.PermissionCommand;
import com.mohistmc.silkard.commands.ShowsCommand;
import com.mohistmc.silkard.commands.SilkardCommand;
import com.mohistmc.silkard.commands.VanishCommand;
import com.mohistmc.silkard.SilkardConfig;
import com.mohistmc.silkard.feature.back.BackCommands;
import com.mohistmc.silkard.feature.back.BackConfig;
import com.mohistmc.silkard.feature.ban.BanConfig;
import com.mohistmc.silkard.feature.ban.BanListener;
import com.mohistmc.silkard.feature.commands.CommandsConfig;
import com.mohistmc.silkard.feature.db.DatabaseConfig;
import com.mohistmc.silkard.feature.db.DatabaseManager;
import com.mohistmc.silkard.feature.entitylimits.EntityLimitsCommands;
import com.mohistmc.silkard.feature.item.ItemsConfig;
import com.mohistmc.silkard.feature.menu.MenuCommand;
import com.mohistmc.silkard.feature.menu.MenuListener;
import com.mohistmc.silkard.feature.plugin.PluginHotReloadCommand;
import com.mohistmc.silkard.feature.tpa.TpaComamands;
import com.mohistmc.silkard.feature.tpa.TpacceptCommands;
import com.mohistmc.silkard.feature.tpa.TpadenyCommands;
import com.mohistmc.silkard.feature.warps.WarpsCommands;
import com.mohistmc.silkard.feature.warps.WarpsConfig;
import com.mohistmc.silkard.feature.world.WorldManage;
import com.mohistmc.silkard.feature.world.commands.WorldsCommands;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.command.Command;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * @author Mgazul
 * {@code @date} 2023/6/14 14:46:34
 */
public class SilkardPlugin {

    public static Logger LOGGER = LogManager.getLogger("SilkardPlugin");

    public static void init() {
        DatabaseConfig.init();
        DatabaseManager.init();
        SilkardConfig.init();
        BanConfig.init();
        if (CommandsConfig.INSTANCE.enable("worlds.enable")) {
            WorldManage.onEnable();
        }
        ItemsConfig.init();
        BackConfig.init();
        WarpsConfig.init();
        EntityClear.start();
        WorldBackup.start();
        GlobalVariableSystem.register();
    }

    public static void shutdown() {
        EntityClear.stop();
        WorldBackup.stop();
        DatabaseManager.shutdown();
    }

    public static void registerCommands(Map<String, Command> commands) {
        CommandsConfig.init();
        if (CommandsConfig.INSTANCE.enable("worlds.enable")) {
            commands.put("worlds", new WorldsCommands("worlds"));
        }
        if (CommandsConfig.INSTANCE.enable("warps.enable")) {
            commands.put("warps", new WarpsCommands("warps"));
        }
        if (CommandsConfig.INSTANCE.enable("tpa.enable")) {
            commands.put("tpa", new TpaComamands("tpa"));
            commands.put("tpadeny", new TpadenyCommands("tpadeny"));
            commands.put("tpaccept", new TpacceptCommands("tpaccept"));
        }
        if (CommandsConfig.INSTANCE.enable("back.enable")) {
            commands.put("back", new BackCommands("back"));
        }
        if (CommandsConfig.INSTANCE.enable("menus.enable")) {
            commands.put("menus", new MenuCommand("menus"));
        }
        if (CommandsConfig.INSTANCE.enable("hideall.enable")) {
            commands.put("hideall", new HideAllCommand("hideall"));
        }
        if (CommandsConfig.INSTANCE.enable("hide.enable")) {
            commands.put("hide", new HideCommand("hide"));
        }
        if (CommandsConfig.INSTANCE.enable("hat.enable")) {
            commands.put("hat", new HatCommand("hat"));
        }
        if (CommandsConfig.INSTANCE.enable("vanish.enable")) {
            commands.put("vanish", new VanishCommand("vanish"));
        }
        if (CommandsConfig.INSTANCE.enable("openinv.enable")) {
            commands.put("openinv", new OpenInvCommand("openinv"));
        }
        if (CommandsConfig.INSTANCE.enable("entitylimits.enable")) {
            commands.put("entitylimits", new EntityLimitsCommands("entitylimits"));
        }
        commands.put("plugin", new PluginHotReloadCommand("plugin"));

    }

    public static void registerAllCommands(org.bukkit.command.CommandMap commandMap) {
        Map<String, Command> commands = new HashMap<>();
        commands.put("silkard", new SilkardCommand("silkard"));
        commands.put("dump", new DumpCommand("dump"));
        commands.put("items", new ItemsCommand("items"));
        commands.put("permission", new PermissionCommand("permission"));
        commands.put("shows", new ShowsCommand("shows"));
        commands.put("infos", new InfoCommand("infos"));
        registerCommands(commands);
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            commandMap.register(entry.getKey(), "silkard", entry.getValue());
        }
        // CraftBukkit - startup sync already ran; without it the brigadier tree misses these labels and sub-commands cannot tab-complete
        ((org.bukkit.craftbukkit.CraftServer) org.bukkit.Bukkit.getServer()).syncCommands();
    }

    public static void registerListener(Event event) {
        if (event instanceof InventoryClickEvent inventoryClickEvent) {
            GuiListener.onInventoryClickEvent(inventoryClickEvent);
        }
        if (event instanceof PrepareAnvilEvent prepareAnvilEvent) {
            EnchantmentFix.anvilListener(prepareAnvilEvent);
        }
        if (event instanceof InventoryCloseEvent event1) {
            BanListener.save(event1);
            GuiListener.onInventoryCloseEvent(event1);
        }
        if (event instanceof PlayerTeleportEvent event1) {
            BackCommands.hookTeleport(event1);
        }
        if (event instanceof PlayerDeathEvent event1) {
            BackCommands.hooktDeath(event1);
        }
        if (event instanceof PlayerInteractEvent event1) {
            if (CommandsConfig.INSTANCE.enable("menus.enable")) {
                MenuListener.onPlayerInteract(event1);
            }
        }
    }
}
