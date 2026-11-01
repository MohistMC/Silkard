package com.mohistmc.silkard.mixin.vanilla.world.entity.vehicle.boat;

import com.mohistmc.silkard.injected.world.entity.vehicle.boat.ContextAbstractBoat;
import com.mohistmc.silkard.util.BukkitUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.bukkit.Location;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/4/2 19:35
 */
@Mixin(AbstractBoat.class)
public class MixinAbstractBoat implements ContextAbstractBoat {

    // CraftBukkit start
    // PAIL: Some of these haven't worked since a few updates, and since 1.9 they are less and less applicable.
    public double maxSpeed = 0.4D;
    public double occupiedDeceleration = 0.2D;
    public double unoccupiedDeceleration = -1;
    public boolean landBoats = false;
    // CraftBukkit end

    @Override
    public double silkard_maxSpeed() {
        return maxSpeed;
    }

    @Override
    public void silkard_maxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    @Override
    public double silkard_occupiedDeceleration() {
        return occupiedDeceleration;
    }

    @Override
    public void silkard_occupiedDeceleration(double occupiedDeceleration) {
        this.occupiedDeceleration = occupiedDeceleration;
    }

    @Override
    public double silkard_unoccupiedDeceleration() {
        return unoccupiedDeceleration;
    }

    @Override
    public void silkard_unoccupiedDeceleration(double occupiedDeceleration) {
        this.unoccupiedDeceleration = occupiedDeceleration;
    }

    @Override
    public boolean silkard_landBoats() {
        return landBoats;
    }

    @Override
    public void silkard_landBoats(boolean landBoats) {
        this.landBoats = landBoats;
    }

    // CraftBukkit start - vehicle entity collision event
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void silkard$vehicleCollision(Entity entity, CallbackInfo ci) {
        AbstractBoat self = (AbstractBoat) (Object) this;
        boolean boat = entity instanceof AbstractBoat;
        double otherMinY = entity.getBoundingBox().minY;
        double selfMinY = self.getBoundingBox().minY;
        double selfMaxY = self.getBoundingBox().maxY;
        if (boat ? otherMinY >= selfMaxY : otherMinY > selfMinY) {
            return;
        }
        if (self.isPassengerOfSameVehicle(entity)) {
            return;
        }
        if (BukkitUtils.callVehicleEntityCollisionEvent(self, entity)) {
            ci.cancel();
        }
    }
    // CraftBukkit end

    // CraftBukkit start - vehicle update and move events
    @Unique
    private Location silkard$lastLocation;

    @Inject(method = "tick", at = @At("RETURN"))
    private void silkard$vehicleEvents(CallbackInfo ci) {
        AbstractBoat self = (AbstractBoat) (Object) this;
        org.bukkit.Server server = self.level().getCraftServer();
        org.bukkit.World world = self.level().getWorld();

        Location to = CraftLocation.toBukkit(self.position(), world, self.getYRot(), self.getXRot());
        Vehicle vehicle = (Vehicle) self.getBukkitEntity();

        server.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));

        if (this.silkard$lastLocation != null && !this.silkard$lastLocation.equals(to)) {
            server.getPluginManager().callEvent(new VehicleMoveEvent(vehicle, this.silkard$lastLocation, to));
        }
        this.silkard$lastLocation = vehicle.getLocation();
    }
    // CraftBukkit end

    // CraftBukkit start - drop leash on destroy if leashed
    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void silkard$dropLeashOnRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        AbstractBoat self = (AbstractBoat) (Object) this;
        if (!self.level().isClientSide() && reason.shouldDestroy() && self.isLeashed()) {
            self.dropLeash();
        }
    }
    // CraftBukkit end
}
