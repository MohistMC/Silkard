package com.mohistmc.silkard.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class SilkardCommands {

    private SilkardCommands() {
    }

    public static void registerVanilla(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (dispatcher.getRoot().getChild("unban") == null) {
            UnbanCommand.register(dispatcher);
        }
        if (dispatcher.getRoot().getChild("unban-ip") == null) {
            UnBanIpCommand.register(dispatcher);
        }
    }
}
