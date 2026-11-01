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

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handlePortal()V"))
    private void silkard$handlePortal(Entity self) {
        if (self instanceof net.minecraft.server.level.ServerPlayer) {
            this.handlePortal();
        }
    }
    // CraftBukkit end
}
