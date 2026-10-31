package com.mohistmc.silkard.mixin.vanilla.world.entity.animal.bee;

import com.mohistmc.silkard.injected.world.entity.animal.bee.ContextBee;
import com.mohistmc.silkard.injected.world.entity.animal.bee.ContextBeePollinateGoal;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal")
public abstract class MixinBee$BeePollinateGoal implements ContextBeePollinateGoal {

    @Shadow
    @Final
    private Bee this$0;

    @Shadow
    private boolean pollinating;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void silkard$registerGoal(CallbackInfo ci) {
        ((ContextBee) (Object) this.this$0).silkard$setPollinateGoal(this);
    }

    @Override
    public void silkard$stopPollinating() {
        this.pollinating = false;
    }

    // CraftBukkit start - stop pollinating only when the bee was damaged
    @Inject(method = "stopPollinating", at = @At("HEAD"), cancellable = true)
    private void silkard$deferStopPollinating(CallbackInfo ci) {
        ci.cancel();
    }
    // CraftBukkit end
}