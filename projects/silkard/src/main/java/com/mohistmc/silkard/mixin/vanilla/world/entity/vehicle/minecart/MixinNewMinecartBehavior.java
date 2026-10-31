package com.mohistmc.silkard.mixin.vanilla.world.entity.vehicle.minecart;

import com.mohistmc.silkard.injected.world.entity.vehicle.minecart.ContextAbstractMinecart;
import com.mohistmc.silkard.injected.world.entity.vehicle.minecart.ContextMinecartBehavior;
import com.mohistmc.silkard.util.BukkitUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(NewMinecartBehavior.class)
public abstract class MixinNewMinecartBehavior {

    // CraftBukkit start - configurable max speed
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void silkard$maxSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        Double maxSpeed = ((ContextAbstractMinecart) this.silkard$cart()).silkard$maxSpeedOrNull();
        if (maxSpeed != null) {
            cir.setReturnValue(this.silkard$cart().isInWater() ? maxSpeed / 2.0D : maxSpeed);
        }
    }

    // CraftBukkit start - configurable slowdown when empty
    @Inject(method = "getSlowdownFactor", at = @At("HEAD"), cancellable = true)
    private void silkard$slowdownFactor(CallbackInfoReturnable<Double> cir) {
        AbstractMinecart cart = this.silkard$cart();
        if (cart.isVehicle() || !((ContextAbstractMinecart) cart).silkard_slowWhenEmpty()) {
            cir.setReturnValue(0.997D);
        }
    }
    // CraftBukkit end

    // CraftBukkit start - powered rail acceleration multiplier
    @Redirect(method = "calculateBoostTrackSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 silkard$boost(Vec3 movement, double factor) {
        double mult = ((ContextAbstractMinecart) this.silkard$cart()).silkard_powRailAccelMult();
        // factor is length + 0.06, the boost part scales with the multiplier
        return movement.scale(factor - 0.06D + 0.06D * mult);
    }
    // CraftBukkit end

    // CraftBukkit start - vehicle entity collision event
    @Redirect(method = "pickupEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean silkard$startRiding(Entity entity, Entity vehicle) {
        if (BukkitUtils.callVehicleEntityCollisionEvent(this.silkard$cart(), entity)) {
            return false;
        }
        return entity.startRiding(vehicle);
    }

    @Redirect(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V", ordinal = 0))
    private void silkard$push(Entity entity, Entity vehicle) {
        if (!this.silkard$cart().isPassengerOfSameVehicle(entity) && BukkitUtils.callVehicleEntityCollisionEvent(this.silkard$cart(), entity)) {
            return;
        }
        entity.push(vehicle);
    }

    @Redirect(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V", ordinal = 1))
    private void silkard$pushOther(Entity entity, Entity vehicle) {
        if (BukkitUtils.callVehicleEntityCollisionEvent(this.silkard$cart(), entity)) {
            return;
        }
        entity.push(vehicle);
    }
    // CraftBukkit end

    private AbstractMinecart silkard$cart() {
        return ((ContextMinecartBehavior) (Object) this).silkard$minecart();
    }
}
