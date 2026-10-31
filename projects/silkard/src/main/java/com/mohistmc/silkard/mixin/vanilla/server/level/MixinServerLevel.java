package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.level.ContextServerLevel;
import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.level.saveddata.ContextWeatherData;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.timers.TimerQueue;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/4/2 18:11
 */
@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements ContextServerLevel {

    // @formatter:off
    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow @Final public ServerLevelData serverLevelData;
    @Shadow public abstract WeatherData getWeatherData();
    // @formatter:on

    // CraftBukkit start
    public LevelStorageSource.LevelStorageAccess storageSource;
    public UUID uuid;
    // Moved from MinecraftServer start
    private SavedDataStorage savedDataStorage;
    private GameRules gameRules;
    private WorldGenSettings worldGenSettings;
    private CustomBossEvents customBossEvents;
    private RandomSequences randomSequences;
    private WeatherData weatherData;
    private TimerQueue<MinecraftServer> scheduledEvents;
    private ServerClockManager clockManager;

    public WorldGenSettings getWorldGenSettings() {
        return this.worldGenSettings;
    }

    public CustomBossEvents getCustomBossEvents() {
        return this.customBossEvents;
    }

    public RandomSource getRandomSequence(Identifier key) {
        return this.randomSequences.get(key, this.worldGenSettings.options().seed());
    }

    public RandomSequences getRandomSequences() {
        return this.randomSequences;
    }

    // CraftBukkit start
    @Inject(method = "getWeatherData", at = @At("RETURN"))
    private void silkard$initWeatherData(CallbackInfoReturnable<WeatherData> cir) {
        ((ContextWeatherData) (Object) cir.getReturnValue()).silkard$init((ServerLevel) (Object) this);
    }
    // CraftBukkit end

    public void setWeatherParameters(int clearTime, int rainTime, boolean raining, boolean thundering) {
        WeatherData weatherdata = this.getWeatherData();

        weatherdata.setClearWeatherTime(clearTime);
        weatherdata.setRainTime(rainTime);
        weatherdata.setThunderTime(rainTime);
        weatherdata.setRaining(raining);
        weatherdata.setThundering(thundering);
    }

    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }
    // Moved from MinecraftServer end

    public LevelChunk getChunkIfLoaded(int x, int z) {
        return this.chunkSource.getChunk(x, z, false);
    }

    @Override
    public ResourceKey<LevelStem> getTypeKey() {
        return storageSource.silkard$getTypeKey();
    }

    @Override
    public LevelStorageSource.LevelStorageAccess silkard_storageSource() {
       return storageSource;
    }

    // CraftBukkit start
    public org.bukkit.generator.ChunkGenerator generator;
    public org.bukkit.generator.BiomeProvider biomeProvider;
    public org.bukkit.World.Environment environment;

    @Unique
    private org.bukkit.craftbukkit.CraftWorld silkard$bukkitWorld;

    @Override
    public UUID silkard$uuid() {
        if (this.uuid == null) {
            this.uuid = UUID.nameUUIDFromBytes(("silkard-world:" + this.serverLevelData.getLevelName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return this.uuid;
    }

    @Override
    public org.bukkit.craftbukkit.CraftWorld getWorld() {
        if (this.silkard$bukkitWorld == null) {
            this.silkard$bukkitWorld = new org.bukkit.craftbukkit.CraftWorld((ServerLevel) (Object) this, this.generator, this.biomeProvider,
                    (this.environment == null) ? org.bukkit.World.Environment.NORMAL : this.environment);
        }
        return this.silkard$bukkitWorld;
    }

    @Override
    public org.bukkit.generator.ChunkGenerator silkard$generator() {
        return this.generator;
    }

    @Override
    public org.bukkit.generator.BiomeProvider silkard$biomeProvider() {
        return this.biomeProvider;
    }

    @Override
    public void silkard$setGenerator(org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider provider) {
        this.generator = gen;
        this.biomeProvider = provider;
    }

    @Override
    public org.bukkit.World.Environment silkard$environment() {
        return this.environment == null ? org.bukkit.World.Environment.NORMAL : this.environment;
    }

    @Override
    public void silkard$setEnvironment(org.bukkit.World.Environment env) {
        this.environment = env;
    }
    // CraftBukkit end

    @Override
    public void silkard_tickEntityManager() {
        this.entityManager.tick(); // SPIGOT-6526
    }

    // CraftBukkit start - portal handling moved to Entity#postTick
    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER))
    private void silkard$postTick(Entity entity, CallbackInfo ci) {
        ((ContextEntity) entity).postTick();
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V", shift = At.Shift.AFTER))
    private void silkard$passengerPostTick(Entity vehicle, Entity passenger, CallbackInfo ci) {
        ((ContextEntity) passenger).postTick();
    }
    // CraftBukkit end

    // CraftBukkit - 26.3 dropped the SpawnReason overloads: fire the add-entity event here, then delegate to the raw 1-arg add.
    public boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (!CraftEventFactory.doEntityAddEventCalling((ServerLevel) (Object) this, entity, reason)) {
            return false;
        }
        return ((ServerLevel) (Object) this).addFreshEntity(entity);
    }

    // CraftBukkit start
    @Override
    public boolean strikeLightning(Entity entity) {
        return this.strikeLightning(entity, LightningStrikeEvent.Cause.UNKNOWN);
    }

    @Override
    public boolean strikeLightning(Entity entity, LightningStrikeEvent.Cause cause) {
        LightningStrikeEvent event = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.getBukkitEntity(), cause);
        if (event.isCancelled()) {
            return false;
        }
        return ((ServerLevel) (Object) this).addFreshEntity(entity);
    }
    // CraftBukkit end
}
