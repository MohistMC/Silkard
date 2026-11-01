package com.mohistmc.silkard.mixin.vanilla.server.network.config;

import com.mojang.authlib.GameProfile;
import com.mohistmc.silkard.injected.server.network.config.ContextPrepareSpawnTask;
import com.mohistmc.silkard.injected.server.players.ContextPlayerList;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Ready")
public class MixinPrepareSpawnTask$Ready {

    @Shadow @Final private PrepareSpawnTask this$0;

    // CraftBukkit - reuse the player created during login
    @Redirect(method = "spawn", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer silkard$reusePlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation information) {
        ServerPlayer player = ((ContextPrepareSpawnTask) (Object) this.this$0).silkard$player();
        return player != null ? player : new ServerPlayer(server, level, profile, information);
    }

    // CraftBukkit - load data through the reused player
    @Redirect(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;loadPlayerData(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;"))
    private Optional<CompoundTag> silkard$loadPlayerData(PlayerList playerList, NameAndId nameAndId) {
        ServerPlayer player = ((ContextPrepareSpawnTask) (Object) this.this$0).silkard$player();
        return player != null ? ((ContextPlayerList) playerList).silkard$loadPlayerData(player) : playerList.loadPlayerData(nameAndId);
    }
}
