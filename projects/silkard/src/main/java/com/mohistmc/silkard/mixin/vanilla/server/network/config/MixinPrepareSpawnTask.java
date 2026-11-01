package com.mohistmc.silkard.mixin.vanilla.server.network.config;

import com.mohistmc.silkard.injected.server.network.config.ContextPrepareSpawnTask;
import com.mohistmc.silkard.injected.server.players.ContextPlayerList;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(PrepareSpawnTask.class)
public class MixinPrepareSpawnTask implements ContextPrepareSpawnTask {

    @Unique
    private ServerPlayer silkard$player;

    @Override
    public ServerPlayer silkard$player() {
        return silkard$player;
    }

    @Override
    public void silkard$setPlayer(ServerPlayer player) {
        this.silkard$player = player;
    }

    // CraftBukkit - reuse the already-created player data
    @Redirect(method = "start", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;loadPlayerData(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;"))
    private Optional<CompoundTag> silkard$loadPlayerData(PlayerList playerList, NameAndId nameAndId) {
        return silkard$player != null ? ((ContextPlayerList) playerList).silkard$loadPlayerData(silkard$player) : playerList.loadPlayerData(nameAndId);
    }

    // CraftBukkit - SPIGOT-7507: fall back to the already-loaded player dimension
    @ModifyVariable(method = "start", at = @At("STORE"), ordinal = 0)
    private LevelData.RespawnData silkard$respawnData(LevelData.RespawnData original) {
        return silkard$player != null ? ((ServerLevel) silkard$player.level()).getRespawnData() : original;
    }
}
