package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import com.mohistmc.silkard.injected.world.damagesource.ContextDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(BedBlock.class)
public class MixinBedBlock {

    @Unique
    private org.bukkit.block.BlockState silkard$capturedState;

    // capture BlockState before remove block, so the explosion reports the bed as its source
    @Inject(method = "destroyOnUse", at = @At("HEAD"))
    private void silkard$captureState(BlockState state, Level level, BlockPos pos, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        this.silkard$capturedState = CraftBlock.at(level, pos).getState();
    }

    @Redirect(method = "destroyOnUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;badRespawnPointExplosion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource silkard$badRespawn(DamageSources sources, Vec3 vec3) {
        return ((ContextDamageSources) sources).badRespawnPointExplosion(vec3, this.silkard$capturedState);
    }
}
