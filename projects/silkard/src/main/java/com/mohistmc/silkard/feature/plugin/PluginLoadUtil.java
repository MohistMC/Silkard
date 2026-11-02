package com.mohistmc.silkard.feature.plugin;

import com.mohistmc.silkard.util.I18n;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.slf4j.Logger;

public final class PluginLoadUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PluginLoadUtil() {
    }

    private static void logProgress(String msg) {
        if (PluginHotReloadUtil.isReloading()) {
            LOGGER.debug(msg);
        } else {
            LOGGER.info(msg);
        }
    }

    /**
     * Read the plugin name from a JAR without fully loading it.
     * Returns null if the JAR is not a valid plugin.
     */
    public static String detectPluginName(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.getName().endsWith(".jar")) return null;
        try (JarFile jar = new JarFile(jarFile, true, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
            ZipEntry entry = jar.getEntry("plugin.yml");
            if (entry == null) return null;
            try (InputStream in = jar.getInputStream(entry)) {
                PluginDescriptionFile desc = new PluginDescriptionFile(in);
                return desc.getName();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Plugin loadPlugin(File jarFile) {
        if (jarFile == null) throw new IllegalArgumentException("jarFile must not be null");
        if (!jarFile.exists())
            throw new IllegalArgumentException(I18n.as("plugin.loadutil.notexists", jarFile.getAbsolutePath()));
        if (!jarFile.getName().endsWith(".jar"))
            throw new IllegalArgumentException(I18n.as("plugin.loadutil.notjar", jarFile.getAbsolutePath()));

        // Pre-check: if a plugin with the same name is already loaded, refuse
        String name = detectPluginName(jarFile);
        if (name != null && Bukkit.getPluginManager().getPlugin(name) != null) {
            LOGGER.warn(I18n.as("plugin.loadutil.duplicate", name, jarFile.getAbsolutePath()));
            return null;
        }

        logProgress(I18n.as("plugin.loadutil.loading", jarFile.getAbsolutePath()));

        try {
            Plugin plugin = Bukkit.getPluginManager().loadPlugin(jarFile);
            if (plugin == null) {
                LOGGER.error(I18n.as("plugin.loadutil.null", jarFile.getAbsolutePath()));
                return null;
            }
            logProgress(I18n.as("plugin.loadutil.success", plugin.getName(), plugin.getDescription().getVersion()));
            return plugin;
        } catch (InvalidPluginException e) {
            LOGGER.error(I18n.as("plugin.loadutil.invalid", jarFile.getAbsolutePath(), e.getMessage()));
            LOGGER.debug(I18n.as("plugin.loadutil.detail"), e);
            return null;
        } catch (Exception e) {
            LOGGER.error(I18n.as("plugin.loadutil.error", jarFile.getAbsolutePath()), e);
            return null;
        }
    }

    public static Plugin loadPlugin(Path jarPath) {
        if (jarPath == null) throw new IllegalArgumentException("jarPath must not be null");
        return loadPlugin(jarPath.toFile());
    }

    public static boolean enablePlugin(Plugin plugin) {
        if (plugin == null) return false;
        if (plugin.isEnabled()) {
            LOGGER.warn(I18n.as("plugin.loadutil.alreadyenabled", plugin.getName()));
            return false;
        }
        logProgress(I18n.as("plugin.loadutil.enabling", plugin.getName()));
        try {
            Bukkit.getPluginManager().enablePlugin(plugin);
            logProgress(I18n.as("plugin.loadutil.enabled", plugin.getName()));
            return true;
        } catch (Throwable e) {
            LOGGER.error(I18n.as("plugin.loadutil.enablefail", plugin.getName()), e);
            return false;
        }
    }

    public static Plugin loadAndEnablePlugin(File jarFile) {
        Plugin plugin = loadPlugin(jarFile);
        if (plugin == null) return null;
        if (enablePlugin(plugin)) return plugin;
        PluginUnloadUtil.unloadPlugin(plugin);
        return null;
    }
}
