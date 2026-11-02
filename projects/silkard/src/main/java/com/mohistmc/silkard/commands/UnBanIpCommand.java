package com.mohistmc.silkard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

/**
 * Restores {@code /unban-ip} against the Bukkit IP ban list.
 */
public class UnBanIpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("unban-ip")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.argument("target", StringArgumentType.word())
                        .suggests(
                            (context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().getPlayerList().getIpBans().getUserList(), builder
                            )
                        )
                        .executes(context -> unbanIp(StringArgumentType.getString(context, "target")))
                )
        );
    }

    private static int unbanIp(String address) {
        BanList banList = Bukkit.getBanList(BanList.Type.IP);
        banList.pardon(address);
        return 1;
    }
}
