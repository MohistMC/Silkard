package com.mohistmc.silkard.injected.server.players;

import com.mohistmc.silkard.util.ContextStateException;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.player.PlayerRespawnEvent;

public interface ContextPlayerList {

    default CraftServer getCraftServer() {
        throw new ContextStateException("Not implemented");
    }

    default ServerPlayer silkard$respawnWithReason(ServerPlayer entityplayer, boolean flag, Entity.RemovalReason entity_removalreason, PlayerRespawnEvent.RespawnReason reason) {
        throw new ContextStateException("Not implemented");
    }

    default void broadcastAll(Packet<?> packet, Player entityhuman) {
        throw new ContextStateException("Not implemented");
    }

    default void broadcastAll(Packet<?> packet, Level world) {
        throw new ContextStateException("Not implemented");
    }

    default void broadcastMessage(Component[] iChatBaseComponents) {
        throw new ContextStateException("Not implemented");
    }

    default ServerStatsCounter getPlayerStats(ServerPlayer entityhuman) {
        throw new ContextStateException("Not implemented");
    }

    default ServerStatsCounter getPlayerStats(NameAndId gameprofile) {
        throw new ContextStateException("Not implemented");
    }

    default String silkard$quiltMsg() {
        throw new ContextStateException("Not implemented");
    }

    default ServerPlayer silkard$respawnWithLocation(ServerPlayer playerIn, boolean flag, Entity.RemovalReason removalReason, PlayerRespawnEvent.RespawnReason respawnReason, Location location) {
        throw new ContextStateException("Not implemented");
    }

    default ServerPlayer canPlayerLogin(ServerLoginPacketListenerImpl loginlistener, GameProfile gameProfile) {
        throw new ContextStateException("Not implemented");
    }

    default boolean disconnectAllPlayersWithProfile(java.util.UUID uuid, ServerPlayer player) {
        throw new ContextStateException("Not implemented");
    }

    default java.util.Optional<net.minecraft.nbt.CompoundTag> silkard$loadPlayerData(ServerPlayer player) {
        throw new ContextStateException("Not implemented");
    }

    default void reloadRecipes() {
        throw new ContextStateException("Not implemented");
    }
}
