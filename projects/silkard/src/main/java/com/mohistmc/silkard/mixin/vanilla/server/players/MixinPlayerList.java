package com.mohistmc.silkard.mixin.vanilla.server.players;

import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.server.level.ContextServerPlayer;
import com.mohistmc.silkard.injected.server.players.ContextPlayerList;
import com.mohistmc.silkard.injected.world.inventory.ContextAbstractContainerMenu;
import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.FileUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/4/2 19:11
 */
// priority 900 < fabric 1000：让 fabric 的 @Inject 能注入被 @Overwrite 合并的 placeNewPlayer/reloadResources
@Mixin(value = PlayerList.class, priority = 900)
public abstract class MixinPlayerList implements ContextPlayerList {

    // @formatter:off
    @Shadow public @Nullable abstract ServerPlayer getPlayer(UUID uuid);
    @Shadow @Final private MinecraftServer server;
    @Shadow @Mutable @Final public List<ServerPlayer> players;
    @Shadow @Final private Map<UUID, ServerPlayer> playersByUUID;
    @Shadow @Final private Map<UUID, ServerStatsCounter> stats;
    @Shadow @Final private UserBanList bans;
    @Shadow @Final private IpBanList ipBans;
    @Shadow @Final private PlayerDataStorage playerIo;
    @Shadow private int sendAllPlayerInfoIn;
    // @formatter:on

    // CraftBukkit start - CraftServer construction (PlayerList.patch ctor hunk)
    @Inject(method = "<init>", at = @At("TAIL"))
    private void silkard$bukkitServerInit(MinecraftServer server, net.minecraft.core.LayeredRegistryAccess<net.minecraft.server.RegistryLayer> registries, net.minecraft.world.level.storage.PlayerDataStorage playerIo, net.minecraft.server.notifications.NotificationService notificationService, CallbackInfo ci) {
        if (((com.mohistmc.silkard.injected.server.ContextMinecraftServer) this.server).silkard$server() == null) {
            ((com.mohistmc.silkard.injected.server.ContextMinecraftServer) this.server).silkard$setServer(new org.bukkit.craftbukkit.CraftServer((net.minecraft.server.dedicated.DedicatedServer) (Object) this.server, (PlayerList) (Object) this));
            ((com.mohistmc.silkard.injected.server.ContextMinecraftServer) this.server).silkard_console(org.bukkit.craftbukkit.command.ColouredConsoleSender.getInstance());
            // CraftServer 构造函数已调用 Bukkit.setServer，这里再调会触发单例重定义异常
        }
        this.players = new CopyOnWriteArrayList<>();
    }
    // CraftBukkit end

    @Override
    public ServerStatsCounter getPlayerStats(ServerPlayer entityhuman) {
        ServerStatsCounter serverstatscounter = entityhuman.getStats();
        return serverstatscounter == null ? this.getPlayerStats(entityhuman.nameAndId()) : serverstatscounter;
    }

