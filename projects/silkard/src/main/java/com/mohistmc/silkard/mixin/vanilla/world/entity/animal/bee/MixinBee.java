package com.mohistmc.silkard.mixin.vanilla.world.entity.animal.bee;

import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import com.mohistmc.silkard.injected.world.entity.animal.bee.ContextBee;
import com.mohistmc.silkard.injected.world.entity.animal.bee.ContextBeePollinateGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(Bee.class)
public class MixinBee implements ContextBee {

    @Unique
    private ContextBeePollinateGoal silkard$pollinateGoal;

    @Override
    public void silkard$setPollinateGoal(ContextBeePollinateGoal goal) {
        this.silkard$pollinateGoal = goal;
    }

    @Override
    public ContextBeePollinateGoal silkard$pollinateGoal() {
        return this.silkard$pollinateGoal;
    }

    // CraftBukkit start - only stop pollinating if the entity was damaged
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void silkard$beeHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.silkard$pollinateGoal != null) {
            this.silkard$pollinateGoal.silkard$stopPollinating();
        }
    }
    // CraftBukkit end

    // CraftBukkit start - add Bukkit effect cause
    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean silkard$addEffect(LivingEntity entity, MobEffectInstance effect, Entity source) {
        return ((ContextLivingEntity) (Object) entity).addEffect(effect, source, EntityPotionEffectEvent.Cause.ATTACK);
    }
    // CraftBukkit end
}
