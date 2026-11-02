package com.mohistmc.silkard.commands;

import com.mohistmc.silkard.SilkardMod;
import com.mohistmc.silkard.api.ServerAPI;
import com.mohistmc.silkard.util.I18n;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public class DumpCommand extends Command {

    private final List<String> tab_cmd = Arrays.asList("potions", "effect", "particle", "enchants", "cbcmds", "modscmds", "entitytypes", "biomes", "pattern", "worldgen", "worldtype", "material", "channels", "advancements");

    public DumpCommand(String name) {
        super(name);
        this.description = I18n.as("dumpcmd.description");
        this.usageMessage = "/dump [potions|enchants|cbcmds|modscmds|entitytypes|biomes|pattern|worldgen|worldtype|material|channels|advancements]";
        this.setPermission("silkard.command.dump");
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if ((sender.isOp() || testPermission(sender))) {
            if (args.length == 1) {
                for (String param : tab_cmd) {
                    if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                        list.add(param);
                    }
                }
            }
        }

        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(I18n.as("commands.usage", usageMessage));
            return false;
        }
        if (args.length == 1) {
            switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "potions" -> dumpPotions(sender);
                case "effect" -> dumpEffect(sender);
                case "particle" -> dumpParticle(sender);
                case "enchants" -> dumpEnchant(sender);
                case "cbcmds" -> dumpCBCommands(sender);
                case "modscmds" -> dumpModsCommands(sender);
                case "entitytypes" -> dumpEntityTypes(sender);
                case "biomes" -> dumpBiomes(sender);
                case "pattern" -> dumpPattern(sender);
                case "worldtype" -> dumpWorldType(sender);
                case "material" -> dumpMaterial(sender);
                case "channels" -> dumpChannels(sender);
                case "advancements" -> dumpAdvancements(sender);
                default -> {
                    sender.sendMessage(I18n.as("commands.usage", usageMessage));
                    return false;
                }
            }
        }
        return false;
    }

    private void dumpEffect(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (PotionEffectType pet : PotionEffectType.values()) {
            sb.append(pet).append("\n");
        }
        dump(sender, "effect", sb);
    }

    private void dumpPotions(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (PotionType pet : PotionType.values()) {
            if (pet != null) {
                sb.append(pet).append("\n");
            }
        }
        dump(sender, "potions", sb);
    }

    private void dumpParticle(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Particle pet : Particle.values()) {
            if (pet != null) {
                sb.append(pet).append("\n");
            }
        }
        dump(sender, "particle", sb);
    }

    private void dumpEnchant(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Enchantment ench : Enchantment.values()) {
            sb.append(ench).append("\n");
        }
        dump(sender, "enchants", sb);
    }

    private void dumpEntityTypes(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (EntityType ent : EntityType.values()) {
            sb.append(ent.toString()).append("\n");
        }
        dump(sender, "entitytypes", sb);
    }

    private void dumpCBCommands(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Command per : ((org.bukkit.craftbukkit.CraftServer) org.bukkit.Bukkit.getServer()).getCommandMap().getCommands()) {
            // Do not print if there is no permission
            if (per.getPermission() == null) {
                continue;
            }
            sb.append(per.getName()).append(": ").append(per.getPermission()).append("\n");
        }
        dump(sender, "cbcommands", sb);
    }

    private void dumpModsCommands(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> m : ServerAPI.forgecmdper.entrySet()) {
            sb.append(m.getKey()).append(": ").append(m.getValue()).append("\n");
        }
        dump(sender, "modscommands", sb);
    }

    private void dumpBiomes(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Biome biome : Biome.values()) {
            sb.append(biome.toString()).append(": ").append(biome.getKey()).append("\n");
        }
        dump(sender, "biomes", sb);
    }

    private void dumpPattern(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (PatternType patternType : PatternType.values()) {
            String key = patternType.getIdentifier();
            sb.append(key).append("_").append(PatternType.getByIdentifier(key)).append("\n");
        }
        dump(sender, "pattern", sb);
    }

    private void dumpWorldType(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (WorldType type : WorldType.values()) {
            String key = type.getName();
            sb.append(type).append("-").append(key).append("\n");
        }
        dump(sender, "worldtype", sb);
    }

    private void dumpMaterial(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Material material : Material.values()) {
            if (!material.isLegacy()) {
                String key = material.getKey().toString();
                sb.append(material).append("-").append(key).append("\n");
            }
        }
        dump(sender, "material", sb);
    }

    private void dumpChannels(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (String channel : ServerAPI.channels_Outgoing()) {
            sb.append(channel).append("\n");
        }
        for (String channel : ServerAPI.channels_Incoming()) {
            sb.append(channel).append("\n");
        }
        dump(sender, "channels", sb);
    }

    private void dumpAdvancements(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (AdvancementHolder channel : ServerAPI.getNMSServer().getAdvancements().getAllAdvancements()) {
            sb.append(channel.id()).append("\n");
        }
        dump(sender, "advancements", sb);
    }

    private void dumpmsg(CommandSender sender, String path, String type) {
        sender.sendMessage(I18n.as("dumpcmd.success", type, path));
    }

    private void dump(CommandSender sender, String type, StringBuilder sb) {
        saveToF(type, sb, sender);
    }

    private void saveToF(String type, StringBuilder sb, CommandSender sender) {
        File file = new File("dump", type + ".txt");
        file.getParentFile().mkdirs();
        writeByteArrayToFile(file, sb);
        dumpmsg(sender, file.getAbsolutePath(), type);
    }

    protected void writeByteArrayToFile(File file, StringBuilder sb) {
        try {
            FileUtils.writeByteArrayToFile(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            SilkardMod.LOGGER.error(e);
        }
    }
}
