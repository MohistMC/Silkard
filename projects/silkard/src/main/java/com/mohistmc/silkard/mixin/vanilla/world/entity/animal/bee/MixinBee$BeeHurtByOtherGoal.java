package com.mohistmc.silkard.mixin.vanilla.world.entity.animal.bee;

import com.mohistmc.silkard.injected.world.entity.ContextMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeHurtByOtherGoal")
public abstract class MixinBee$BeeHurtByOtherGoal {

    // CraftBukkit start - target reason
    @Redirect(method = "alertOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void silkard$setTarget(Mob mob, LivingEntity target) {
        ((ContextMob) (Object) mob).setTarget(target, EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY, true);
    }
    // CraftBukkit end
}