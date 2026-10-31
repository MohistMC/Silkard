package com.mohistmc.silkard.mixin.vanilla.world.level.block.state;

import com.mohistmc.silkard.injected.world.level.ContextExplosion;
import com.mohistmc.silkard.injected.world.level.block.state.ContextBlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(BlockBehaviour.class)
public abstract class MixinBlockBehaviour implements ContextBlockBehaviour {

    @Shadow
    protected abstract void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston);

    // CraftBukkit - context aware placement
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, UseOnContext context) {
        this.onPlace(state, level, pos, oldState, movedByPiston);
    }

    // CraftBukkit - use the configured explosion yield for the loot radius
    @Redirect(method = "onExplosionHit", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/Explosion;getBlockInteraction()Lnet/minecraft/world/level/Explosion$BlockInteraction;"))
    private static Explosion.BlockInteraction silkard$blockInteraction(Explosion explosion) {
        if (explosion instanceof ServerExplosion serverExplosion && ((ContextExplosion) (Object) serverExplosion).silkard$getYield() < 1.0F) {
            return Explosion.BlockInteraction.DESTROY_WITH_DECAY;
        }
        return explosion.getBlockInteraction();
    }

    @Redirect(method = "onExplosionHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;radius()F"))
    private static float silkard$radius(Explosion explosion) {
        if (explosion instanceof ServerExplosion serverExplosion) {
            float yield = ((ContextExplosion) (Object) serverExplosion).silkard$getYield();
            if (yield < 1.0F) {
                return 1.0F / yield;
            }
        }
        return explosion.radius();
    }
}
