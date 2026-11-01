package com.mohistmc.silkard.mixin.vanilla.server.network;

import com.mohistmc.silkard.injected.network.ContextDiscardedPayload;
import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.server.level.ContextServerPlayer;
import com.mohistmc.silkard.injected.server.network.ContextServerCommonPacketListenerImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import java.util.concurrent.ExecutionException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class MixinServerCommonPacketListenerImpl implements ContextServerCommonPacketListenerImpl, CraftPlayer.TransferCookieConnection {

    // @formatter:off
    @Shadow @Final private static org.slf4j.Logger LOGGER;
    @Shadow @Final protected MinecraftServer server;
    @Shadow @Final protected Connection connection;
    @Shadow private boolean transferred;
    @Shadow public abstract void send(Packet<?> packet);
    @Shadow public abstract void disconnect(Component reason);
    // @formatter:on

    // CraftBukkit start
    @Unique
    public boolean processedDisconnect;

    @Unique
    public ServerPlayer player;

    @Unique
    public CraftServer cserver;

    private static final Identifier silkard$CUSTOM_REGISTER = Identifier.withDefaultNamespace("register");
    private static final Identifier silkard$CUSTOM_UNREGISTER = Identifier.withDefaultNamespace("unregister");
    // CraftBukkit end

    @Inject(method = "<init>", at = @At("TAIL"))
    private void silkard$init(MinecraftServer server, Connection connection, CommonListenerCookie cookie, CallbackInfo ci) {
        this.cserver = ((ContextMinecraftServer) server).silkard$server();
    }

    @Override
    public boolean silkard$processedDisconnect() {
        return processedDisconnect;
    }

    @Override
    public void silkard$setProcessedDisconnect(boolean processedDisconnect) {
        this.processedDisconnect = processedDisconnect;
    }

    @Override
    public MinecraftServer silkard$server() {
        return this.server;
    }

    @Override
    public ServerPlayer silkard$player() {
        return player;
    }

    @Override
    public void silkard$setPlayer(ServerPlayer player) {
        this.player = player;
        if (player != null) {
            ((ContextServerPlayer) player).silkard$transferCookieConnection(this);
        }
    }

    @Override
    public CraftPlayer getCraftPlayer() {
        return (player != null) ? (CraftPlayer) player.getBukkitEntity() : null;
    }

    @Override
    public boolean isTransferred() {
        return this.transferred;
    }

    @Override
    public ConnectionProtocol getProtocol() {
        return ((PacketListener) (Object) this).protocol();
    }

    @Override
    public void sendPacket(Packet<?> packet) {
        this.send(packet);
    }

    @Override
    public void kickPlayer(Component reason) {
        this.disconnect(reason);
    }

    @Override
    public boolean isDisconnected() {
        return player != null && !((ContextServerPlayer) player).silkard$joining() && !this.connection.isConnected();
    }

    @Inject(method = "handleKeepAlive", at = @At("HEAD"))
    private void silkard$ensureKeepAliveThread(ServerboundKeepAlivePacket packet, CallbackInfo ci) {
        if (player != null) {
            PacketUtils.ensureRunningOnSameThread(packet, (ServerCommonPacketListenerImpl) (Object) this, player.level());
        }
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void silkard$customPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!(packet.payload() instanceof DiscardedPayload discarded) || player == null || cserver == null) {
            return;
        }
        PacketUtils.ensureRunningOnSameThread(packet, (ServerCommonPacketListenerImpl) (Object) this, player.level());
        Identifier identifier = packet.payload().type().id();
        ByteBuf payload = ((ContextDiscardedPayload) (Object) discarded).silkard$data();

        if (identifier.equals(silkard$CUSTOM_REGISTER)) {
            try {
                String channels = payload.toString(com.google.common.base.Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    getCraftPlayer().addChannel(channel);
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't register custom payload", ex);
                this.disconnect(Component.literal("Invalid payload REGISTER!"));
            }
        } else if (identifier.equals(silkard$CUSTOM_UNREGISTER)) {
            try {
                String channels = payload.toString(com.google.common.base.Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    getCraftPlayer().removeChannel(channel);
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't unregister custom payload", ex);
                this.disconnect(Component.literal("Invalid payload UNREGISTER!"));
            }
        } else {
            try {
                byte[] data = new byte[payload.readableBytes()];
                payload.readBytes(data);
                cserver.getMessenger().dispatchIncomingMessage(player.getBukkitEntity(), identifier.toString(), data);
            } catch (Exception ex) {
                LOGGER.error("Couldn't dispatch custom payload", ex);
                this.disconnect(Component.literal("Invalid custom payload!"));
            }
        }
    }

    @Inject(method = "handleCustomClickAction", at = @At("HEAD"), cancellable = true)
    private void silkard$customClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerCommonPacketListenerImpl) (Object) this, this.server.packetProcessor());
        ((ContextMinecraftServer) this.server).handleCustomClickAction(packet.id(), packet.payload(), player);
        ci.cancel();
    }

    @Inject(method = "handleResourcePackResponse", at = @At("TAIL"))
    private void silkard$resourcePackStatus(ServerboundResourcePackPacket packet, CallbackInfo ci) {
        if (player != null && cserver != null) {
            cserver.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(getCraftPlayer(), packet.id(), PlayerResourcePackStatusEvent.Status.values()[packet.action().ordinal()]));
        }
    }

    @Inject(method = "handleCookieResponse", at = @At("HEAD"), cancellable = true)
    private void silkard$cookieResponse(ServerboundCookieResponsePacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerCommonPacketListenerImpl) (Object) this, this.server.packetProcessor());
        if (player != null && player.getBukkitEntity().handleCookieResponse(packet)) {
            ci.cancel();
        }
    }

    // CraftBukkit - 25000L keep-alive timeout
    @ModifyConstant(method = "keepConnectionAlive", constant = @Constant(longValue = 15000L))
    private long silkard$keepAliveTimeout(long constant) {
        return 25000L;
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void silkard$send(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (packet == null) {
            ci.cancel();
            return;
        }
        if (packet instanceof ClientboundSetDefaultSpawnPositionPacket spawn && player != null) {
            ((ContextServerPlayer) player).silkard$compassTarget(CraftLocation.toBukkit(spawn.respawnData().pos(), getCraftPlayer().getWorld()));
        }
    }

    // CraftBukkit start - fire PlayerKickEvent
    @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("HEAD"), cancellable = true)
    private void silkard$disconnect(DisconnectionDetails details, CallbackInfo ci) {
        if (player == null || cserver == null) {
            return;
        }
        if (this.processedDisconnect) {
            ci.cancel();
            return;
        }
        if (!this.cserver.isPrimaryThread()) {
            ServerCommonPacketListenerImpl self = (ServerCommonPacketListenerImpl) (Object) this;
            Waitable<Void> waitable = new Waitable<Void>() {
                @Override
                protected Void evaluate() {
                    self.disconnect(details);
                    return null;
                }
            };

            ((ContextMinecraftServer) this.server).silkard_processQueue().add(waitable);

            try {
                waitable.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            ci.cancel();
            return;
        }

        String leaveMessage = ChatFormatting.YELLOW + player.getScoreboardName() + " left the game.";
        PlayerKickEvent event = new PlayerKickEvent(player.getBukkitEntity(), CraftChatMessage.fromComponent(details.reason()), leaveMessage);

        if (this.cserver.getServer().isRunning()) {
            this.cserver.getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        ((ContextServerPlayer) player).silkard$setKickLeaveMessage(event.getLeaveMessage());
        silkard$disconnect0(new DisconnectionDetails(CraftChatMessage.fromString(event.getReason(), true)[0], details.report(), details.bugReportLink()));
        ci.cancel();
    }

    @Unique
    private void silkard$disconnect0(DisconnectionDetails details) {
        this.connection.send(new ClientboundDisconnectPacket(details.reason()), PacketSendListener.thenRun(() -> this.connection.disconnect(details)));
        ((ServerCommonPacketListenerImpl) (Object) this).onDisconnect(details);
        this.connection.setReadOnly();
        this.server.execute(this.connection::handleDisconnection); // CraftBukkit - Don't wait
    }
    // CraftBukkit end
}