    @Override
    public ServerStatsCounter getPlayerStats(NameAndId gameprofile) {
        ServerPlayer entityhuman = this.getPlayer(gameprofile.id());
        ServerStatsCounter serverstatscounter = entityhuman == null ? null : (ServerStatsCounter) entityhuman.getStats();
        if (serverstatscounter == null) {
            Path path = this.locateStatsFile(gameprofile);

            return new ServerStatsCounter(this.server, path);
        }
        return serverstatscounter;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - stats come from the player instance instead of a PlayerList map
     */
    @Overwrite
    public ServerStatsCounter getPlayerStats(net.minecraft.world.entity.player.Player player) {
        return this.getPlayerStats(player.nameAndId());
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - lazily create and cache a PlayerAdvancements, keeping it attached to the player
     */
    @Overwrite
    public PlayerAdvancements getPlayerAdvancements(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerAdvancements playeradvancements = this.advancements.get(uuid);
        if (playeradvancements == null) {
            Path path = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(String.valueOf(uuid));
            playeradvancements = new PlayerAdvancements(this.server.getFixerUpper(), (PlayerList) (Object) this, this.server.getAdvancements(), path, player);
            this.advancements.put(uuid, playeradvancements);
        }
        playeradvancements.setPlayer(player);
        return playeradvancements;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - honour the persistent flag before saving
     */
    @Overwrite
    public void save(ServerPlayer player) {
        if (!player.getBukkitEntity().isPersistent()) {
            return;
        }
        this.playerIo.save(player);
        ServerStatsCounter serverstatscounter = player.getStats();
        if (serverstatscounter != null) {
            serverstatscounter.save();
        }
        PlayerAdvancements playeradvancements = player.getAdvancements();
        if (playeradvancements != null) {
            playeradvancements.save();
        }
    }

    @Unique
    private Path locateStatsFile(NameAndId gameProfile) {
        // CraftBukkit end
        Path path = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Path path1 = path.resolve(String.valueOf(gameProfile.id()) + ".json");

        if (Files.exists(path1, new LinkOption[0])) {
            return path1;
        } else {
            String s = gameProfile.name() + ".json";

            if (FileUtil.isValidPathSegment(s)) {
                Path path2 = path.resolve(s);

                if (Files.isRegularFile(path2, new LinkOption[0])) {
                    try {
                        return Files.move(path2, path1);
                    } catch (IOException ioexception) {
                        PlayerList.LOGGER.warn("Failed to copy file {} to {}", s, path1);
                        return path2;
                    }
                }
            }

            return path1;
        }
    }

    // CraftBukkit start - CraftServer accessor for PlayerList-side event dispatch
    @Override
    public CraftServer getCraftServer() {
        return ((com.mohistmc.silkard.injected.server.ContextMinecraftServer) this.server).silkard$server();
    }
    // CraftBukkit end

    // CraftBukkit start - PlayerLoginEvent: ban/whitelist/ipban/full checks become event disallows
    @Override
    public ServerPlayer canPlayerLogin(ServerLoginPacketListenerImpl serverloginpacketlistenerimpl, GameProfile gameprofile) {
        NameAndId nameAndId = new NameAndId(gameprofile);
        UUID uuid = gameprofile.id();
        Set<ServerPlayer> set = Sets.newIdentityHashSet();

        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.getUUID().equals(uuid)) {
                set.add(serverplayer);
            }
        }

        for (ServerPlayer serverplayer2 : set) {
            ((PlayerList) (Object) this).save(serverplayer2);
            serverplayer2.connection.disconnect(PlayerList.DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
        }

        Connection connection = ((com.mohistmc.silkard.injected.server.network.ContextServerLoginPacketListenerImpl) serverloginpacketlistenerimpl).silkard$connection();
        SocketAddress address = connection.getRemoteAddress();

        ServerPlayer entity = new ServerPlayer(this.server, this.server.getLevel(Level.OVERWORLD), gameprofile, ClientInformation.createDefault());
        ((ContextServerPlayer) entity).silkard$transferCookieConnection((CraftPlayer.TransferCookieConnection) (Object) serverloginpacketlistenerimpl);
        org.bukkit.entity.Player player = entity.getBukkitEntity();
        PlayerLoginEvent event = new PlayerLoginEvent(player, ((com.mohistmc.silkard.injected.network.ContextConnection) connection).silkard$hostname(), ((java.net.InetSocketAddress) address).getAddress());

        if (this.bans.isBanned(nameAndId)) {
            UserBanListEntry userbanlistentry = (UserBanListEntry) this.bans.get(nameAndId);
            MutableComponent mutablecomponent = Component.translatable("multiplayer.disconnect.banned.reason", userbanlistentry.getReasonMessage());
            if (userbanlistentry.getExpires() != null) {
                mutablecomponent.append(Component.translatable("multiplayer.disconnect.banned.expiration", PlayerList.BAN_DATE_FORMAT.format(userbanlistentry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(mutablecomponent));
        } else if (!((PlayerList) (Object) this).isWhiteListed(nameAndId)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, CraftChatMessage.fromComponent(Component.translatable("multiplayer.disconnect.not_whitelisted")));
        } else if (this.ipBans.isBanned(address)) {
            IpBanListEntry ipbanlistentry = this.ipBans.get(address);
            MutableComponent mutablecomponent1 = Component.translatable("multiplayer.disconnect.banned_ip.reason", ipbanlistentry.getReasonMessage());
            if (ipbanlistentry.getExpires() != null) {
                mutablecomponent1.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", PlayerList.BAN_DATE_FORMAT.format(ipbanlistentry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(mutablecomponent1));
        } else if (this.players.size() >= ((PlayerList) (Object) this).getMaxPlayers() && !((PlayerList) (Object) this).canBypassPlayerLimit(nameAndId)) {
            event.disallow(PlayerLoginEvent.Result.KICK_FULL, "The server is full");
        }

        this.getCraftServer().getPluginManager().callEvent(event);
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            ((com.mohistmc.silkard.injected.server.network.ContextServerLoginPacketListenerImpl) serverloginpacketlistenerimpl).silkard$setKickMessage(Component.literal(event.getKickMessage()));
            return null;
        }
        return entity;
    }

    @Override
    public boolean disconnectAllPlayersWithProfile(UUID playerId, ServerPlayer player) {
        return player == null;
    }

    @Override
    public Optional<CompoundTag> silkard$loadPlayerData(ServerPlayer player) {
        NameAndId nameAndId = player.nameAndId();
        UUID uuid = this.server.getWorldData().getSinglePlayerUUID();
        if (this.server.isSingleplayerOwner(nameAndId) && uuid != null) {
            return this.playerIo.load(new NameAndId(uuid, "<singleplayer owner>"));
        } else {
            return this.playerIo.load(nameAndId);
        }
    }
    // CraftBukkit end

    // CraftBukkit start - split reloadResources so recipes can be re-sent after addRecipe
    @Shadow
    @Final
    private Map<UUID, PlayerAdvancements> advancements;

    @Shadow
    @Final
    private LayeredRegistryAccess<RegistryLayer> registries;

    @Shadow
    public abstract void broadcastAll(Packet<?> packet);

    /**
     * @author Mgazul
     * @reason CraftBukkit - extract recipe resending into reloadRecipes
     */
    @Overwrite
    public void reloadResources() {
        for (ServerPlayer player : this.players) {
            player.getAdvancements().reload(this.server.getAdvancements());
            player.getAdvancements().flushDirty(player, false);
        }

        this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
        this.reloadRecipes();
    }

    @Override
    public void reloadRecipes() {
        RecipeManager recipemanager = this.server.getRecipeManager();
        ClientboundUpdateRecipesPacket clientboundupdaterecipespacket = new ClientboundUpdateRecipesPacket(recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes());

        for (ServerPlayer player : this.players) {
            player.connection.send(clientboundupdaterecipespacket);
            player.getRecipeBook().sendInitialRecipeBook(player);
        }
    }
    // CraftBukkit end

    /**
     * @author Mgazul
     * @reason CraftBukkit - PlayerJoinEvent with plugin-controlled join message, visibility-aware player list
     */
    @Overwrite
    public void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        NameAndId nameAndId = player.nameAndId();
        net.minecraft.server.players.UserNameToIdResolver usernametoidresolver = this.server.services().nameToIdCache();
        String s = usernametoidresolver.get(nameAndId.id()).map(NameAndId::name).orElse(nameAndId.name());
        // CraftBukkit start - better rename detection
        String lastKnownName = player.getBukkitEntity().getLastKnownName();
        if (lastKnownName != null) {
            s = lastKnownName;
        }
        // CraftBukkit end
        usernametoidresolver.add(nameAndId);
        ServerLevel serverlevel = player.level();
        String s1 = connection.getLoggableAddress(this.server.logIPs());

        // CraftBukkit - moved message to after join
        LevelData leveldata = serverlevel.getLevelData();
        ServerGamePacketListenerImpl servergamepacketlistenerimpl = new ServerGamePacketListenerImpl(this.server, connection, player, cookie);

        connection.setupInboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess()), servergamepacketlistenerimpl), servergamepacketlistenerimpl);
        servergamepacketlistenerimpl.suspendFlushing();
        GameRules gamerules = serverlevel.getGameRules();

        boolean flag = gamerules.get(GameRules.IMMEDIATE_RESPAWN);
        boolean flag1 = gamerules.get(GameRules.REDUCED_DEBUG_INFO);
        boolean flag2 = gamerules.get(GameRules.LIMITED_CRAFTING);

        servergamepacketlistenerimpl.send(new ClientboundLoginPacket(player.getId(), leveldata.isHardcore(), this.server.levelKeys(), ((PlayerList) (Object) this).getMaxPlayers(), ((PlayerList) (Object) this).getViewDistance(), ((PlayerList) (Object) this).getSimulationDistance(), flag1, !flag, flag2, player.createCommonSpawnInfo(serverlevel), this.server.usesAuthentication(), this.server.enforceSecureProfile()));
        player.getBukkitEntity().sendSupportedChannels(); // CraftBukkit
        servergamepacketlistenerimpl.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
        servergamepacketlistenerimpl.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        servergamepacketlistenerimpl.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
        RecipeManager recipemanager = this.server.getRecipeManager();

        servergamepacketlistenerimpl.send(new ClientboundUpdateRecipesPacket(recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes()));
        ((PlayerList) (Object) this).sendPlayerPermissionLevel(player);
        player.getStats().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        ((PlayerList) (Object) this).updateEntireScoreboard(serverlevel.getScoreboard(), player);
        this.server.invalidateStatus();
        MutableComponent mutablecomponent;

        if (player.getGameProfile().name().equalsIgnoreCase(s)) {
            mutablecomponent = Component.translatable("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutablecomponent = Component.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), s);
        }

        // CraftBukkit start
        mutablecomponent.withStyle(net.minecraft.ChatFormatting.YELLOW);
        String joinMessage = CraftChatMessage.fromComponent(mutablecomponent);

        servergamepacketlistenerimpl.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        ServerStatus serverstatus = this.server.getStatus();

        if (serverstatus != null && !cookie.transferred()) {
            player.sendServerStatus(serverstatus);
        }

        CraftPlayer bukkitPlayer = player.getBukkitEntity();

        // Ensure that player inventory is populated with its viewer
        ((ContextAbstractContainerMenu) player.containerMenu).transferTo(player.containerMenu, bukkitPlayer);

        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(bukkitPlayer, joinMessage);
        this.getCraftServer().getPluginManager().callEvent(playerJoinEvent);

        if (!player.connection.isAcceptingMessages()) {
            return;
        }

        joinMessage = playerJoinEvent.getJoinMessage();

        if (joinMessage != null && joinMessage.length() > 0) {
            for (Component line : CraftChatMessage.fromString(joinMessage)) {
                ((PlayerList) (Object) this).broadcastSystemMessage(line, false);
            }
        }
        // CraftBukkit end

        // CraftBukkit start - sendAll above replaced with this loop
        ClientboundPlayerInfoUpdatePacket packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));

        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayer serverplayer1 = this.players.get(i);

