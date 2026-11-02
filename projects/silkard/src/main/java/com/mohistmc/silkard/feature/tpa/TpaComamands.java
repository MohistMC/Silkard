package com.mohistmc.silkard.feature.tpa;

import com.mohistmc.silkard.util.I18n;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaComamands extends Command {

    public static HashMap<Player, Player> tpa = new HashMap<>();

    public TpaComamands(String name) {
        super(name);
        this.usageMessage = "/tpa <player_name>";
        this.setPermission("silkard.command.tpa");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (sender instanceof Player player) {
            Player a = Bukkit.getPlayer(args[0]);
            if (a == null) {
                player.sendMessage(I18n.as("tpacommands.noplayer"));
                return false;
            }
            if (a == player) {
                player.sendMessage(I18n.as("tpacommands.yourself"));
                return false;
            }
            a.sendMessage(I18n.as("tpacommands.sent", player.getName()));
            a.sendMessage(I18n.as("tpacommands.click.accept") + " /tpaccept");
            a.sendMessage(I18n.as("tpacommands.click.deny") + " /tpadeny");
            player.sendMessage(I18n.as("tpacommands.successfully"));
            tpa.remove(a);
            tpa.put(a, player);
            return true;
        }
        return false;
    }
}
