package com.mohistmc.silkard.commands;

import net.minecraft.server.players.NameAndId;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

/**
 * Restores {@code /unban} against the Bukkit profile ban list.
 */
public class UnbanCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("unban")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests(
                            (context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().getPlayerList().getBans().getUserList(), builder
                            )
                        )
                        .executes(context -> pardon(GameProfileArgument.getGameProfiles(context, "targets")))
                )
        );
    }

    private static int pardon(Collection<NameAndId> profiles) {
        BanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
        int count = 0;
        for (NameAndId profile : profiles) {
            if (profile.name() == null) {
                continue;
            }
            banList.pardon(profile.name());
            count++;
        }
        return count;
    }
}
