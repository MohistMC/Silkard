package com.mohistmc.silkard.injected.server.network;

import com.mohistmc.silkard.util.ContextStateException;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public interface ContextServerCommonPacketListenerImpl {

    default void disconnect(String s) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$processedDisconnect() {
        throw new ContextStateException("Not implemented");
    }

    default boolean isDisconnected() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setProcessedDisconnect(boolean processedDisconnect) {
        throw new ContextStateException("Not implemented");
    }

    default CraftPlayer getCraftPlayer() {
        throw new ContextStateException("Not implemented");
    }

    default boolean isTransferred() {
        throw new ContextStateException("Not implemented");
    }

    default ConnectionProtocol getProtocol() {
        throw new ContextStateException("Not implemented");
    }

    default void sendPacket(Packet<?> packet) {
        throw new ContextStateException("Not implemented");
    }

    default void kickPlayer(Component reason) {
        throw new ContextStateException("Not implemented");
    }

    default net.minecraft.server.MinecraftServer silkard$server() {
        throw new ContextStateException("Not implemented");
    }

    default ServerPlayer silkard$player() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setPlayer(ServerPlayer player) {
        throw new ContextStateException("Not implemented");
    }
}
