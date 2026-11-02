package com.mohistmc.silkard.commands;

import com.mohistmc.silkard.api.ColorAPI;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand extends BukkitCommand {

    public static ArrayList<Player> vanished = new ArrayList<>();

    public VanishCommand(String name) {
        super(name);
        this.description = "Invisibility yourself";
        this.usageMessage = "/vanish";
        this.setPermission("silkard.command.vanish");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ColorAPI.string("&cThe console is not available."));
            return false;
        }
        if (args.length != 0 || !p.isOp()) {
            if (args.length == 1 && p.isOp()) {
                Player p2 = Bukkit.getServer().getPlayer(args[0]);
                if (p2 != null) {
                    if (!VanishCommand.vanished.contains(p2)) {
                        for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                            pl.hidePlayer(p2);
                        }
                        VanishCommand.vanished.add(p2);
                        p2.sendMessage(ColorAPI.string("&2Incognito mode is turned on"));
                        return true;
                    }
                    for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                        pl.showPlayer(p2);
                    }
                    VanishCommand.vanished.remove(p2);
                    p2.sendMessage(ColorAPI.string("&2Incognito mode is turned off"));
                    return true;
                } else {
                    p.sendMessage(ColorAPI.string("&cThe player is not online"));
                }
            }
            return false;
        }
        if (!VanishCommand.vanished.contains(p)) {
            for (Player pl2 : Bukkit.getServer().getOnlinePlayers()) {
                pl2.hidePlayer(p);
            }
            VanishCommand.vanished.add(p);
            p.sendMessage(ColorAPI.string("&2Incognito mode is turned on"));
            return true;
        }
        for (Player pl2 : Bukkit.getServer().getOnlinePlayers()) {
            pl2.showPlayer(p);
        }
        VanishCommand.vanished.remove(p);
        p.sendMessage(ColorAPI.string("&2Incognito mode is turned off"));
        return true;
    }
}
