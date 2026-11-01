package com.mohistmc.silkard.mixin.vanilla.server.network;

import com.mohistmc.silkard.injected.server.network.ContextServerCommonPacketListenerImpl;
import com.mohistmc.silkard.injected.server.network.config.ContextPrepareSpawnTask;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.bukkit.craftbukkit.CraftServerLinks;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(ServerConfigurationPacketListenerImpl.class)
public class MixinServerConfigurationPacketListenerImpl {

    // @formatter:off
    @Shadow private ClientInformation clientInformation;
    @Shadow private PrepareSpawnTask prepareSpawnTask;
    // @formatter:on

    // CraftBukkit - PlayerLinksSendEvent before sending server links
    @Redirect(method = "startConfiguration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;serverLinks()Lnet/minecraft/server/ServerLinks;"))
    private ServerLinks silkard$serverLinks(MinecraftServer server) throws ExecutionException, InterruptedException {
        ServerLinks serverLinks = server.serverLinks();
        ServerPlayer player = ((ContextServerCommonPacketListenerImpl) (Object) this).silkard$player();
        if (player == null) {
            return serverLinks;
        }

        // CraftBukkit - run on the server thread
        if (server.isSameThread()) {
            return silkard$sendServerLinks(serverLinks, player);
        }

        // CraftBukkit - this path runs on a Netty/fabric async thread. Blocking on the server
        // processQueue is unsafe here (the main thread is not guaranteed to drain it during the
        // configuration handshake), which deadlocks every joining client. Dispatch the event to the
        // scheduler and return the original links so the handshake is never blocked.
        server.execute(() -> {
            try {
                silkard$sendServerLinks(serverLinks, player);
            } catch (Throwable t) {
                // fire-and-forget; never let the event break the join handshake
            }
        });
        return serverLinks;
    }

    @Unique
    private ServerLinks silkard$sendServerLinks(ServerLinks serverLinks, ServerPlayer player) {
        CraftServerLinks wrapper = new CraftServerLinks(serverLinks);
        PlayerLinksSendEvent event = new PlayerLinksSendEvent(player.getBukkitEntity(), wrapper);
        player.getBukkitEntity().getServer().getPluginManager().callEvent(event);
        return wrapper.getServerLinks();
    }

    // CraftBukkit - hand the already-created player to the spawn task
    @Inject(method = "returnToWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;prepareSpawnTask:Lnet/minecraft/server/network/config/PrepareSpawnTask;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void silkard$attachPlayerToSpawnTask(CallbackInfo ci) {
        if (this.prepareSpawnTask != null) {
            ((ContextPrepareSpawnTask) (Object) this.prepareSpawnTask).silkard$setPlayer(((ContextServerCommonPacketListenerImpl) (Object) this).silkard$player());
        }
    }

    // CraftBukkit - run on the server thread
    @Inject(method = "handleAcceptCodeOfConduct", at = @At("HEAD"))
    private void silkard$ensureCodeOfConductThread(ServerboundAcceptCodeOfConductPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerConfigurationPacketListenerImpl) (Object) this, ((ContextServerCommonPacketListenerImpl) (Object) this).silkard$server().packetProcessor());
    }

    // CraftBukkit - login checks already completed
    @Redirect(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"))
    private Component silkard$skipLoginCheck(PlayerList playerList, SocketAddress address, NameAndId nameAndId) {
        return null;
    }

    // CraftBukkit - apply the client information collected during configuration
    @Inject(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/config/PrepareSpawnTask;spawnPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/level/ServerPlayer;", shift = At.Shift.BEFORE))
    private void silkard$updateOptions(ServerboundFinishConfigurationPacket packet, CallbackInfo ci) {
        ServerPlayer player = ((ContextServerCommonPacketListenerImpl) (Object) this).silkard$player();
        if (player != null) {
            player.updateOptions(this.clientInformation);
        }
    }
}