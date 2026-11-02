package com.mohistmc.silkard.feature.commands;

import com.mohistmc.silkard.feature.config.SilkardPluginConfig;

/**
 * @author Mgazul
 * {@code @date} 2025/11/23 01:28
 */
public class CommandsConfig extends SilkardPluginConfig {

    public static CommandsConfig INSTANCE;

    public CommandsConfig(String file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new CommandsConfig("commands.yml");
    }

    public boolean enable(String key) {
        if (!yaml.contains(key)) {
            yaml.set(key, false);
            save();
            return false;
        }
        return yaml.getBoolean(key, false);
    }
}
