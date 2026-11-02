package com.mohistmc.silkard.mixin.vanilla.server.network;

import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.server.network.ContextServerCommonPacketListenerImpl;
import com.mohistmc.silkard.injected.server.network.ContextServerGamePacketListenerImpl;
import com.mohistmc.silkard.injected.world.inventory.ContextAbstractContainerMenu;
import com.mojang.brigadier.ParseResults;
import java.util.Collections;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.SmithingInventory;
import org.slf4j.Logger;
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
        this.awaitingTeleportTime = this.tickCount;
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }
        this.player.teleportSetPosition(destination, relatives);
        this.awaitingPositionFromClient = this.player.position();
        this.silkard$lastPosX = this.awaitingPositionFromClient.x;
        this.silkard$lastPosY = this.awaitingPositionFromClient.y;
        this.silkard$lastPosZ = this.awaitingPositionFromClient.z;
        this.silkard$lastYaw = this.player.getYRot();
        this.silkard$lastPitch = this.player.getXRot();
        // without this the client never leaves the terrain-loading stage and renders no chunks
        ((ServerGamePacketListenerImpl) (Object) this).send(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.of(this.awaitingTeleport, destination, relatives));
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

    @Shadow private int awaitingTeleport;
    @Shadow private int awaitingTeleportTime;
    @Shadow private int tickCount;
    @Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;

    // Spigot/CraftBukkit tracking state - these fields do not exist in vanilla, so keep them mixin-owned
    @Unique private double silkard$lastPosX;
    @Unique private double silkard$lastPosY;
    @Unique private double silkard$lastPosZ;
    @Unique private float silkard$lastYaw;
    @Unique private float silkard$lastPitch;
    @Unique private boolean silkard$hasMoved;
    // CraftBukkit end

    // CraftBukkit start - PlayerMoveEvent
    @Inject(method = "handlePlayerPositionChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"))
    private void silkard$playerMoveEvent(double requestedX, double requestedY, double requestedZ, float requestedYRot, float requestedXRot, boolean isOnGround, boolean horizontalCollision, CallbackInfo ci) {
        this.player.absSnapTo(this.silkard$lastPosX, this.silkard$lastPosY, this.silkard$lastPosZ, this.silkard$lastYaw, this.silkard$lastPitch);

        org.bukkit.entity.Player player = this.getCraftPlayer();
        if (!this.silkard$hasMoved) {
            this.silkard$lastPosX = this.player.getX();
            this.silkard$lastPosY = this.player.getY();
            this.silkard$lastPosZ = this.player.getZ();
            this.silkard$lastYaw = this.player.getYRot();
            this.silkard$lastPitch = this.player.getXRot();
            this.silkard$hasMoved = true;
        }
        org.bukkit.Location from = new org.bukkit.Location(player.getWorld(), this.silkard$lastPosX, this.silkard$lastPosY, this.silkard$lastPosZ, this.silkard$lastYaw, this.silkard$lastPitch);
        org.bukkit.Location to = player.getLocation().clone();
        to.setX(requestedX);
        to.setY(requestedY);
        to.setZ(requestedZ);
        to.setYaw(requestedYRot);
        to.setPitch(requestedXRot);

        double delta = Math.pow(this.silkard$lastPosX - to.getX(), 2) + Math.pow(this.silkard$lastPosY - to.getY(), 2) + Math.pow(this.silkard$lastPosZ - to.getZ(), 2);
        float deltaAngle = Math.abs(this.silkard$lastYaw - to.getYaw()) + Math.abs(this.silkard$lastPitch - to.getPitch());
        if ((delta > 1f / 256 || deltaAngle > 10f) && !((ContextServerGamePacketListenerImpl) (Object) this).silkard$isImmobilePlayer()) {
            this.silkard$lastPosX = to.getX();
            this.silkard$lastPosY = to.getY();
            this.silkard$lastPosZ = to.getZ();
            this.silkard$lastYaw = to.getYaw();
            this.silkard$lastPitch = to.getPitch();

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

    // CraftBukkit start - call InventoryClickEvent
    @Redirect(method = "handleContainerClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"))
    private void silkard$inventoryClick(AbstractContainerMenu menu, int slotNum, int button, ContainerInput input, Player player) {
        ServerPlayer serverPlayer = this.player;
        if (slotNum < -1 && slotNum != -999) {
            return;
        }
        if (input == ContainerInput.QUICK_CRAFT || menu.quickcraftStatus != 0) {
            menu.clicked(slotNum, button, input, player);
            return;
        }

        InventoryView view = ((ContextAbstractContainerMenu) menu).getBukkitView();
        if (view == null) {
            menu.clicked(slotNum, button, input, player);
            return;
        }

        org.bukkit.event.inventory.ClickType click = org.bukkit.event.inventory.ClickType.UNKNOWN;
        InventoryAction action = InventoryAction.UNKNOWN;

        switch (input) {
            case PICKUP -> {
                if (button == 0) {
                    click = org.bukkit.event.inventory.ClickType.LEFT;
                } else if (button == 1) {
                    click = org.bukkit.event.inventory.ClickType.RIGHT;
                }
                if (button == 0 || button == 1) {
                    action = InventoryAction.NOTHING;
                    if (slotNum == -999) {
                        if (!menu.getCarried().isEmpty()) {
                            action = button == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
                        }
                    } else if (slotNum < 0) {
                        action = InventoryAction.NOTHING;
                    } else {
                        Slot slot = menu.getSlot(slotNum);
                        if (slot != null) {
                            ItemStack clickedItem = slot.getItem();
                            ItemStack cursor = menu.getCarried();
                            if (clickedItem.isEmpty()) {
                                if (!cursor.isEmpty()) {
                                    action = button == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
                                }
                            } else if (slot.mayPickup(player)) {
                                if (cursor.isEmpty()) {
                                    action = button == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
                                } else if (slot.mayPlace(cursor)) {
                                    if (ItemStack.isSameItemSameComponents(clickedItem, cursor)) {
                                        int toPlace = button == 0 ? cursor.getCount() : 1;
                                        toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.getCount());
                                        toPlace = Math.min(toPlace, slot.container.getMaxStackSize() - clickedItem.getCount());
                                        if (toPlace == 1) {
                                            action = InventoryAction.PLACE_ONE;
                                        } else if (toPlace == cursor.getCount()) {
                                            action = InventoryAction.PLACE_ALL;
                                        } else if (toPlace < 0) {
                                            action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE;
                                        } else if (toPlace != 0) {
                                            action = InventoryAction.PLACE_SOME;
                                        }
                                    } else if (cursor.getCount() <= slot.getMaxStackSize()) {
                                        action = InventoryAction.SWAP_WITH_CURSOR;
                                    }
                                } else if (ItemStack.isSameItemSameComponents(cursor, clickedItem)) {
                                    if (clickedItem.getCount() >= 0 && clickedItem.getCount() + cursor.getCount() <= cursor.getMaxStackSize()) {
                                        action = InventoryAction.PICKUP_ALL;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            case QUICK_MOVE -> {
                if (button == 0) {
                    click = org.bukkit.event.inventory.ClickType.SHIFT_LEFT;
                } else if (button == 1) {
                    click = org.bukkit.event.inventory.ClickType.SHIFT_RIGHT;
                }
                if (button == 0 || button == 1) {
                    if (slotNum < 0) {
                        action = InventoryAction.NOTHING;
                    } else {
                        Slot slot = menu.getSlot(slotNum);
                        if (slot != null && slot.mayPickup(player) && slot.hasItem()) {
                            action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                        } else {
                            action = InventoryAction.NOTHING;
                        }
                    }
                }
            }
            case SWAP -> {
                if ((button >= 0 && button < 9) || button == 40) {
                    click = button == 40 ? org.bukkit.event.inventory.ClickType.SWAP_OFFHAND : org.bukkit.event.inventory.ClickType.NUMBER_KEY;
                    Slot clickedSlot = slotNum >= 0 ? menu.getSlot(slotNum) : null;
                    if (clickedSlot != null && clickedSlot.mayPickup(player)) {
                        ItemStack hotbar = serverPlayer.getInventory().getItem(button);
                        boolean canCleanSwap = hotbar.isEmpty() || (clickedSlot.container == serverPlayer.getInventory() && clickedSlot.mayPlace(hotbar));
                        if (clickedSlot.hasItem()) {
                            action = canCleanSwap ? InventoryAction.HOTBAR_SWAP : InventoryAction.HOTBAR_MOVE_AND_READD;
                        } else if (!hotbar.isEmpty() && clickedSlot.mayPlace(hotbar)) {
                            action = InventoryAction.HOTBAR_SWAP;
                        } else {
                            action = InventoryAction.NOTHING;
                        }
                    } else {
                        action = InventoryAction.NOTHING;
                    }
                }
            }
            case CLONE -> {
                if (button == 2) {
                    click = org.bukkit.event.inventory.ClickType.MIDDLE;
                    Slot slot = slotNum >= 0 ? menu.getSlot(slotNum) : null;
                    if (slot != null && slot.hasItem() && player.getAbilities().instabuild && menu.getCarried().isEmpty()) {
                        action = InventoryAction.CLONE_STACK;
                    } else {
                        action = InventoryAction.NOTHING;
                    }
                } else {
                    click = org.bukkit.event.inventory.ClickType.UNKNOWN;
                    action = InventoryAction.UNKNOWN;
                }
            }
            case THROW -> {
                if (slotNum >= 0) {
                    Slot slot = menu.getSlot(slotNum);
                    boolean droppable = slot != null && slot.hasItem() && slot.mayPickup(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR);
                    if (button == 0) {
                        click = org.bukkit.event.inventory.ClickType.DROP;
                        action = droppable ? InventoryAction.DROP_ONE_SLOT : InventoryAction.NOTHING;
                    } else if (button == 1) {
                        click = org.bukkit.event.inventory.ClickType.CONTROL_DROP;
                        action = droppable ? InventoryAction.DROP_ALL_SLOT : InventoryAction.NOTHING;
                    }
                } else {
                    click = button == 1 ? org.bukkit.event.inventory.ClickType.RIGHT : org.bukkit.event.inventory.ClickType.LEFT;
                    action = InventoryAction.NOTHING;
                }
            }
            case PICKUP_ALL -> {
                click = org.bukkit.event.inventory.ClickType.DOUBLE_CLICK;
                action = InventoryAction.NOTHING;
                if (slotNum >= 0 && !menu.getCarried().isEmpty()) {
                    ItemStack cursor = menu.getCarried();
                    if (view.getTopInventory().contains(CraftItemType.minecraftToBukkit(cursor.getItem())) || view.getBottomInventory().contains(CraftItemType.minecraftToBukkit(cursor.getItem()))) {
                        action = InventoryAction.COLLECT_TO_CURSOR;
                    }
                }
            }
            default -> {
            }
        }

        InventoryClickEvent event = click == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                ? new InventoryClickEvent(view, view.getSlotType(slotNum), slotNum, click, action, button)
                : new InventoryClickEvent(view, view.getSlotType(slotNum), slotNum, click, action);

        org.bukkit.inventory.Inventory top = view.getTopInventory();
        if (slotNum == 0 && top instanceof CraftingInventory crafting) {
            org.bukkit.inventory.Recipe recipe = crafting.getRecipe();
            if (recipe != null) {
                event = click == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                        ? new CraftItemEvent(recipe, view, view.getSlotType(slotNum), slotNum, click, action, button)
                        : new CraftItemEvent(recipe, view, view.getSlotType(slotNum), slotNum, click, action);
            }
        }
        if (slotNum == 3 && top instanceof SmithingInventory smithing) {
            if (smithing.getResult() != null) {
                event = click == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                        ? new SmithItemEvent(view, view.getSlotType(slotNum), slotNum, click, action, button)
                        : new SmithItemEvent(view, view.getSlotType(slotNum), slotNum, click, action);
            }
        }

        AbstractContainerMenu oldMenu = menu;
        serverPlayer.level().getCraftServer().getPluginManager().callEvent(event);
        if (serverPlayer.containerMenu != oldMenu) {
            // SPIGOT-1224: the menu was replaced while the event was handled
            return;
        }

        if (event.getResult() == Event.Result.ALLOW || event.getResult() == Event.Result.DEFAULT) {
            menu.clicked(slotNum, button, input, player);
        } else {
            this.silkard$resyncSlot(serverPlayer, menu, slotNum, action);
        }

        if (event instanceof CraftItemEvent || event instanceof SmithItemEvent) {
            menu.sendAllDataToRemote();
        }
    }

    private void silkard$resyncSlot(ServerPlayer player, AbstractContainerMenu menu, int slotNum, InventoryAction action) {
        switch (action) {
            case PICKUP_ALL, MOVE_TO_OTHER_INVENTORY, HOTBAR_MOVE_AND_READD, HOTBAR_SWAP, COLLECT_TO_CURSOR, UNKNOWN -> menu.sendAllDataToRemote();
            case PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> {
                ((ContextAbstractContainerMenu) menu).broadcastCarriedItem();
                this.silkard$sendSlot(player, menu, slotNum);
            }
            case DROP_ALL_SLOT, DROP_ONE_SLOT -> this.silkard$sendSlot(player, menu, slotNum);
            case DROP_ALL_CURSOR, DROP_ONE_CURSOR, CLONE_STACK -> ((ContextAbstractContainerMenu) menu).broadcastCarriedItem();
            default -> {
            }
        }
    }

    private void silkard$sendSlot(ServerPlayer player, AbstractContainerMenu menu, int slotNum) {
        if (slotNum < 0) {
            return;
        }
        Slot slot = menu.getSlot(slotNum);
        if (slot != null) {
            player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), slotNum, slot.getItem()));
        }
    }
    // CraftBukkit end
}