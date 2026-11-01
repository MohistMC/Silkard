package com.mohistmc.silkard.mixin.vanilla.server.network;

import com.mohistmc.silkard.injected.network.ContextConnection;
import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(ServerHandshakePacketListenerImpl.class)
public class MixinServerHandshakePacketListenerImpl {

    @Unique
    private static final Map<InetAddress, Long> silkard$throttleTracker = new HashMap<>();

    @Unique
    private static int silkard$throttleCounter = 0;

    @Shadow
    @Final
    private Connection connection;

    @Shadow
    @Final
    private MinecraftServer server;

    // CraftBukkit start - set hostname
    @Inject(method = "handleIntention", at = @At("HEAD"))
    private void silkard$hostname(ClientIntentionPacket packet, CallbackInfo ci) {
        ((ContextConnection) (Object) this.connection).silkard$setHostName(packet.hostName() + ":" + packet.port());
    }

    // CraftBukkit start - connection throttle
    @Inject(method = "beginLogin", at = @At("HEAD"), cancellable = true)
    private void silkard$throttle(ClientIntentionPacket packet, boolean transfer, CallbackInfo ci) {
        try {
            long currentTime = System.currentTimeMillis();
            long connectionThrottle = ((ContextMinecraftServer) (Object) this.server).silkard$server().getConnectionThrottle();
            InetAddress address = ((InetSocketAddress) this.connection.getRemoteAddress()).getAddress();

            synchronized (silkard$throttleTracker) {
                if (silkard$throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - silkard$throttleTracker.get(address) < connectionThrottle) {
                    silkard$throttleTracker.put(address, currentTime);
                    Component message = Component.literal("Connection throttled! Please wait before reconnecting.");
                    this.connection.send(new ClientboundLoginDisconnectPacket(message));
                    this.connection.disconnect(message);
                    ci.cancel();
                    return;
                }

                silkard$throttleTracker.put(address, currentTime);
                silkard$throttleCounter++;
                if (silkard$throttleCounter > 200) {
                    silkard$throttleCounter = 0;
                    silkard$throttleTracker.entrySet().removeIf(entry -> entry.getValue() > connectionThrottle);
                }
            }
        } catch (Throwable throwable) {
            org.apache.logging.log4j.LogManager.getLogger().debug("Failed to check connection throttle", throwable);
        }
    }
    // CraftBukkit end
}
