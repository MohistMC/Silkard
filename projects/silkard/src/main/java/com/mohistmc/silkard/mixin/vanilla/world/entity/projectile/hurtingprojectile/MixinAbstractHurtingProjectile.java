package com.mohistmc.silkard.mixin.vanilla.world.entity.projectile.hurtingprojectile;

import com.mohistmc.silkard.injected.world.entity.projectile.hurtingprojectile.ContextAbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/5/1 16:50
 */
@Mixin(AbstractHurtingProjectile.class)
public class MixinAbstractHurtingProjectile implements ContextAbstractHurtingProjectile {

    public float bukkitYield = 1; // CraftBukkit
    public boolean isIncendiary = true; // CraftBukkit

    public void setDirection(double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        if (vec3.lengthSqr() == 0.0) {
            this.setDeltaMovement(0.0, 0.0, 0.0);
            return;
        }
        double speed = this.getDeltaMovement().length();
        this.setDeltaMovement(vec3.normalize().scale(speed));
        this.needsSync = true;
    }

    public float silkard$bukkitYield() {
        return bukkitYield;
    }

    public boolean silkard$isIncendiary() {
        return isIncendiary;
    }

    public void silkard$bukkitYield(float yield) {
        this.bukkitYield = yield;
    }

    public void silkard$isIncendiary(boolean incendiary) {
        this.isIncendiary = incendiary;
    }

    // CraftBukkit start - projectile hit event / remove cause
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/AbstractHurtingProjectile;hitTargetOrDeflectSelf(Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/entity/projectile/ProjectileDeflection;"))
    private ProjectileDeflection silkard$preHitTargetOrDeflectSelf(AbstractHurtingProjectile instance, HitResult hitResult) {
        return instance.preHitTargetOrDeflectSelf(hitResult);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/AbstractHurtingProjectile;discard()V"))
    private void silkard$discard(AbstractHurtingProjectile instance) {
        instance.discard(EntityRemoveEvent.Cause.DESPAWN);
    }
    // CraftBukkit end
}
