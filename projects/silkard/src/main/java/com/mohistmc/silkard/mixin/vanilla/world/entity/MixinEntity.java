package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import net.minecraft.core.BlockPos;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/3/28 05:14
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements ContextEntity {

    // @formatter:off
    @Shadow private float yRot;
    @Shadow private Level level;
    @Shadow @Final public abstract double getX();
    @Shadow @Final public abstract double getZ();
    // CraftBukkit start
    @Unique public boolean forceDrops;
    @Unique public boolean persist = true;
    @Unique public boolean visibleByDefault = true;
    @Unique public boolean valid;
    @Unique public boolean inWorld = false;
    @Unique public boolean generation;
    @Unique public int maxAirTicks = getDefaultMaxAirSupply(); // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    @Unique public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    @Unique public boolean lastDamageCancelled; // SPIGOT-5339, SPIGOT-6252, SPIGOT-6777: Keep track if the event was canceled
    @Unique public boolean persistentInvisibility = false;
    @Unique public BlockPos lastLavaContact;
    // Marks an entity, that it was removed by a plugin via Entity#remove
    // Main use case currently is for SPIGOT-7487, preventing dropping of leash when leash is removed
    @Unique public boolean pluginRemoved = false;
    @Unique private CraftEntity silkard$bukkitEntity;
    // @formatter:on

    public float getBukkitYaw() {
        return this.yRot;
    }

    public boolean isChunkLoaded() {
        return level.hasChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }

    // CraftBukkit - the runtime NMS has no concrete getBukkitEntity() on Entity (only the ContextEntity stub); provide the real lazily-created entity here.
    public CraftEntity getBukkitEntity() {
        if (this.silkard$bukkitEntity == null) {
            this.silkard$bukkitEntity = CraftEntity.getEntity(level.getCraftServer(), (Entity) (Object) this);
        }
        return this.silkard$bukkitEntity;
    }
    // CraftBukkit end

    // CraftBukkit start - EntityRemoveEvent
    @Override
    public void remove(Entity.RemovalReason removalReason, EntityRemoveEvent.Cause cause) {
        ((ContextEntity) (Object) this).silkard$superRemove(removalReason, cause);
    }

    @Override
    public void silkard$superRemove(Entity.RemovalReason removalReason, EntityRemoveEvent.Cause cause) {
        CraftEventFactory.callEntityRemoveEvent((Entity) (Object) this, cause);
        ((Entity) (Object) this).remove(removalReason);
    }

    @Override
    public void discard(EntityRemoveEvent.Cause cause) {
        this.remove(Entity.RemovalReason.DISCARDED, cause);
    }

    @Override
    public void setRemoved(Entity.RemovalReason removalReason, EntityRemoveEvent.Cause cause) {
        CraftEventFactory.callEntityRemoveEvent((Entity) (Object) this, cause);
        ((Entity) (Object) this).setRemoved(removalReason);
    }
    // CraftBukkit end

    // CraftBukkit - EntitySelector.pushableBy calls this; CB returns isPushable() here (the argument is intentionally ignored).
    @Override
    public boolean canCollideWithBukkit(Entity entity) {
        return ((Entity) (Object) this).isPushable();
    }

    @Override
    public boolean silkard_generation() {
        return generation;
    }

    @Override
    public void silkard_generation(boolean gen) {
        generation = gen;
    }

    @Override
    public int silkard_maxAirTicks() {
        return maxAirTicks;
    }

    @Override
    public void silkard_maxAirTicks(int maxAirTicks) {
        this.maxAirTicks = maxAirTicks;
    }

    @Override
    public org.bukkit.projectiles.ProjectileSource silkard_projectileSource() {
       return projectileSource;
    }

    @Override
    public void silkard_projectileSource(org.bukkit.projectiles.ProjectileSource projectileSource) {
        this.projectileSource = projectileSource;
    }

    @Override
    public boolean silkard$forceDrops() {
       return forceDrops;
    }

    @Override
    public void silkard$forceDrops(boolean forceDrops) {
       this.forceDrops = forceDrops;
    }

    @Override
    public boolean silkard_pluginRemoved() {
       return pluginRemoved;
    }

    @Override
    public void silkard_pluginRemoved(boolean pluginRemoved) {
       this.pluginRemoved = pluginRemoved;
    }


    @Override
    public boolean silkard_valid() {
       return valid;
    }

    @Override
    public void silkard_valid(boolean valid) {
       this.valid = valid;
    }

    @Override
    public boolean silkard_inWorld() {
       return inWorld;
    }

    @Override
    public void silkard_inWorld(boolean inWorld) {
       this.inWorld = inWorld;
    }

    @Override
    public boolean silkard_visibleByDefault() {
       return visibleByDefault;
    }

    @Override
    public void silkard_visibleByDefault(boolean visibleByDefault) {
       this.visibleByDefault = visibleByDefault;
    }

    @Override
    public boolean silkard_persist() {
       return persist;
    }

    @Override
    public void silkard_persist(boolean persist) {
        this.persist = persist;
    }

    @Override
    public boolean silkard_persistentInvisibility() {
        return persistentInvisibility;
    }

    @Override
    public void silkard_persistentInvisibility(boolean persistentInvisibility) {
        this.persistentInvisibility = persistentInvisibility;
    }

    @Override
    public boolean silkard$lastDamageCancelled() {
        return lastDamageCancelled;
    }

    @Override
    public void silkard$lastDamageCancelled(boolean lastDamageCancelled) {
        this.lastDamageCancelled = lastDamageCancelled;
    }

    @Override
    public BlockPos silkard$lastLavaContact() {
        return lastLavaContact;
    }

    @Override
    public void silkard$setLastLavaContact(BlockPos lastLavaContact) {
        this.lastLavaContact = lastLavaContact;
    }

    // CraftBukkit start
    /**
     * @author Mgazul
     * @reason CraftBukkit - delegate to cause overload
     */
    @Overwrite
    public boolean teleportTo(net.minecraft.server.level.ServerLevel level, double x, double y, double z, java.util.Set<net.minecraft.world.entity.Relative> relatives, float newYRot, float newXRot, boolean resetCamera) {
        return this.teleportTo(level, x, y, z, relatives, newYRot, newXRot, resetCamera, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @Override
    public boolean teleportTo(net.minecraft.server.level.ServerLevel level, double x, double y, double z, java.util.Set<net.minecraft.world.entity.Relative> relatives, float newYRot, float newXRot, boolean resetCamera, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        net.minecraft.world.level.portal.TeleportTransition transition = new net.minecraft.world.level.portal.TeleportTransition(level, new net.minecraft.world.phys.Vec3(x, y, z), net.minecraft.world.phys.Vec3.ZERO, newYRot, newXRot, relatives, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
        ((com.mohistmc.silkard.injected.world.level.portal.ContextTeleportTransition) (Object) transition).silkard$cause(cause);
        return this.teleport(transition) != null;
    }

    @Shadow
    public abstract Entity teleport(net.minecraft.world.level.portal.TeleportTransition transition);

    @Shadow
    private Entity teleportSameDimension(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.portal.TeleportTransition transition) {
        throw new AssertionError();
    }

    @Shadow
    private Entity teleportCrossDimension(net.minecraft.server.level.ServerLevel oldLevel, net.minecraft.server.level.ServerLevel newLevel, net.minecraft.world.level.portal.TeleportTransition transition) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void stopRiding();

    @Shadow
    public abstract boolean isRemoved();

    /**
     * @author Mgazul
     * @reason CraftBukkit - EntityTeleportEvent
     */
    @Inject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;stopRiding()V"), cancellable = true)
    private void silkard$entityTeleportEvent(net.minecraft.world.level.portal.TeleportTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // CraftBukkit start
            net.minecraft.world.entity.PositionMoveRotation absolutePosition = net.minecraft.world.entity.PositionMoveRotation.calculateAbsolute(net.minecraft.world.entity.PositionMoveRotation.of((Entity) (Object) this), net.minecraft.world.entity.PositionMoveRotation.of(transition), transition.relatives());
            org.bukkit.Location to = org.bukkit.craftbukkit.util.CraftLocation.toBukkit(absolutePosition.position(), ((com.mohistmc.silkard.injected.world.level.ContextLevel) transition.newLevel()).getWorld(), absolutePosition.yRot(), absolutePosition.xRot());
            org.bukkit.event.entity.EntityTeleportEvent teleEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityTeleportEvent((Entity) (Object) this, to);
            if (teleEvent.isCancelled()) {
                cir.setReturnValue(null);
                return;
            }
            // 目标修正（teleEvent.getTo() 与 to 不同时重建 transition）待跨维度簇一并处理
            // CraftBukkit end
        }
    }
    // CraftBukkit end

    // CraftBukkit start - portal handling moved to postTick
    @Shadow
    protected abstract void handlePortal();

    @Override
    public void postTick() {
        // No clean way to break out of ticking once the entity has been copied to a new world, so instead we move the portalling later in the tick cycle
        if (!(((Object) this) instanceof net.minecraft.server.level.ServerPlayer)) {
            this.handlePortal();
        }
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handlePortal()V"))
    private void silkard$handlePortal(Entity self) {
        if (self instanceof net.minecraft.server.level.ServerPlayer) {
            this.handlePortal();
        }
    }
    // CraftBukkit end

    // CraftBukkit start - Entity.patch
    public int getDefaultMaxAirSupply() {
        return 300;
    }

    @Override
    public void refreshEntityData(net.minecraft.server.level.ServerPlayer to) {
        java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> list = ((Entity) (Object) this).getEntityData().getNonDefaultValues();
        if (list != null) {
            to.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(((Entity) (Object) this).getId(), list));
        }
    }

    @Shadow
    protected abstract net.minecraft.sounds.SoundEvent getSwimSound();

    @Shadow
    protected abstract net.minecraft.sounds.SoundEvent getSwimSplashSound();

    @Shadow
    protected abstract net.minecraft.sounds.SoundEvent getSwimHighSpeedSplashSound();

    @Override
    public net.minecraft.sounds.SoundEvent getSwimSound0() {
        return this.getSwimSound();
    }

    @Override
    public net.minecraft.sounds.SoundEvent getSwimSplashSound0() {
        return this.getSwimSplashSound();
    }

    @Override
    public net.minecraft.sounds.SoundEvent getSwimHighSpeedSplashSound0() {
        return this.getSwimHighSpeedSplashSound();
    }

    public final void igniteForSeconds(float numberOfSeconds, boolean callEvent) {
        if (callEvent) {
            org.bukkit.event.entity.EntityCombustEvent event = new org.bukkit.event.entity.EntityCombustEvent(this.getBukkitEntity(), numberOfSeconds);
            this.level.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            numberOfSeconds = event.getDuration();
        }
        ((Entity) (Object) this).igniteForTicks(net.minecraft.util.Mth.floor(numberOfSeconds * 20.0F));
    }

    @Override
    public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output, boolean includeAll) {
        if (!this.persist) {
            return false;
        }
        boolean result = ((Entity) (Object) this).saveAsPassenger(output);
        if (result) {
            output.putInt("Bukkit.updateLevel", 2);
        }
        return result;
    }

    @Override
    public void saveWithoutId(net.minecraft.world.level.storage.ValueOutput output, boolean includeAll) {
        ((Entity) (Object) this).saveWithoutId(output);
        output.putInt("Bukkit.updateLevel", 2);
        if (!this.persist) {
            output.putBoolean("Bukkit.persist", this.persist);
        }
        if (!this.visibleByDefault) {
            output.putBoolean("Bukkit.visibleByDefault", this.visibleByDefault);
        }
        if (this.persistentInvisibility) {
            output.putBoolean("Bukkit.invisible", this.persistentInvisibility);
        }
    }

    @Override
    public boolean dropAllLeashConnections(net.minecraft.world.entity.player.Player player, org.bukkit.event.entity.EntityUnleashEvent.UnleashReason reason) {
        java.util.List<net.minecraft.world.entity.Leashable> list = net.minecraft.world.entity.Leashable.leashableLeashedTo((Entity) (Object) this);
        boolean flag = !list.isEmpty();

        if (((Object) this) instanceof net.minecraft.world.entity.Leashable leashable && leashable.isLeashed()) {
            this.level.getCraftServer().getPluginManager().callEvent(new org.bukkit.event.entity.EntityUnleashEvent(this.getBukkitEntity(), reason));
            leashable.dropLeash();
            flag = true;
        }

        for (net.minecraft.world.entity.Leashable leashable1 : list) {
            if (leashable1 instanceof Entity entity) {
                this.level.getCraftServer().getPluginManager().callEvent(new org.bukkit.event.entity.EntityUnleashEvent(entity.getBukkitEntity(), reason));
            }
            leashable1.dropLeash();
        }

        return flag;
    }

    @Override
    public org.bukkit.craftbukkit.event.CraftPortalEvent callPortalEvent(Entity entity, org.bukkit.Location exit, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        org.bukkit.Location enter = this.getBukkitEntity().getLocation();
        org.bukkit.event.entity.EntityPortalEvent event = new org.bukkit.event.entity.EntityPortalEvent((org.bukkit.entity.Entity) this.getBukkitEntity(), enter, exit, searchRadius, true, creationRadius);
        org.bukkit.Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new org.bukkit.craftbukkit.event.CraftPortalEvent(event);
    }

    @Unique
    private final net.minecraft.commands.CommandSource silkard$commandSource = new net.minecraft.commands.CommandSource() {
        @Override
        public void sendSystemMessage(net.minecraft.network.chat.Component component) {
            MixinEntity.this.getBukkitEntity().sendMessage(component.getString());
        }

        @Override
        public org.bukkit.command.CommandSender getBukkitSender(net.minecraft.commands.CommandSourceStack wrapper) {
            return ((Entity) (Object) MixinEntity.this).getBukkitEntity();
        }

        @Override
        public boolean acceptsSuccess() {
            return false;
        }

        @Override
        public boolean acceptsFailure() {
            return false;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    };

    @Override
    public net.minecraft.commands.CommandSource silkard$commandSource() {
        return this.silkard$commandSource;
    }

    @Override
    public void silkard$setCommandSource(net.minecraft.commands.CommandSource commandSource) {
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - EntityRemoveEvent.DEATH
     */
    @Overwrite
    public void kill(net.minecraft.server.level.ServerLevel level) {
        ((ContextEntity) (Object) this).setRemoved(Entity.RemovalReason.KILLED, EntityRemoveEvent.Cause.DEATH);
        ((Entity) (Object) this).gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE);
    }

    @Overwrite
    protected void onBelowWorld() {
        this.discard(EntityRemoveEvent.Cause.OUT_OF_WORLD);
    }

    @Inject(method = "setPose", at = @At("HEAD"))
    private void silkard$poseChange(net.minecraft.world.entity.Pose pose, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (pose != ((Entity) (Object) this).getPose()) {
            this.level.getCraftServer().getPluginManager().callEvent(new org.bukkit.event.entity.EntityPoseChangeEvent(this.getBukkitEntity(), org.bukkit.entity.Pose.values()[pose.ordinal()]));
        }
    }

    @Overwrite
    protected void setRot(float yRot, float xRot) {
        if (Float.isNaN(yRot)) {
            yRot = 0;
        }
        if (yRot == Float.POSITIVE_INFINITY || yRot == Float.NEGATIVE_INFINITY) {
            yRot = 0;
        }
        if (Float.isNaN(xRot)) {
            xRot = 0;
        }
        if (xRot == Float.POSITIVE_INFINITY || xRot == Float.NEGATIVE_INFINITY) {
            xRot = 0;
        }
        ((Entity) (Object) this).setYRot(yRot % 360.0F);
        ((Entity) (Object) this).setXRot(xRot % 360.0F);
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - EntityCombustEvent for lava ignition
     */
    @Overwrite
    public void lavaIgnite() {
        if (!((Entity) (Object) this).fireImmune()) {
            if (((Object) this) instanceof net.minecraft.world.entity.LivingEntity && this.remainingFireTicks <= 0) {
                org.bukkit.block.Block damager = (this.lastLavaContact == null) ? null : org.bukkit.craftbukkit.block.CraftBlock.at(this.level, this.lastLavaContact);
                org.bukkit.event.entity.EntityCombustEvent combustEvent = new org.bukkit.event.entity.EntityCombustByBlockEvent(damager, this.getBukkitEntity(), 15);
                this.level.getCraftServer().getPluginManager().callEvent(combustEvent);
                if (!combustEvent.isCancelled()) {
                    this.igniteForSeconds(combustEvent.getDuration(), false);
                }
            } else {
                this.igniteForSeconds(15.0F, false);
            }
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void silkard$clearLavaContact(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!((Entity) (Object) this).isInLava()) {
            this.lastLavaContact = null;
        }
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    private void silkard$toggleSwim(boolean swimming, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (this.valid && ((Entity) (Object) this).isSwimming() != swimming && ((Object) this) instanceof net.minecraft.world.entity.LivingEntity) {
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callToggleSwimEvent((net.minecraft.world.entity.LivingEntity) (Object) this, swimming).isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "snapTo(DDDFF)V", at = @At("TAIL"))
    private void silkard$loadChunkOnSnap(double x, double y, double z, float yRot, float xRot, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (this.valid && this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
        }
    }
    // CraftBukkit end

    @Shadow
    protected int remainingFireTicks;
}
