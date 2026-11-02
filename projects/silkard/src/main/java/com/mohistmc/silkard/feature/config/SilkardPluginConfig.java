package com.mohistmc.silkard.feature.config;

import com.mohistmc.silkard.util.YamlUtils;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class SilkardPluginConfig {

    public static final String CONFIG_FILE = "silkard-config";
    public final FileConfiguration yaml;
    public final File config;

    public SilkardPluginConfig(File file) {
        config = file;
        yaml = YamlConfiguration.loadConfiguration(config);
        if (!config.exists()) {
            save();
        }
    }

    public SilkardPluginConfig(String yml) {
        config = new File(SilkardPluginConfig.CONFIG_FILE, yml);
        yaml = YamlConfiguration.loadConfiguration(config);
        if (!config.exists()) {
            save();
        }
    }

    public void save() {
        YamlUtils.save(config, yaml);
    }

    public void put(String key, Object v) {
        yaml.set(key, v);
        save();
    }

    public void remove(String name) {
        yaml.set(name, null);
        save();
    }

    public boolean has(String name) {
        return yaml.get(name) != null;
    }
}