            if (serverplayer1.getBukkitEntity().canSee(bukkitPlayer)) {
                serverplayer1.connection.send(packet);
            }

            if (!bukkitPlayer.canSee(serverplayer1.getBukkitEntity())) {
                continue;
            }

            player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(serverplayer1)));
        }
        ((ContextServerPlayer) player).silkard$setSentListPacket(true);
        // CraftBukkit end

        // CraftBukkit - vanilla registers the player with PlayerList before sendLevelInfo/addNewPlayer (was missing in this @Overwrite)
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);

        ((ContextEntity) player).refreshEntityData(player); // CraftBukkit - SPIGOT-7218
        ((PlayerList) (Object) this).sendLevelInfo(player, player.level()); // CraftBukkit - SPIGOT-8149 per-world time sync

        // CraftBukkit start - only add if the player wasn't moved in the event
        if (player.level() == serverlevel && !serverlevel.players().contains(player)) {
            serverlevel.addNewPlayer(player);
            this.server.getCustomBossEvents().onPlayerConnect(player);
        }

        serverlevel = player.level(); // CraftBukkit - update in case join event changed it
        // CraftBukkit end
        ((PlayerList) (Object) this).sendActivePlayerEffects(player);
        player.sendPostEffects();
        player.initInventoryMenu();
        this.server.notificationManager().playerJoined(player);
        servergamepacketlistenerimpl.resumeFlushing();
        // CraftBukkit - moved from above, added world
        PlayerList.LOGGER.info("{}[{}] logged in with entity id {} at ([{}]{}, {}, {})", player.getPlainTextName(), s1, player.getId(), ((ServerLevelData) serverlevel.getLevelData()).getLevelName(), player.getX(), player.getY(), player.getZ());
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - PlayerQuitEvent before the final save, visibility-aware player list removal
     */
    @Overwrite
    public void remove(ServerPlayer player) {
        ServerLevel serverlevel = player.level();

        player.awardStat(Stats.LEAVE_GAME);

        // CraftBukkit start - quitting must be before the final save, plugins may modify data (SPIGOT-5799, SPIGOT-6145)
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }

        String leaveMessage = ((ContextServerPlayer) player).silkard$kickLeaveMessage();
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(player.getBukkitEntity(), leaveMessage != null ? leaveMessage : "\u00A7e" + player.getScoreboardName() + " left the game");
        this.getCraftServer().getPluginManager().callEvent(playerQuitEvent);
        player.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());

        player.doTick(); // SPIGOT-924
        // CraftBukkit end

        ((PlayerList) (Object) this).save(player);
        if (player.isPassenger()) {
            Entity entity = player.getRootVehicle();

            if (entity.hasExactlyOnePlayerPassenger()) {
                PlayerList.LOGGER.debug("Removing player mount");
                player.stopRiding();
                entity.getPassengersAndSelf().forEach((entity1) -> {
                    ((ContextEntity) entity1).setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER, EntityRemoveEvent.Cause.PLAYER_QUIT);
                });
            }
        }

        player.unRide();

        for (ThrownEnderpearl thrownenderpearl : player.getEnderPearls()) {
            ((ContextEntity) thrownenderpearl).setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER, EntityRemoveEvent.Cause.PLAYER_QUIT);
        }
        // CraftBukkit - return value ignored (vanilla signature kept)

        serverlevel.removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        player.getAdvancements().clearTriggers();
        this.players.remove(player);
        this.server.getCustomBossEvents().onPlayerDisconnect(player);
        UUID uuid = player.getUUID();

        if (this.playersByUUID.get(uuid) == player) {
            this.playersByUUID.remove(uuid);
            this.server.notificationManager().playerLeft(player);
        }

        // CraftBukkit start - only send to players that can see the leaving player
        ClientboundPlayerInfoRemovePacket packet = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer serverplayer2 = this.players.get(i);

            if (serverplayer2.getBukkitEntity().canSee(player.getBukkitEntity())) {
                serverplayer2.connection.send(packet);
            } else {
                serverplayer2.getBukkitEntity().onEntityRemove(player);
            }
        }
        this.getCraftServer().getScoreboardManager().removePlayer(player.getBukkitEntity());
        // CraftBukkit end
    }

    @Inject(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void silkard$recalculatePermissions(ServerPlayer player, CallbackInfo ci) {
        player.getBukkitEntity().recalculatePermissions();
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - receivers must be able to see the source entity
     */
    @Overwrite
    public void broadcast(net.minecraft.world.entity.player.Player except, double x, double y, double z, double radius, net.minecraft.resources.ResourceKey<Level> dimension, Packet<?> packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayer serverplayer = this.players.get(i);

            if (except != null && !serverplayer.getBukkitEntity().canSee(((ContextServerPlayer) except).getBukkitEntity())) {
                continue;
            }

            if (serverplayer != except && serverplayer.level().dimension() == dimension) {
                double dx = x - serverplayer.getX();
                double dy = y - serverplayer.getY();
                double dz = z - serverplayer.getZ();

                if (dx * dx + dy * dy + dz * dz < radius * radius) {
                    serverplayer.connection.send(packet);
                }
            }
        }
    }

    // CraftBukkit start - world/entity limited broadcast
    @Override
    public void broadcastAll(Packet<?> packet, net.minecraft.world.entity.player.Player player) {
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayer serverplayer = this.players.get(i);
            if (player != null && !serverplayer.getBukkitEntity().canSee(((ContextServerPlayer) player).getBukkitEntity())) {
                continue;
            }
            serverplayer.connection.send(packet);
        }
    }

    @Override
    public void broadcastAll(Packet<?> packet, Level level) {
        for (int i = 0; i < level.players().size(); ++i) {
            ((ServerPlayer) level.players().get(i)).connection.send(packet);
        }
    }

    @Override
    public void broadcastMessage(Component[] components) {
        for (Component component : components) {
            ((PlayerList) (Object) this).broadcastSystemMessage(component, false);
        }
    }
    // CraftBukkit end

    /**
     * @author Mgazul
     * @reason CraftBukkit - per-receiver visibility filtering for latency updates
     */
    @Overwrite
    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            for (int i = 0; i < this.players.size(); ++i) {
                final ServerPlayer target = this.players.get(i);

                target.connection.send(new ClientboundPlayerInfoUpdatePacket(java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players.stream().filter(input -> target.getBukkitEntity().canSee(input.getBukkitEntity())).collect(Collectors.toList())));
            }
            this.sendAllPlayerInfoIn = 0;
        }
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - custom shutdown message
     */
    @Overwrite
    public void removeAll() {
        for (ServerPlayer player : this.players) {
            player.connection.disconnect(CraftChatMessage.fromStringOrEmpty(((ContextMinecraftServer) this.server).silkard$server().getShutdownMessage()));
        }
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - per-player weather on level info
     */
    @Overwrite
    public void sendLevelInfo(ServerPlayer player, ServerLevel level) {
        WorldBorder worldborder = level.getWorldBorder();

        player.connection.send(new ClientboundInitializeBorderPacket(worldborder));
        player.connection.send(level.clockManager().createFullSyncPacket());
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        if (level.isRaining()) {
            ((ContextServerPlayer) player).setPlayerWeather(org.bukkit.WeatherType.DOWNFALL, false);
            ((ContextServerPlayer) player).updateWeather(-level.rainLevel, level.rainLevel, -level.thunderLevel, level.thunderLevel);
        }

        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0F));
        this.server.tickRateManager().updateJoiningPlayer(player);
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - scaled health, metadata sync, gamerule driven packets
     */
    @Overwrite
    public void sendAllPlayerInfo(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        player.getBukkitEntity().updateScaledHealth();
        ((ContextEntity) player).refreshEntityData(player);
        player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));

        int i = player.level().getGameRules().get(GameRules.REDUCED_DEBUG_INFO) ? 22 : 23;
        player.connection.send(new ClientboundEntityEventPacket(player, (byte) i));
        float immediateRespawn = player.level().getGameRules().get(GameRules.IMMEDIATE_RESPAWN) ? 1.0F : 0.0F;
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, immediateRespawn));
    }

    // CraftBukkit start - PlayerRespawnEvent aware respawn (named separately to keep fabric's single "respawn" inject happy)
    @Override
    public ServerPlayer silkard$respawnWithReason(ServerPlayer serverPlayer, boolean keepAllPlayerData, Entity.RemovalReason removalReason, PlayerRespawnEvent.RespawnReason reason) {
        return this.silkard$respawnWithLocation(serverPlayer, keepAllPlayerData, removalReason, reason, null);
    }

    @Override
    public ServerPlayer silkard$respawnWithLocation(ServerPlayer serverPlayer, boolean keepAllPlayerData, Entity.RemovalReason removalReason, PlayerRespawnEvent.RespawnReason reason, Location location) {
        ServerLevel fromWorld = serverPlayer.level();
        serverPlayer.stopRiding();
        this.players.remove(serverPlayer);
        fromWorld.removePlayerImmediately(serverPlayer, removalReason);

        ServerPlayer serverplayer1 = serverPlayer;
        serverPlayer.wonGame = false;

        serverplayer1.connection = serverPlayer.connection;
        serverplayer1.restoreFrom(serverPlayer, keepAllPlayerData);
        serverplayer1.setId(serverPlayer.getId());
        serverplayer1.setMainArm(serverPlayer.getMainArm());

        for (String s : serverPlayer.entityTags()) {
            serverplayer1.addTag(s);
        }

        TeleportTransition teleporttransition;
        if (location == null) {
            teleporttransition = ((ContextServerPlayer) serverPlayer).findRespawnPositionAndUseSpawnBlock(!keepAllPlayerData, TeleportTransition.DO_NOTHING, reason);
            if (!keepAllPlayerData) {
                ((ContextServerPlayer) serverPlayer).reset(); // SPIGOT-4785
            }
        } else {
            teleporttransition = new TeleportTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3D(location), Vec3.ZERO, location.getYaw(), location.getPitch(), Set.of(), TeleportTransition.DO_NOTHING);
        }

        ServerLevel serverlevel = teleporttransition.newLevel();
        ((ContextServerPlayer) serverplayer1).spawnIn(serverlevel, keepAllPlayerData);
        serverplayer1.unsetRemoved();
        serverplayer1.setShiftKeyDown(false);
        Vec3 vec3 = teleporttransition.position();
        ((ContextServerPlayer) serverplayer1).forceSetPositionRotation(vec3.x, vec3.y, vec3.z, teleporttransition.yRot(), teleporttransition.xRot());

        if (teleporttransition.missingRespawnBlock()) {
            serverplayer1.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            ((ContextServerPlayer) serverplayer1).setRespawnPosition(null, false, PlayerSpawnChangeEvent.Cause.RESET); // SPIGOT-5988
        }

        byte b0 = (byte) (keepAllPlayerData ? 1 : 0);
        ServerLevel serverlevel1 = serverplayer1.level();
        LevelData leveldata = serverlevel1.getLevelData();

        serverplayer1.connection.send(new ClientboundRespawnPacket(serverplayer1.createCommonSpawnInfo(serverlevel1), b0));
        serverplayer1.connection.teleport(CraftLocation.toBukkit(serverplayer1.position(), serverlevel1.getWorld(), serverplayer1.getYRot(), serverplayer1.getXRot()));
        serverplayer1.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverlevel.getRespawnData()));
        serverplayer1.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
        serverplayer1.connection.send(new ClientboundSetExperiencePacket(serverplayer1.experienceProgress, serverplayer1.totalExperience, serverplayer1.experienceLevel));
        ((PlayerList) (Object) this).sendActivePlayerEffects(serverplayer1);
        ((PlayerList) (Object) this).sendLevelInfo(serverplayer1, serverlevel);
        ((PlayerList) (Object) this).sendPlayerPermissionLevel(serverplayer1);
        if (!((com.mohistmc.silkard.injected.server.network.ContextServerGamePacketListenerImpl) serverPlayer.connection).isDisconnected()) {
            serverlevel.addRespawnedPlayer(serverplayer1);
            this.players.add(serverplayer1);
            this.playersByUUID.put(serverplayer1.getUUID(), serverplayer1);
        }
        serverplayer1.setHealth(serverplayer1.getHealth());
        ServerPlayer.RespawnConfig serverplayer_respawnconfig = serverplayer1.getRespawnConfig();

        if (!keepAllPlayerData && serverplayer_respawnconfig != null) {
            LevelData.RespawnData respawnData = serverplayer_respawnconfig.respawnData();
            ServerLevel serverlevel2 = this.server.getLevel(respawnData.dimension());

            if (serverlevel2 != null) {
                BlockPos blockpos = respawnData.pos();
                net.minecraft.world.level.block.state.BlockState blockstate = serverlevel2.getBlockState(blockpos);

                if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
                    serverplayer1.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, (double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ(), 1.0F, 1.0F, serverlevel.getRandom().nextLong()));
                }
            }
        }

        ((PlayerList) (Object) this).sendAllPlayerInfo(serverPlayer);
        serverPlayer.onUpdateAbilities();
        for (net.minecraft.world.effect.MobEffectInstance mobEffect : serverPlayer.getActiveEffects()) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(serverPlayer.getId(), mobEffect, false));
        }

        ((ContextServerPlayer) serverPlayer).silkard$triggerDimensionChangeTriggers(serverlevel);

        if (fromWorld != serverlevel) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(serverPlayer.getBukkitEntity(), fromWorld.getWorld());
            ((ContextMinecraftServer) this.server).silkard$server().getPluginManager().callEvent(event);
        }

        if (((com.mohistmc.silkard.injected.server.network.ContextServerGamePacketListenerImpl) serverPlayer.connection).isDisconnected()) {
            ((PlayerList) (Object) this).save(serverPlayer);
        }

        return serverplayer1;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - vanilla 3-arg respawn delegates to the event aware overload
     */
    @Overwrite
    public ServerPlayer respawn(ServerPlayer serverPlayer, boolean keepAllPlayerData, Entity.RemovalReason removalReason) {
        return this.silkard$respawnWithReason(serverPlayer, keepAllPlayerData, removalReason, PlayerRespawnEvent.RespawnReason.DEATH);
    }
    // CraftBukkit end
}