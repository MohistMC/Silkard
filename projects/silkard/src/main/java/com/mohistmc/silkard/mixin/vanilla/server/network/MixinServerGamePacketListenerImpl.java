package com.mohistmc.silkard.mixin.vanilla.server.network;

import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.server.network.ContextServerCommonPacketListenerImpl;
import com.mohistmc.silkard.injected.server.network.ContextServerGamePacketListenerImpl;
import com.mojang.brigadier.ParseResults;
import java.util.Collections;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.slf4j.Logger;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl implements ContextServerGamePacketListenerImpl {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public ServerPlayer player;

    // CraftBukkit - rewritten command stashed by the preprocess injectors, consumed by the redirects below
    @Unique
    private String silkard$unsignedCommand;

    @Unique
    private String silkard$signedCommand;

    @Unique
    private String silkard$signedOriginal;

    @Unique
    private String silkard$spamCommand;

    // CraftBukkit - expose the player to the inherited common listener
    @Inject(method = "<init>", at = @At("TAIL"))
    private void silkard$init(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        ((ContextServerCommonPacketListenerImpl) (Object) this).silkard$setPlayer(player);
    }

    @Redirect(method = "forceSendPlayerSupportBlocks", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"))
    private void silkard$demoteStandingOnAirLog(Logger instance, String format, Object arg) {
        // CraftBukkit - SPIGOT-8156: warn -> debug
        instance.debug(format, arg);
    }

    @Shadow
    private ParseResults<CommandSourceStack> parseCommand(String command) {
        throw new AssertionError();
    }

    @Shadow
    private <S> Map<String, PlayerChatMessage> collectSignedArguments(ServerboundChatCommandSignedPacket packet, SignableCommand<S> signable, LastSeenMessages lastSeenMessages) throws net.minecraft.network.chat.SignedMessageChain.DecodeException {
        throw new AssertionError();
    }

    // CraftBukkit start - PlayerCommandPreprocessEvent
    @Inject(method = "performUnsignedChatCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void silkard$preprocessUnsigned(String command, CallbackInfo ci) {
        this.silkard$unsignedCommand = null;
        net.minecraft.server.MinecraftServer server = this.player.level().getServer();
        CraftServer craftServer = ((ContextMinecraftServer) server).silkard$server();
        if (craftServer == null) {
            return;
        }
        if (org.spigotmc.SpigotConfig.logCommands) LOGGER.info(this.player.getBukkitEntity().getName() + " issued server command: /" + command);
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(this.player.getBukkitEntity(), "/" + command, new LazyPlayerSet(server));
        craftServer.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        this.silkard$unsignedCommand = event.getMessage().substring(1);
        if (this.silkard$dispatchBukkit(craftServer, event.getPlayer(), this.silkard$unsignedCommand)) {
            ci.cancel();
        }
    }

    // CraftBukkit - plugin commands live in Bukkit's CommandMap, not the NMS dispatcher; route player commands through it.
    @Unique
    private boolean silkard$dispatchBukkit(CraftServer craftServer, org.bukkit.entity.Player player, String command) {
        try {
            return craftServer.dispatchCommand(player, command);
        } catch (org.bukkit.command.CommandException ex) {
            player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
            org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.SEVERE, null, ex);
            return true;
        }
    }

    @Redirect(method = "performUnsignedChatCommand(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;parseCommand(Ljava/lang/String;)Lcom/mojang/brigadier/ParseResults;"))
    private ParseResults<CommandSourceStack> silkard$parseUnsigned(ServerGamePacketListenerImpl self, String command) {
        return this.parseCommand(this.silkard$unsignedCommand != null ? this.silkard$unsignedCommand : command);
    }

    @Redirect(method = "performUnsignedChatCommand(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V"))
    private void silkard$performUnsigned(Commands commands, ParseResults<CommandSourceStack> results, String commandString) {
        commands.performCommand(results, this.silkard$unsignedCommand != null ? this.silkard$unsignedCommand : commandString);
    }

    @Inject(method = "performSignedChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandSignedPacket;Lnet/minecraft/network/chat/LastSeenMessages;)V", at = @At("HEAD"), cancellable = true)
    private void silkard$preprocessSigned(ServerboundChatCommandSignedPacket packet, LastSeenMessages lastSeenMessages, CallbackInfo ci) {
        this.silkard$signedCommand = null;
        this.silkard$signedOriginal = null;
        net.minecraft.server.MinecraftServer server = this.player.level().getServer();
        CraftServer craftServer = ((ContextMinecraftServer) server).silkard$server();
        if (craftServer == null) {
            return;
        }
        String command = packet.command();
        this.silkard$spamCommand = command;
        if (org.spigotmc.SpigotConfig.logCommands) LOGGER.info(this.player.getBukkitEntity().getName() + " issued server command: /" + command);
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(this.player.getBukkitEntity(), "/" + command, new LazyPlayerSet(server));
        craftServer.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        this.silkard$signedOriginal = command;
        this.silkard$signedCommand = event.getMessage().substring(1);
        if (this.silkard$dispatchBukkit(craftServer, event.getPlayer(), this.silkard$signedCommand)) {
            ci.cancel();
        }
    }

    @Redirect(method = "performSignedChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandSignedPacket;Lnet/minecraft/network/chat/LastSeenMessages;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundChatCommandSignedPacket;command()Ljava/lang/String;"))
    private String silkard$signedCommand(ServerboundChatCommandSignedPacket packet) {
        return this.silkard$signedCommand != null ? this.silkard$signedCommand : packet.command();
    }

    @Redirect(method = "performSignedChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandSignedPacket;Lnet/minecraft/network/chat/LastSeenMessages;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;collectSignedArguments(Lnet/minecraft/network/protocol/game/ServerboundChatCommandSignedPacket;Lnet/minecraft/network/chat/SignableCommand;Lnet/minecraft/network/chat/LastSeenMessages;)Ljava/util/Map;"))
    private Map<String, PlayerChatMessage> silkard$collectSigned(ServerGamePacketListenerImpl self, ServerboundChatCommandSignedPacket packet, SignableCommand<CommandSourceStack> signable, LastSeenMessages lastSeenMessages) throws net.minecraft.network.chat.SignedMessageChain.DecodeException {
        // CraftBukkit - a plugin rewrote the command, the original signatures no longer match
        if (this.silkard$signedOriginal == null || this.silkard$signedOriginal.equals(this.silkard$signedCommand)) {
            return this.collectSignedArguments(packet, signable, lastSeenMessages);
        }
        return Collections.emptyMap();
    }
    // CraftBukkit end

    // CraftBukkit start - teleport API
    @Override
    public org.bukkit.craftbukkit.entity.CraftPlayer getCraftPlayer() {
        return this.player.getBukkitEntity();
    }

    @Override
    public boolean silkard$isImmobilePlayer() {
        return ((com.mohistmc.silkard.injected.server.level.ContextServerPlayer) this.player).silkard$isImmobile();
    }

    @Override
    public boolean teleport(double x, double y, double z, float yRot, float xRot, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        return this.teleport(new net.minecraft.world.entity.PositionMoveRotation(new net.minecraft.world.phys.Vec3(x, y, z), net.minecraft.world.phys.Vec3.ZERO, yRot, xRot), Collections.emptySet(), cause);
    }

    public boolean teleport(net.minecraft.world.entity.PositionMoveRotation destination, java.util.Set<net.minecraft.world.entity.Relative> relatives, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        org.bukkit.entity.Player player = this.getCraftPlayer();
        org.bukkit.Location from = player.getLocation();
        net.minecraft.world.entity.PositionMoveRotation absolute = net.minecraft.world.entity.PositionMoveRotation.calculateAbsolute(net.minecraft.world.entity.PositionMoveRotation.of(this.player), destination, relatives);
        org.bukkit.Location to = org.bukkit.craftbukkit.util.CraftLocation.toBukkit(absolute.position(), this.getCraftPlayer().getWorld(), absolute.yRot(), absolute.xRot());
        if (from.equals(to)) {
            this.internalTeleport(destination, relatives);
            return true;
        }

        org.bukkit.event.player.PlayerTeleportEvent event = new org.bukkit.event.player.PlayerTeleportEvent(player, from.clone(), to.clone(), cause);
        ((ContextMinecraftServer) this.player.level().getServer()).silkard$server().getPluginManager().callEvent(event);
        if (event.isCancelled() || !to.equals(event.getTo())) {
            relatives = Collections.emptySet();
            to = event.isCancelled() ? event.getFrom() : event.getTo();
            destination = new net.minecraft.world.entity.PositionMoveRotation(org.bukkit.craftbukkit.util.CraftLocation.toVec3D(to), net.minecraft.world.phys.Vec3.ZERO, to.getYaw(), to.getPitch());
        }
        this.internalTeleport(destination, relatives);
        return !event.isCancelled();
    }

    @Override
    public void teleport(org.bukkit.Location dest) {
        this.internalTeleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch());
    }

    private void internalTeleport(double x, double y, double z, float yRot, float xRot) {
        this.internalTeleport(new net.minecraft.world.entity.PositionMoveRotation(new net.minecraft.world.phys.Vec3(x, y, z), net.minecraft.world.phys.Vec3.ZERO, yRot, xRot), Collections.emptySet());
    }

    public void internalTeleport(net.minecraft.world.entity.PositionMoveRotation destination, java.util.Set<net.minecraft.world.entity.Relative> relatives) {
        if (Float.isNaN(destination.yRot())) {
            destination = new net.minecraft.world.entity.PositionMoveRotation(destination.position(), destination.deltaMovement(), 0, destination.xRot());
        }
        if (Float.isNaN(destination.xRot())) {
            destination = new net.minecraft.world.entity.PositionMoveRotation(destination.position(), destination.deltaMovement(), destination.yRot(), 0);
        }
        this.justTeleported = true;
        this.awaitingTeleportTime = this.tickCount;
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }
        this.player.teleportSetPosition(destination, relatives);
        this.awaitingPositionFromClient = this.player.position();
        this.lastPosX = this.awaitingPositionFromClient.x;
        this.lastPosY = this.awaitingPositionFromClient.y;
        this.lastPosZ = this.awaitingPositionFromClient.z;
        this.lastYaw = this.player.getYRot();
        this.lastPitch = this.player.getXRot();
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - delegate to the event aware teleport
     */
    @Overwrite
    public void teleport(double x, double y, double z, float yRot, float xRot) {
        this.teleport(x, y, z, yRot, xRot, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @Overwrite
    public void teleport(net.minecraft.world.entity.PositionMoveRotation destination, java.util.Set<net.minecraft.world.entity.Relative> relatives) {
        this.teleport(destination, relatives, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @Shadow private double lastPosX;
    @Shadow private double lastPosY;
    @Shadow private double lastPosZ;
    @Shadow private float lastYaw;
    @Shadow private float lastPitch;
    @Shadow private boolean hasMoved;
    @Shadow private boolean justTeleported;
    @Shadow private int awaitingTeleport;
    @Shadow private long awaitingTeleportTime;
    @Shadow private int tickCount;
    @Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;
    // CraftBukkit end

    // CraftBukkit start - PlayerMoveEvent
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"))
    private void silkard$playerMoveEvent(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        this.player.absSnapTo(this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch);

        org.bukkit.entity.Player player = this.getCraftPlayer();
        if (!this.hasMoved) {
            this.lastPosX = this.player.getX();
            this.lastPosY = this.player.getY();
            this.lastPosZ = this.player.getZ();
            this.lastYaw = this.player.getYRot();
            this.lastPitch = this.player.getXRot();
            this.hasMoved = true;
        }
        org.bukkit.Location from = new org.bukkit.Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch);
        org.bukkit.Location to = player.getLocation().clone();
        if (packet.hasPos) {
            to.setX(packet.x);
            to.setY(packet.y);
            to.setZ(packet.z);
        }
        if (packet.hasRot) {
            to.setYaw(packet.yRot);
            to.setPitch(packet.xRot);
        }

        double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
        float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());
        if ((delta > 1f / 256 || deltaAngle > 10f) && !((ContextServerGamePacketListenerImpl) (Object) this).silkard$isImmobilePlayer()) {
            this.lastPosX = to.getX();
            this.lastPosY = to.getY();
            this.lastPosZ = to.getZ();
            this.lastYaw = to.getYaw();
            this.lastPitch = to.getPitch();

            org.bukkit.Location oldTo = to.clone();
            org.bukkit.event.player.PlayerMoveEvent event = new org.bukkit.event.player.PlayerMoveEvent(player, from, to);
            ((ContextMinecraftServer) this.player.level().getServer()).silkard$server().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                this.teleport(from);
                ci.cancel();
                return;
            }
            if (!oldTo.equals(event.getTo())) {
                player.teleport(event.getTo(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                ci.cancel();
            }
        }
    }
    // CraftBukkit end

    // Spigot - moved-wrongly threshold (0056)
    @ModifyConstant(method = "handlePlayerPositionChange(DDDFFZZ)V", constant = @Constant(doubleValue = 0.0625D))
    private double silkard$movedWronglyThreshold(double constant) {
        return org.spigotmc.SpigotConfig.movedWronglyThreshold;
    }

    // Spigot - spam exclusions (0035)
    @Inject(method = "detectCommandRateSpam", at = @At("HEAD"), cancellable = true)
    private void silkard$spamExclusions(CallbackInfo ci) {
        if (this.silkard$spamCommand != null) {
            for (String exclude : org.spigotmc.SpigotConfig.spamExclusions) {
                if (this.silkard$spamCommand.startsWith(exclude)) {
                    ci.cancel();
                    break;
                }
            }
            this.silkard$spamCommand = null;
        }
    }
}
