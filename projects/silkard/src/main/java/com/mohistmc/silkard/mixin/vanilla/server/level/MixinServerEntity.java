package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.level.ContextServerEntity;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(ServerEntity.class)
public class MixinServerEntity implements ContextServerEntity {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private Entity entity;

    // CraftBukkit start - per-entity tracked player set
    @Unique
    private java.util.Set<ServerPlayerConnection> silkard$trackedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public void setTrackedPlayers(java.util.Set<ServerPlayerConnection> trackedPlayers) {
        this.silkard$trackedPlayers = trackedPlayers;
    }

    public java.util.Set<ServerPlayerConnection> silkard$trackedPlayers() {
        return this.silkard$trackedPlayers;
    }

    // CraftBukkit - send passenger updates to the entity itself as well
    @Redirect(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"))
    private void silkard$sendPassengers(ServerEntity.Synchronizer synchronizer, Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener> packet) {
        synchronizer.sendToTrackingPlayers(packet);
        if (this.entity instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
    }

    // CraftBukkit - only update map holders that actually track the entity
    @Redirect(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;players()Ljava/util/List;"))
    private List<ServerPlayer> silkard$trackedMapPlayers(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayerConnection connection : this.silkard$trackedPlayers) {
            players.add(connection.getPlayer());
        }
        return players;
    }

    // CraftBukkit - PlayerVelocityEvent
    @Redirect(method = "sendChanges", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;syncVelocity:Z", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private boolean silkard$playerVelocity(Entity entity) {
        if (entity instanceof ServerPlayer) {
            Player player = (Player) entity.getBukkitEntity();
            org.bukkit.util.Vector velocity = player.getVelocity();
            PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
            ((ContextLevel) entity.level()).getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            if (!velocity.equals(event.getVelocity())) {
                player.setVelocity(event.getVelocity());
            }
        }
        return entity.syncVelocity;
    }

    // CraftBukkit - just return for removed entities instead of spamming the log
    @Inject(method = "sendPairingData", at = @At("HEAD"), cancellable = true)
    private void silkard$skipRemovedPairing(ServerPlayer player, java.util.function.Consumer<Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>> broadcast, CallbackInfo ci) {
        if (this.entity.isRemoved()) {
            ci.cancel();
        }
    }
    // CraftBukkit end
}
