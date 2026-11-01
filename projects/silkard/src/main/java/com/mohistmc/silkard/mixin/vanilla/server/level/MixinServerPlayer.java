package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.level.ContextServerPlayer;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import net.minecraft.world.level.portal.TeleportTransition;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import com.mohistmc.silkard.injected.world.level.portal.ContextTeleportTransition;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PositionMoveRotation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/5/13 17:07
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player implements ContextServerPlayer {

    // @formatter:off
    @Shadow private int containerCounter;
    @Shadow public void nextContainerCounter() {}
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final public net.minecraft.server.level.ServerPlayerGameMode gameMode;
    @Shadow private int lastSentExp;
    @Shadow private net.minecraft.world.phys.Vec3 enteredNetherPosition;
    @Shadow private static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(ServerLevel level, ServerPlayer.RespawnConfig config, boolean consumeSpawnBlock) { throw new AssertionError(); }
    // @formatter:on

    @Unique
    private CraftEntity silkard$serverBukkitEntity;

    // CraftBukkit start
    public CraftPlayer.TransferCookieConnection transferCookieConnection;
    public String displayName;
    public Component listName;
    public int listOrder = 0;
    public org.bukkit.Location compassTarget;
    public int newExp = 0;
    public int newLevel = 0;
    public int newTotalExp = 0;
    public boolean keepLevel = false;
    public double maxHealthCache;
    public boolean joining = true;
    public boolean sentListPacket = false;
    public String kickLeaveMessage = null; // SPIGOT-3034: Forward leave message to PlayerQuitEvent
    // CraftBukkit end
    public PlayerSpawnChangeEvent.Cause respawnPositionCause = PlayerSpawnChangeEvent.Cause.UNKNOWN;
    public boolean initialized = false;

    public MixinServerPlayer(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Override
    public void setRespawnPosition(ServerPlayer.RespawnConfig respawnConfig, boolean showMessage, PlayerSpawnChangeEvent.Cause cause) {
        Location newSpawn = null;
        boolean forced = false;
        if (respawnConfig != null) {
            net.minecraft.world.level.storage.LevelData.RespawnData respawnData = respawnConfig.respawnData();
            ServerLevel newWorld = this.server.getLevel(respawnData.dimension());
            if (newWorld != null) {
                newSpawn = CraftLocation.toBukkit(respawnData.pos(), newWorld.getWorld(), respawnData.yaw(), respawnData.pitch());
            }
            forced = respawnConfig.forced();
        }

        PlayerSpawnChangeEvent event = new PlayerSpawnChangeEvent(this.getBukkitEntity(), newSpawn, forced, cause);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        newSpawn = event.getNewSpawn();

        if (newSpawn != null) {
            respawnConfig = new ServerPlayer.RespawnConfig(net.minecraft.world.level.storage.LevelData.RespawnData.of(((org.bukkit.craftbukkit.CraftWorld) newSpawn.getWorld()).getHandle().dimension(), net.minecraft.core.BlockPos.containing(newSpawn.getX(), newSpawn.getY(), newSpawn.getZ()), newSpawn.getYaw(), newSpawn.getPitch()), event.isForced());
        } else {
            respawnConfig = null;
        }
        ((ServerPlayer) (Object) this).setRespawnPosition(respawnConfig, showMessage);
    }

    @Override
    public int nextContainerCounterInt() {
        nextContainerCounter();
        return containerCounter;
    }

    @Override
    public void silkard$transferCookieConnection(CraftPlayer.TransferCookieConnection transferCookieConnection) {
        this.transferCookieConnection = transferCookieConnection;
    }

    @Override
    public CraftPlayer.TransferCookieConnection silkard$transferCookieConnection() {
        return this.transferCookieConnection;
    }

    @Override
    public boolean silkard$sentListPacket() {
        return this.sentListPacket;
    }

    @Override
    public void silkard$setSentListPacket(boolean sentListPacket) {
        this.sentListPacket = sentListPacket;
    }

    @Override
    public String silkard$kickLeaveMessage() {
        return this.kickLeaveMessage;
    }

    @Override
    public void silkard$setKickLeaveMessage(String kickLeaveMessage) {
        this.kickLeaveMessage = kickLeaveMessage;
    }

    // CraftBukkit start - ContextServerPlayer accessors
    @Unique
    private String silkard$locale = "en_us";

    @Unique
    private Integer silkard$clientViewDistance;

    @Override
    public boolean silkard$initialized() {
        return this.initialized;
    }

    @Override
    public String silkard$locale() {
        return this.silkard$locale;
    }

    @Override
    public void silkard$setLocale(String locale) {
        this.silkard$locale = locale;
    }

    @Override
    public long silkard$timeOffset() {
        return this.timeOffset;
    }

    @Override
    public void silkard$timeOffset(long timeOffset) {
        this.timeOffset = timeOffset;
    }

    @Override
    public boolean silkard$relativeTime() {
        return this.relativeTime;
    }

    @Override
    public void silkard$relativeTime(boolean relativeTime) {
        this.relativeTime = relativeTime;
    }

    @Override
    public Component silkard$listName() {
        return this.listName;
    }

    @Override
    public void silkard$listName(Component listName) {
        this.listName = listName;
    }

    @Override
    public int silkard$listOrder() {
        return this.listOrder;
    }

    @Override
    public void silkard$listOrder(int listOrder) {
        this.listOrder = listOrder;
    }

    @Override
    public int silkard$newExp() {
        return this.newExp;
    }

    @Override
    public void silkard$newExp(int newExp) {
        this.newExp = newExp;
    }

    @Override
    public int silkard$newLevel() {
        return this.newLevel;
    }

    @Override
    public void silkard$newLevel(int newLevel) {
        this.newLevel = newLevel;
    }

    @Override
    public int silkard$newTotalExp() {
        return this.newTotalExp;
    }

    @Override
    public void silkard$newTotalExp(int newTotalExp) {
        this.newTotalExp = newTotalExp;
    }

    @Override
    public boolean silkard$keepLevel() {
        return this.keepLevel;
    }

    @Override
    public void silkard$keepLevel(boolean keepLevel) {
        this.keepLevel = keepLevel;
    }

    @Override
    public double silkard$maxHealthCache() {
        return this.maxHealthCache;
    }

    @Override
    public void silkard$maxHealthCache(double maxHealthCache) {
        this.maxHealthCache = maxHealthCache;
    }

    @Override
    public boolean silkard$joining() {
        return this.joining;
    }

    @Override
    public void silkard$setJoining(boolean joining) {
        this.joining = joining;
    }

    @Override
    public Integer silkard$clientViewDistance() {
        return this.silkard$clientViewDistance;
    }

    @Override
    public void silkard$setClientViewDistance(Integer clientViewDistance) {
        this.silkard$clientViewDistance = clientViewDistance;
    }
    // CraftBukkit end

    // CraftBukkit start - per-player time / weather
    public long timeOffset = 0;
    public boolean relativeTime = true;
    public org.bukkit.WeatherType weather = null;
    private float pluginRainPosition;
    private float pluginRainPositionPrevious;

    @Override
    public long getPlayerTime(long totalTicks) {
        if (this.relativeTime) {
            return totalTicks + this.timeOffset;
        }
        return totalTicks - (totalTicks % 24000) + this.timeOffset;
    }

    @Override
    public org.bukkit.WeatherType getPlayerWeather() {
        return this.weather;
    }

    @Override
    public void setPlayerWeather(org.bukkit.WeatherType type, boolean plugin) {
        if (!plugin && this.weather != null) {
            return;
        }
        if (plugin) {
            this.weather = type;
        }
        if (type == org.bukkit.WeatherType.DOWNFALL) {
            ((ServerPlayer) (Object) this).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
        } else {
            ((ServerPlayer) (Object) this).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
        }
    }

    @Override
    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weather == null) {
            if (oldRain != newRain) {
                ((ServerPlayer) (Object) this).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, newRain));
            }
        } else if (this.pluginRainPositionPrevious != this.pluginRainPosition) {
            ((ServerPlayer) (Object) this).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.pluginRainPosition));
        }

        if (oldThunder != newThunder) {
            if (this.weather == org.bukkit.WeatherType.DOWNFALL || this.weather == null) {
                ((ServerPlayer) (Object) this).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, newThunder));
            } else {
                ((ServerPlayer) (Object) this).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0));
            }
        }
    }

    @Override
    public void tickWeather() {
        if (this.weather == null) {
            return;
        }
        this.pluginRainPositionPrevious = this.pluginRainPosition;
        if (this.weather == org.bukkit.WeatherType.DOWNFALL) {
            this.pluginRainPosition += 0.01F;
        } else {
            this.pluginRainPosition -= 0.01F;
        }
        this.pluginRainPosition = net.minecraft.util.Mth.clamp(this.pluginRainPosition, 0.0F, 1.0F);
    }

    @Override
    public void resetPlayerWeather() {
        this.weather = null;
        this.setPlayerWeather(((ServerLevel) ((Entity) (Object) this).level()).getWeatherData().isRaining() ? org.bukkit.WeatherType.DOWNFALL : org.bukkit.WeatherType.CLEAR, false);
    }
    // CraftBukkit end

    /**
     * @author Mgazul
     * @reason CraftBukkit - tab list display name/order
     */
    @Overwrite
    public Component getTabListDisplayName() {
        return this.listName;
    }

    @Overwrite
    public int getTabListOrder() {
        return this.listOrder;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - reset experience sent flag too
     */
    @Overwrite
    public void resetSentInfo() {
        this.lastSentHealth = -1.0E8F;
        this.lastSentExp = -1;
    }

    @Shadow
    private float lastSentHealth;

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void silkard$optionsEvents(net.minecraft.server.level.ClientInformation information, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (((ServerPlayer) (Object) this).getMainArm() != information.mainHand()) {
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerChangedMainHandEvent(this.getBukkitEntity(), ((ServerPlayer) (Object) this).getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? org.bukkit.inventory.MainHand.LEFT : org.bukkit.inventory.MainHand.RIGHT));
        }
        if (!this.language.equals(information.language())) {
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerLocaleChangeEvent(this.getBukkitEntity(), information.language()));
        }
    }

    @Shadow
    private String language;

    // CraftBukkit start - world fallback / respawn support
    @Override
    public void spawnIn(Level level, boolean flag) {
        this.setLevel(level);
        if (level == null) {
            this.unsetRemoved();
            TeleportTransition teleporttransition = ((ServerPlayer) (Object) this).findRespawnPositionAndUseSpawnBlock(!flag, TeleportTransition.DO_NOTHING, null);
            this.setLevel(teleporttransition.newLevel());
            this.setPos(teleporttransition.position());
        }
        this.gameMode.setLevel((ServerLevel) level);
    }

    public TeleportTransition findRespawnPositionAndUseSpawnBlock(boolean consumeSpawnBlock, TeleportTransition.PostTeleportTransition postTeleportTransition, PlayerRespawnEvent.RespawnReason reason) {
        TeleportTransition teleportTransition;
        ServerPlayer.RespawnConfig config = ((ServerPlayer) (Object) this).getRespawnConfig();
        ServerLevel serverlevel = config == null ? null : this.server.getLevel(config.respawnData().dimension());
        if (serverlevel != null && config != null) {
            Optional<ServerPlayer.RespawnPosAngle> optional = findRespawnAndUseSpawnBlock(serverlevel, config, consumeSpawnBlock);
            if (optional.isPresent()) {
                ServerPlayer.RespawnPosAngle angle = optional.get();
                teleportTransition = new TeleportTransition(serverlevel, angle.position(), Vec3.ZERO, angle.yaw(), angle.pitch(), postTeleportTransition);
            } else {
                teleportTransition = TeleportTransition.missingRespawnBlock((ServerPlayer) (Object) this, postTeleportTransition);
            }
        } else {
            teleportTransition = TeleportTransition.createDefault((ServerPlayer) (Object) this, postTeleportTransition);
        }

        if (reason == null) {
            return teleportTransition;
        }

        Location location = CraftLocation.toBukkit(teleportTransition.position(), teleportTransition.newLevel().getWorld(), teleportTransition.yRot(), teleportTransition.xRot());
        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(this.getBukkitEntity(), location, false, false, reason);
        Bukkit.getPluginManager().callEvent(respawnEvent);
        location = respawnEvent.getRespawnLocation();

        return new TeleportTransition(((org.bukkit.craftbukkit.CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3D(location), teleportTransition.deltaMovement(), location.getYaw(), location.getPitch(), teleportTransition.relatives(), teleportTransition.postTeleportTransition());
    }

    @Override
    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        ((ServerPlayer) (Object) this).snapTo(x, y, z, yaw, pitch);
        ((ServerPlayer) (Object) this).connection.resetPosition();
    }

    @Override
    public void reset() {
        float exp = 0;
        if (this.keepLevel) {
            exp = this.experienceProgress;
            this.newTotalExp = this.totalExperience;
            this.newLevel = this.experienceLevel;
        }

        ((ServerPlayer) (Object) this).setHealth(((ServerPlayer) (Object) this).getMaxHealth());
        ((ServerPlayer) (Object) this).stopUsingItem();
        ((ServerPlayer) (Object) this).setRemainingFireTicks(0);
        this.fallDistance = 0;
        this.foodData = new FoodData();
        this.experienceLevel = this.newLevel;
        this.totalExperience = this.newTotalExp;
        this.experienceProgress = 0;
        this.deathTime = 0;
        ((ContextLivingEntity) (Object) this).setArrowCount(0, true);
        ((ContextLivingEntity) (Object) this).removeAllEffects(EntityPotionEffectEvent.Cause.DEATH);
        this.containerMenu = this.inventoryMenu;
        ((ServerPlayer) (Object) this).setLastHurtByPlayer((Player) null, 0);
        ((ServerPlayer) (Object) this).setLastHurtByMob(null);
        this.lastSentExp = -1;
        if (this.keepLevel) {
            this.experienceProgress = exp;
        } else {
            ((ServerPlayer) (Object) this).giveExperiencePoints(this.newExp);
        }
        this.keepLevel = false;
        ((ServerPlayer) (Object) this).setDeltaMovement(0, 0, 0);
    }

    @Override
    public boolean silkard$isImmobile() {
        return super.isImmobile() || !this.getBukkitEntity().isOnline();
    }

    @Override
    public org.bukkit.Location silkard$compassTarget() {
        return this.compassTarget;
    }

    @Override
    public void silkard$compassTarget(org.bukkit.Location compassTarget) {
        this.compassTarget = compassTarget;
    }

    @Override
    public void silkard$triggerDimensionChangeTriggers(ServerLevel oldLevel) {
        net.minecraft.resources.ResourceKey<Level> resourcekey = oldLevel.dimension();
        net.minecraft.resources.ResourceKey<Level> resourcekey1 = ((ServerPlayer) (Object) this).level().dimension();
        net.minecraft.resources.ResourceKey<Level> maindimensionkey = org.bukkit.craftbukkit.util.CraftDimensionUtil.getMainDimensionKey(oldLevel);
        net.minecraft.resources.ResourceKey<Level> maindimensionkey1 = org.bukkit.craftbukkit.util.CraftDimensionUtil.getMainDimensionKey(((ServerPlayer) (Object) this).level());

        net.minecraft.advancements.triggers.CriteriaTriggers.CHANGED_DIMENSION.trigger((ServerPlayer) (Object) this, maindimensionkey, maindimensionkey1);
        if (maindimensionkey != resourcekey || maindimensionkey1 != resourcekey1) {
            net.minecraft.advancements.triggers.CriteriaTriggers.CHANGED_DIMENSION.trigger((ServerPlayer) (Object) this, resourcekey, resourcekey1);
        }

        if (maindimensionkey == Level.NETHER && maindimensionkey1 == Level.OVERWORLD && this.enteredNetherPosition != null) {
            net.minecraft.advancements.triggers.CriteriaTriggers.NETHER_TRAVEL.trigger((ServerPlayer) (Object) this, this.enteredNetherPosition);
        }

        if (maindimensionkey1 != Level.NETHER) {
            this.enteredNetherPosition = null;
        }
    }
    // CraftBukkit end

    // CraftBukkit - runtime NMS has no concrete getBukkitEntity() on ServerPlayer (only the ContextServerPlayer stub); provide the real lazily-created entity.
    public CraftPlayer getBukkitEntity() {
        if (this.silkard$serverBukkitEntity == null) {
            this.silkard$serverBukkitEntity = CraftEntity.getEntity(level().getCraftServer(), (Entity) (Object) this);
        }
        return (CraftPlayer) this.silkard$serverBukkitEntity;
    }

    @Override
    public String silkard$displayName() {

        return displayName;
    }

    @Override
    public void silkard$displayName(String displayName) {
        this.displayName = displayName;
    }

    // CraftBukkit start
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void silkard$playerTeleportEvent(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        if (this.isSleeping()) {
            cir.setReturnValue(null); // CraftBukkit - SPIGOT-3154: can't teleport while sleeping
            return;
        }

        ServerLevel newLevel = transition.newLevel();
        PositionMoveRotation absolutePosition = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of((ServerPlayer) (Object) this), PositionMoveRotation.of(transition), transition.relatives());
        Location enter = this.getBukkitEntity().getLocation();
        Location exit = newLevel == null ? null : CraftLocation.toBukkit(absolutePosition.position(), ((com.mohistmc.silkard.injected.world.level.ContextLevel) newLevel).getWorld(), absolutePosition.yRot(), absolutePosition.xRot());
        PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit, ((ContextTeleportTransition) (Object) transition).silkard$cause());
        Bukkit.getServer().getPluginManager().callEvent(tpEvent);

        if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
            cir.setReturnValue(null);
        }
        // 插件修改目标位置（tpEvent.getTo() != exit 时重建 transition）待传送链后续补齐
    }
    // CraftBukkit end
}