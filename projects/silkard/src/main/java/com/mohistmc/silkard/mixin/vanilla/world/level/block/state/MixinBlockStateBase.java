package com.mohistmc.silkard.mixin.vanilla.world.level.block.state;

import com.mohistmc.silkard.injected.world.level.ContextLevel;
import com.mohistmc.silkard.injected.world.level.block.state.ContextBlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/10/28
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase {

    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    // CraftBukkit - context aware placement
    @Overwrite
    public void onPlace(Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        ((ContextBlockBehaviour) (Object) this.getBlock()).onPlace(this.asState(), level, pos, oldState, movedByPiston, null);
    }

    public void onPlace(Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, @Nullable UseOnContext context) {
        ((ContextBlockBehaviour) (Object) this.getBlock()).onPlace(this.asState(), level, pos, oldState, movedByPiston, context);
    }

    // CraftBukkit start
    @Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
    private void silkard$blockPhysics(Level level, BlockPos pos, Block changedBlock, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        // BUKKIT-4923: Ignore Block Physics in Chunk population.
        if (((ContextLevel) level).silkard$populating()) {
            ci.cancel();
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            CraftWorld cworld = serverLevel.getWorld();
            if (cworld != null) {
                BlockState state = (BlockState) (Object) this;
                BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(level, pos), CraftBlockData.fromData(state));
                serverLevel.getCraftServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    ci.cancel();
                }
            }
        }
    }
    // CraftBukkit end
}
