package com.mohistmc.silkard.injected.server.level;

import com.mohistmc.silkard.injected.world.entity.player.ContextPlayer;
import com.mohistmc.silkard.util.ContextStateException;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.Nullable;

public interface ContextServerPlayer extends ContextPlayer {

    @Override
    default CraftPlayer getBukkitEntity() {
        throw new ContextStateException("Not implemented");
    }

    default int nextContainerCounterInt() {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$initialized() {
        throw new ContextStateException("Not implemented");
    }

    default String silkard$locale() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setLocale(String locale) {
        throw new ContextStateException("Not implemented");
    }

    default long silkard$timeOffset() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$timeOffset(long timeOffset) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$relativeTime() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$relativeTime(boolean relativeTime) {
        throw new ContextStateException("Not implemented");
    }

    default Component silkard$listName() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$listName(Component listName) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$listOrder() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$listOrder(int listOrder) {
        throw new ContextStateException("Not implemented");
    }

    default org.bukkit.Location silkard$compassTarget() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$compassTarget(org.bukkit.Location compassTarget) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$newExp(){
        throw new ContextStateException("Not implemented");
    }

    default void silkard$newExp(int newExp) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$newLevel() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$newLevel(int newLevel) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$newTotalExp() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$newTotalExp(int newTotalExp) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$keepLevel() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$keepLevel(boolean keepLevel) {
        throw new ContextStateException("Not implemented");
    }

    default double silkard$maxHealthCache() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$maxHealthCache(double maxHealthCache) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$joining() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setJoining(boolean joining) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$sentListPacket() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setSentListPacket(boolean sentListPacket) {
        throw new ContextStateException("Not implemented");
    }

    default Integer silkard$clientViewDistance() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setClientViewDistance(Integer clientViewDistance) {
        throw new ContextStateException("Not implemented");
    }

    default String silkard$kickLeaveMessage() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setKickLeaveMessage(String kickLeaveMessage) {
        throw new ContextStateException("Not implemented");
    }

    default BlockPos getSpawnPoint(ServerLevel worldserver) {
        throw new ContextStateException("Not implemented");
    }

    default String silkard$displayName() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$displayName(String displayName) {
        throw new ContextStateException("Not implemented");
    }

    default void spawnIn(Level world, boolean flag) {
        throw new ContextStateException("Not implemented");
    }

    default Entity changeDimension(ServerLevel worldserver, PlayerTeleportEvent.TeleportCause cause) {
        throw new ContextStateException("Not implemented");
    }

    default Either<Player.BedSleepingProblem, Unit> getBedResult(BlockPos blockposition, Direction enumdirection) {
        throw new ContextStateException("Not implemented");
    }

    default void teleportTo(ServerLevel worldserver, double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        throw new ContextStateException("Not implemented");
    }

    default void setRespawnPosition(ServerPlayer.@Nullable RespawnConfig respawnConfig, boolean showMessage, PlayerSpawnChangeEvent.Cause cause) {
        throw new ContextStateException("Not implemented");
    }

    default long getPlayerTime(long totalTicks) {
        throw new ContextStateException("Not implemented");
    }

    default WeatherType getPlayerWeather() {
        throw new ContextStateException("Not implemented");
    }

    default void setPlayerWeather(WeatherType type, boolean plugin) {
        throw new ContextStateException("Not implemented");
    }

    default void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        throw new ContextStateException("Not implemented");
    }

    default void tickWeather() {
        throw new ContextStateException("Not implemented");
    }

    default void resetPlayerWeather() {
        throw new ContextStateException("Not implemented");
    }

    default void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        throw new ContextStateException("Not implemented");
    }

    default net.minecraft.world.level.portal.TeleportTransition findRespawnPositionAndUseSpawnBlock(boolean consumeSpawnBlock, net.minecraft.world.level.portal.TeleportTransition.PostTeleportTransition postTeleportTransition, org.bukkit.event.player.PlayerRespawnEvent.RespawnReason reason) {
        throw new ContextStateException("Not implemented");
    }

    default void reset() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$triggerDimensionChangeTriggers(ServerLevel oldLevel) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$isImmobile() {
        throw new ContextStateException("Not implemented");
    }

    default void pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause cause) {
        throw new ContextStateException("Not implemented");
    }

    default void pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause cause) {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$transferCookieConnection(CraftPlayer.TransferCookieConnection transferCookieConnection) {
        throw new ContextStateException("Not implemented");
    }

    default CraftPlayer.TransferCookieConnection silkard$transferCookieConnection() {
        throw new ContextStateException("Not implemented");
    }

    default ItemEntity dropItem(boolean dropAll) {
        throw new ContextStateException("Not implemented");
    }
}
