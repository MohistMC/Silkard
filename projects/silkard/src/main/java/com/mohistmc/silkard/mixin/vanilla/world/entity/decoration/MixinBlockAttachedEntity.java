package com.mohistmc.silkard.mixin.vanilla.world.entity.decoration;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.entity.Hanging;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(BlockAttachedEntity.class)
public abstract class MixinBlockAttachedEntity {

    private Level silkard$level() {
        return ((Entity) (Object) this).level();
    }

    // CraftBukkit start - fire break events
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;survives()Z", shift = At.Shift.AFTER), cancellable = true)
    private void silkard$hangingBreakByPhysics(CallbackInfo ci) {
        BlockState material = this.silkard$level().getBlockState(((Entity) (Object) this).blockPosition());
        HangingBreakEvent.RemoveCause cause = material.isAir() ? HangingBreakEvent.RemoveCause.PHYSICS : HangingBreakEvent.RemoveCause.OBSTRUCTION;

        HangingBreakEvent event = new HangingBreakEvent(this.silkard$hanging(), cause);
        this.silkard$callEvent(event);

        if (((Entity) (Object) this).isRemoved() || event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"), cancellable = true)
    private void silkard$hangingBreakByEntity(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        Entity damager = source.isDirect() ? source.getDirectEntity() : source.getEntity();
        HangingBreakEvent event;
        if (damager != null) {
            event = new HangingBreakByEntityEvent(this.silkard$hanging(), ((ContextEntity) (Object) damager).getBukkitEntity(),
                    source.is(DamageTypeTags.IS_EXPLOSION) ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.ENTITY);
        } else {
            event = new HangingBreakEvent(this.silkard$hanging(),
                    source.is(DamageTypeTags.IS_EXPLOSION) ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.DEFAULT);
        }
        this.silkard$callEvent(event);

        if (((Entity) (Object) this).isRemoved() || event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
    private void silkard$hangingBreakByPiston(MoverType moverType, Vec3 delta, CallbackInfo ci) {
        HangingBreakEvent event = new HangingBreakEvent(this.silkard$hanging(), HangingBreakEvent.RemoveCause.PHYSICS);
        this.silkard$callEvent(event);

        if (((Entity) (Object) this).isRemoved() || event.isCancelled()) {
            ci.cancel();
        }
    }
    // CraftBukkit end

    // CraftBukkit - pushing must not break hanging entities
    @Inject(method = "push", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
    private void silkard$disablePushBreak(double xa, double ya, double za, CallbackInfo ci) {
        ci.cancel();
    }

    // CraftBukkit start - add Bukkit remove cause
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;discard()V"))
    private void silkard$discard(BlockAttachedEntity self) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DROP);
    }
    // CraftBukkit end

    private Hanging silkard$hanging() {
        return (Hanging) ((ContextEntity) (Object) this).getBukkitEntity();
    }

    private void silkard$callEvent(HangingBreakEvent event) {
        ((ContextLevel) (Object) this.silkard$level()).getCraftServer().getPluginManager().callEvent(event);
    }
}
