package com.mohistmc.silkard.mixin.vanilla.server.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.server.network.ContextServerCommonPacketListenerImpl;
import com.mohistmc.silkard.injected.server.network.ContextServerLoginPacketListenerImpl;
import com.mohistmc.silkard.injected.server.players.ContextPlayerList;
import com.mojang.authlib.GameProfile;
import java.net.SocketAddress;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public class MixinServerLoginPacketListenerImpl implements ContextServerLoginPacketListenerImpl, CraftPlayer.TransferCookieConnection {

    // @formatter:off
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private Connection connection;
    @Shadow private boolean transferred;
    // @formatter:on

    @Unique
    private ServerPlayer silkard$player;

    @Unique
    private volatile boolean silkard$preLoginDone;

    @Unique
    private Component silkard$kickMessage;

    @Override
    public Connection silkard$connection() {
        return this.connection;
    }

    @Override
    public boolean isTransferred() {
        return this.transferred;
    }

    @Override
    public ConnectionProtocol getProtocol() {
        return ConnectionProtocol.LOGIN;
    }

    @Override
    public void sendPacket(Packet<?> packet) {
        this.connection.send(packet);
    }

    @Override
    public void kickPlayer(Component reason) {
        ((ServerLoginPacketListenerImpl) (Object) this).disconnect(reason);
    }

    @Override
    public void silkard$setKickMessage(Component component) {
        this.silkard$kickMessage = component;
    }

    @Override
    public Component silkard$kickMessage() {
        return this.silkard$kickMessage;
    }

    @Override
    public void disconnect(String s) {
        ((ServerLoginPacketListenerImpl) (Object) this).disconnect(Component.literal(s));
    }

    @Override
    public void callPlayerPreLoginEvents(GameProfile gameprofile) throws Exception {
        String playerName = gameprofile.name();
        java.net.InetAddress address = ((java.net.InetSocketAddress) this.connection.getRemoteAddress()).getAddress();
        UUID uniqueId = gameprofile.id();
        final CraftServer server = ((ContextMinecraftServer) this.server).silkard$server();

        AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address, uniqueId, this.transferred);
        server.getPluginManager().callEvent(asyncEvent);

        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
            final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address, uniqueId);
            if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
            }
            Waitable<PlayerPreLoginEvent.Result> waitable = new Waitable<PlayerPreLoginEvent.Result>() {
                @Override
                protected PlayerPreLoginEvent.Result evaluate() {
                    server.getPluginManager().callEvent(event);
                    return event.getResult();
                }
            };

            ((ContextMinecraftServer) this.server).silkard_processQueue().add(waitable);
            if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED) {
                disconnect(event.getKickMessage());
                return;
            }
        } else if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            disconnect(asyncEvent.getKickMessage());
        }
    }

    // CraftBukkit - fire pre-login events before client verification starts
    @Inject(method = "startClientVerification", at = @At("HEAD"))
    private void silkard$preLoginEvents(GameProfile gameprofile, CallbackInfo ci) throws Exception {
        if (this.silkard$preLoginDone) {
            return;
        }
        this.silkard$preLoginDone = true;
        callPlayerPreLoginEvents(gameprofile);
    }

    // CraftBukkit - offline mode: run the blocking pre-login events off the netty thread
    @WrapOperation(method = "handleHello",
        at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;startClientVerification(Lcom/mojang/authlib/GameProfile;)V"))
    private void silkard$offlineLogin(ServerLoginPacketListenerImpl self, GameProfile gameprofile, Operation<Void> original) {
        Thread thread = new Thread("User Authenticator") {

            @Override
            public void run() {
                try {
                    callPlayerPreLoginEvents(gameprofile);
                    silkard$preLoginDone = true;
                    original.call(self, gameprofile);
                } catch (Exception ex) {
                    disconnect("Failed to verify username!");
                    MinecraftServer.LOGGER.warn("Exception verifying {}", gameprofile.name(), ex);
                }
            }
        };

        thread.start();
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"))
    private Component silkard$canPlayerLogin(PlayerList playerlist, SocketAddress address, NameAndId nameAndId) {
        ServerPlayer player = ((ContextPlayerList) playerlist).canPlayerLogin((ServerLoginPacketListenerImpl) (Object) this, new GameProfile(nameAndId.id(), nameAndId.name()));
        if (player == null) {
            return this.silkard$kickMessage == null ? Component.empty() : this.silkard$kickMessage;
        }
        this.silkard$player = player;
        return null;
    }

    // CraftBukkit - run on the server thread
    @Inject(method = "handleLoginAcknowledgement", at = @At("HEAD"))
    private void silkard$ensureLoginAckThread(ServerboundLoginAcknowledgedPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerLoginPacketListenerImpl) (Object) this, this.server.packetProcessor());
    }

    // CraftBukkit - plugin cookie handling
    @Inject(method = "handleCookieResponse", at = @At("HEAD"), cancellable = true)
    private void silkard$cookieResponse(ServerboundCookieResponsePacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerLoginPacketListenerImpl) (Object) this, this.server.packetProcessor());
        if (this.silkard$player != null && this.silkard$player.getBukkitEntity().handleCookieResponse(packet)) {
            ci.cancel();
        }
    }

    // CraftBukkit - carry the already-created player into the configuration phase
    @Inject(method = "handleLoginAcknowledgement", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;startConfiguration()V", shift = At.Shift.BEFORE))
    private void silkard$attachPlayerToConfiguration(ServerboundLoginAcknowledgedPacket packet, CallbackInfo ci, @Local(ordinal = 0) ServerConfigurationPacketListenerImpl listener) {
        ((ContextServerCommonPacketListenerImpl) listener).silkard$setPlayer(this.silkard$player);
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;disconnectAllPlayersWithProfile(Ljava/util/UUID;)Z"))
    private boolean silkard$disconnectAllPlayersWithProfile(PlayerList playerlist, UUID uuid) {
        return ((ContextPlayerList) playerlist).disconnectAllPlayersWithProfile(uuid, this.silkard$player);
    }
}
