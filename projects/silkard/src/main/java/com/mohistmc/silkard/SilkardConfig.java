package com.mohistmc.silkard;

import com.google.common.base.Throwables;
import com.mohistmc.silkard.feature.entitylimits.EntityLimitsConfig;
import com.mohistmc.silkard.util.I18n;
import com.mohistmc.silkard.util.YamlUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class SilkardConfig {

    private static final List<String> HEADER = Arrays.asList("""
        This is the main configuration file for Silkard.
        As you can see, there's tons to configure. Some options may impact gameplay, so use
        with caution, and make sure you know what each option does before configuring.

        If you need help with the configuration or have any questions related to Spigot,
        join us at the Discord or drop by our forums and leave a post.

        Discord: https://discord.gg/mohistmc
        Forums: https://mohistmc.com/

        """.split("\\n"));
    /*========================================================================*/
    public static YamlConfiguration config;
    public static File silkardyml = new File("silkard-config", "silkard.yml");
    public static YamlConfiguration yml = YamlConfiguration.loadConfiguration(silkardyml);
    public static boolean show_logo;
    public static String silkard_lang;
    public static int maximumRepairCost;
    public static boolean enchantment_fix;
    public static int max_enchantment_level;
    public static int maxBees;
    public static boolean bookAnimationTick;
    public static boolean networkmanager_debug;
    public static List<String> networkmanager_intercept;
    public static boolean keepinventory_global;
    public static boolean keepinventory_inventory;
    public static boolean keepinventory_permission_enable;
    public static String keepinventory_inventory_permission;
    public static boolean keepinventory_exp;
    public static String keepinventory_exp_permission;
    // Thread Priority
    public static int server_thread;
    public static boolean clear_item;
    public static List<String> clear_item_whitelist;
    public static String clear_item_msg;
    public static int clear_item_time;
    public static boolean clear_monster;
    public static List<String> clear_monster_whitelist;
    public static String clear_monster_msg;
    public static int clear_monster_time;
    // Ban
    public static boolean ban_item_enable;
    public static boolean ban_block_enable;
    public static boolean ban_entity_enable;
    public static boolean no_vanilla_entity_enable;
    public static List<String> no_vanilla_entity_whitelist;
    public static boolean ban_enchantment_enable;
    public static boolean ban_recipe_enable;
    public static boolean ban_world_enable;
    public static String pingCommandOutput;
    // Ban events
    public static boolean doFireTick;
    public static boolean explosion;
    public static boolean farmlandTrample;
    public static boolean join_message;
    public static boolean quit_message;
    public static boolean bukkitpermissionshandler;
    public static boolean recipe_warn;
    public static boolean permissions_debug_console;
    public static boolean permissions_send_player;
    public static boolean pluginchannel_debug;
    public static boolean custom_no_villager;
    public static boolean custom_entity_tp_end;
    public static boolean custom_entity_tp_nether;
    public static boolean custom_raid_no_emerald;
    public static int custom_lava_speed_normal;
    public static int custom_lava_speed_nether;
    public static boolean custom_fix_flat;
    public static boolean custom_fix_cmi_tempban;
    public static boolean custom_disabled_spawner;
    public static boolean custom_disabled_sign_commands;
    public static boolean player_modlist_blacklist_enable;
    public static boolean player_modlist_blacklist_use_real_feedback;
    public static String player_modlist_blacklist_failurereasons;
    public static List<String> player_modlist_blacklist;
    public static boolean fakeplayer_callbukkitevent = false;
    public static boolean no_damage_particle;
    public static boolean backup_world_enable;
    public static int backup_world_interval;
    public static int backup_world_max_backups;
    public static boolean backup_world_use_zstd;

    static int version;
    private static File CONFIG_FILE;

    public static void init() {
        CONFIG_FILE = silkardyml;
        config = new YamlConfiguration();
        try {
            if (!CONFIG_FILE.exists()) {
                File parent = CONFIG_FILE.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                CONFIG_FILE.createNewFile();
            }
            config.load(CONFIG_FILE);
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load silkard.yml, please correct your syntax errors", ex);
            Throwables.throwIfUnchecked(ex);
        }

        config.options().setHeader(HEADER);
        config.options().copyDefaults(true);

        EntityLimitsConfig.init();
        readConfig();

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILE, ex);
        }
    }

    public static void save() {
        YamlUtils.save(silkardyml, config);
    }

    static void readConfig() {
        for (Method method : SilkardConfig.class.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(null);
                    } catch (InvocationTargetException ex) {
                        Throwables.throwIfUnchecked(ex.getCause());
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error invoking " + method, ex);
                    }
                }
            }
        }
    }

    private static void set(String path, Object val) {
        config.set(path, val);
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, def);
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, def);
    }

    private static <T> List<String> getStringList(String path, T def) {
        config.addDefault(path, def);
        return config.getStringList(path);
    }

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, def);
    }

    private static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, def);
    }

    public static String silkard_lang() {
        return config.getString("silkard.lang", Locale.getDefault().toString());
    }

    private static void silkard() {
        show_logo = getBoolean("silkard.show_logo", true);
        silkard_lang = getString("silkard.lang", Locale.getDefault().toString());
        maximumRepairCost = getInt("anvilfix.maximumrepaircost", 40);
        enchantment_fix = getBoolean("anvilfix.enchantment_fix", false);
        max_enchantment_level = getInt("anvilfix.max_enchantment_level", 32767);
        maxBees = getInt("max-bees-in-hive", 3);
        bookAnimationTick = getBoolean("enchantment-table-book-animation-tick", false);
        networkmanager_debug = getBoolean("networkmanager.debug", false);
        networkmanager_intercept = getStringList("networkmanager.intercept", new ArrayList<>());
        keepinventory_global = getBoolean("keepinventory.global.enable", false);
        keepinventory_permission_enable = getBoolean("keepinventory.permission.enable", false);
        keepinventory_inventory = getBoolean("keepinventory.global.inventory", true);
        keepinventory_inventory_permission = getString("keepinventory.permission.inventory", "silkard.keepinventory.inventory");
        keepinventory_exp = getBoolean("keepinventory.global.exp", true);
        keepinventory_exp_permission = getString("keepinventory.permission.exp", "silkard.keepinventory.exp");
        server_thread = getInt("threadpriority.server_thread", 5);

        clear_item = getBoolean("entity.clear.item.enable", false);
        clear_item_whitelist = getStringList("entity.clear.item.whitelist", new ArrayList<>());
        clear_item_msg = getString("entity.clear.item.msg", "[Server] Cleaned up %size% drop item");
        clear_item_time = getInt("entity.clear.item.time", 1800);

        clear_monster = getBoolean("entity.clear.monster.enable", false);
        clear_monster_whitelist = getStringList("entity.clear.monster.whitelist", new ArrayList<>());
        clear_monster_msg = getString("entity.clear.monster.msg", "[Server] Cleaned up %size% monster");
        clear_monster_time = getInt("entity.clear.monster.time", 1800);

        ban_item_enable = getBoolean("bans.item", false);
        ban_block_enable = getBoolean("bans.block", false);
        ban_entity_enable = getBoolean("bans.entity.enable", false);
        no_vanilla_entity_enable = getBoolean("bans.entity.vanilla_entity.enable", false);
        no_vanilla_entity_whitelist = getStringList("bans.entity.vanilla_entity.whitelist", new ArrayList<>());
        ban_enchantment_enable = getBoolean("bans.enchantment", false);
        ban_recipe_enable = getBoolean("bans.recipe", false);
        ban_world_enable = getBoolean("bans.world", false);

        pingCommandOutput = getString("settings.messages.ping-command-output", "§2%s's ping is %sms");

        doFireTick = getBoolean("events.fire_tick", false);
        explosion = getBoolean("events.explosion", false);
        farmlandTrample = getBoolean("events.farmlandTrample", false);
        join_message = getBoolean("events.join_message", true);
        quit_message = getBoolean("events.quit_message", true);

        bukkitpermissionshandler = getBoolean("neoforge.bukkitpermissionshandler", true);

        recipe_warn = getBoolean("recipe.warn", false);

        permissions_debug_console = getBoolean("permissions.debug.console", false);
        permissions_send_player = getBoolean("permissions.debug.player", false);
        pluginchannel_debug = getBoolean("pluginchannel.debug", false);

        custom_no_villager = getBoolean("custom.no_villager", false);
        custom_entity_tp_end = getBoolean("custom.entity_tp_end", true);
        custom_entity_tp_nether = getBoolean("custom.entity_tp_nether", true);
        custom_raid_no_emerald = getBoolean("custom.raid_no_emerald", false);
        custom_lava_speed_normal = getInt("custom.lava_speed.normal", 30);
        custom_lava_speed_nether = getInt("custom.lava_speed.nether", 10);
        custom_fix_flat = getBoolean("custom.fix_flat", false);
        custom_fix_cmi_tempban = getBoolean("custom.fix_cmi_tempban", false);
        custom_disabled_spawner = getBoolean("custom.disabled_spawner", false);
        custom_disabled_sign_commands = getBoolean("custom.disabled_signblock_commands", true);
        no_damage_particle = getBoolean("custom.no_damage_particle", false);

        backup_world_enable = getBoolean("backup_world.enable", false);
        backup_world_interval = getInt("backup_world.interval", 3600);
        backup_world_max_backups = getInt("backup_world.max_backups", 5);
        backup_world_use_zstd = getBoolean("backup_world.use_zstd", false);

        player_modlist_blacklist_enable = getBoolean("player_modlist_blacklist.enable", false);
        player_modlist_blacklist_use_real_feedback = getBoolean("player_modlist_blacklist.use_real_feedback", false);
        player_modlist_blacklist_failurereasons = getString("player_modlist_blacklist.failurereasons", "<gradient:#00FF00:#0000FF>Do not install mods privately</gradient>");
        player_modlist_blacklist = getStringList("player_modlist_blacklist.list", new ArrayList<>());

        fakeplayer_callbukkitevent = getBoolean("fakeplayer.callbukkitevent", fakeplayer_callbukkitevent);

        getBoolean("keepinventory.world.inventory", false);
        getBoolean("keepinventory.world.exp", false);
    }

    public static boolean isCN() {
        return config.getString("silkard.lang", Locale.getDefault().toString()).contains("CN");
    }

    public static int getThreadPriority() {
        int priority = SilkardConfig.config.getInt("threadpriority.server_thread", 5);
        priority = Math.clamp(priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        return priority;
    }
}
