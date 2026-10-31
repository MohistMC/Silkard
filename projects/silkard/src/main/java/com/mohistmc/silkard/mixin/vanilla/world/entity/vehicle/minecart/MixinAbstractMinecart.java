package com.mohistmc.silkard.mixin.vanilla.world.entity.vehicle.minecart;

import com.mohistmc.silkard.injected.world.entity.vehicle.minecart.ContextAbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * @author Mgazul
 * @date 2026/4/2 19:41
 */
@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecart implements ContextAbstractMinecart  {

    // CraftBukkit start
    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public Double maxSpeed;
    public double powRailAccelMult = 1.0;
    // CraftBukkit end

    @Override
    public boolean silkard_slowWhenEmpty() {
        return slowWhenEmpty;
    }

    @Override
    public void silkard_slowWhenEmpty(boolean slowWhenEmpty) {
        this.slowWhenEmpty = slowWhenEmpty;
    }

    @Override
    public double silkard_maxSpeed() {
        return maxSpeed;
    }

    @Override
    public Double silkard$maxSpeedOrNull() {
        return maxSpeed;
    }

    @Override
    public void silkard_maxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void silkard_powRailAccelMult(double powRailAccelMult) {
        this.powRailAccelMult = powRailAccelMult;
    }

    @Override
    public double silkard_powRailAccelMult() {
        return powRailAccelMult;
    }

    // CraftBukkit start - Methods for getting and setting flying and derailed velocity modifiers
    @Override
    public Vector getFlyingVelocityMod() {
        return new Vector(flyingX, flyingY, flyingZ);
    }

    @Override
    public void setFlyingVelocityMod(Vector flying) {
        flyingX = flying.getX();
        flyingY = flying.getY();
        flyingZ = flying.getZ();
    }

    @Override
    public Vector getDerailedVelocityMod() {
        return new Vector(derailedX, derailedY, derailedZ);
    }

    @Override
    public void setDerailedVelocityMod(Vector derailed) {
        derailedX = derailed.getX();
        derailedY = derailed.getY();
        derailedZ = derailed.getZ();
    }
    // CraftBukkit end

    // CraftBukkit start - vehicle update and move events
    @Unique
    private double silkard$prevX;
    @Unique
    private double silkard$prevY;
    @Unique
    private double silkard$prevZ;
    @Unique
    private float silkard$prevYaw;
    @Unique
    private float silkard$prevPitch;

    @Inject(method = "tick", at = @At("HEAD"))
    private void silkard$savePosition(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        this.silkard$prevX = self.getX();
        this.silkard$prevY = self.getY();
        this.silkard$prevZ = self.getZ();
        this.silkard$prevYaw = self.getYRot();
        this.silkard$prevPitch = self.getXRot();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartBehavior;tick()V", shift = At.Shift.AFTER))
    private void silkard$vehicleEvents(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        org.bukkit.World world = self.level().getWorld();
        org.bukkit.Location from = new org.bukkit.Location(world, this.silkard$prevX, this.silkard$prevY, this.silkard$prevZ, this.silkard$prevYaw, this.silkard$prevPitch);
        org.bukkit.Location to = org.bukkit.craftbukkit.util.CraftLocation.toBukkit(self.position(), world, self.getYRot(), self.getXRot());
        org.bukkit.entity.Vehicle vehicle = (org.bukkit.entity.Vehicle) self.getBukkitEntity();

        self.level().getCraftServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleUpdateEvent(vehicle));

        if (!from.equals(to)) {
            self.level().getCraftServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleMoveEvent(vehicle, from, to));
        }
    }
    // CraftBukkit end

    // CraftBukkit start - configurable derailed and flying velocity modifiers
    @Redirect(method = "comeOffTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private net.minecraft.world.phys.Vec3 silkard$derailed(net.minecraft.world.phys.Vec3 movement, double scale) {
        return new net.minecraft.world.phys.Vec3(movement.x * this.derailedX, movement.y * this.derailedY, movement.z * this.derailedZ);
    }

    @Redirect(method = "comeOffTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private net.minecraft.world.phys.Vec3 silkard$flying(net.minecraft.world.phys.Vec3 movement, double scale) {
        return new net.minecraft.world.phys.Vec3(movement.x * this.flyingX, movement.y * this.flyingY, movement.z * this.flyingZ);
    }
    // CraftBukkit end

    // CraftBukkit start - portal handling moved to Entity#postTick
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;handlePortal()V"))
    private void silkard$handlePortal(AbstractMinecart self) {
        // handled in postTick
    }
    // CraftBukkit end

    // CraftBukkit start - vehicle entity collision event
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)    private void silkard$vehicleCollision(net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        if (entity.noPhysics || self.noPhysics) {
            return;
        }
        if (self.hasPassenger(entity)) {
            return;
        }
        if (com.mohistmc.silkard.util.BukkitUtils.callVehicleEntityCollisionEvent(self, entity)) {
            ci.cancel();
        }
    }
    // CraftBukkit end
}
