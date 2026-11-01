package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import com.mohistmc.silkard.injected.world.level.block.ContextBaseFireBlock;
import com.mohistmc.silkard.injected.world.level.block.ContextFireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(FireBlock.class)
public abstract class MixinFireBlock implements ContextFireBlock {

    @Shadow
    protected abstract boolean canSurvive(BlockState state, LevelReader level, BlockPos pos);

    @Shadow
    private BlockState getStateWithAge(LevelReader level, BlockPos pos, int age) {
        throw new AssertionError();
    }

    @Shadow
    private static int getFireTickDelay(RandomSource random) {
        throw new AssertionError();
    }

    @Unique
    private BlockPos silkard$sourcePos;

    @Inject(method = "tick", at = @At("HEAD"))
    private void silkard$captureSource(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        this.silkard$sourcePos = pos.immutable();
    }

    // CraftBukkit - BlockFadeEvent
    @Overwrite
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            if (!(level instanceof Level actualLevel)) {
                return Blocks.AIR.defaultBlockState();
            }
            CraftBlockState blockState = CraftBlockStates.getBlockState(actualLevel, pos);
            blockState.setData(Blocks.AIR.defaultBlockState());

            BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
            actualLevel.getCraftServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                return blockState.getHandle();
            }
        }
        return this.getStateWithAge(level, pos, state.getValue(FireBlock.AGE));
    }

    // CraftBukkit - context aware placement
    @Overwrite
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, (Block) (Object) this, getFireTickDelay(level.getRandom()));
    }

    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, UseOnContext context) {
        this.onPlace(state, level, pos, oldState, movedByPiston);
    }

    // CraftBukkit - extinguish through fireExtinguished whenever the block is removed during a tick
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean silkard$removeBlock(ServerLevel level, BlockPos pos, boolean movedByPiston) {
        ((ContextBaseFireBlock) (Object) this).fireExtinguished(level, pos);
        return true;
    }

    // CraftBukkit - BlockIgniteEvent and BlockSpreadEvent for fire spread
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean silkard$spreadFire(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
            return false;
        }
        if (CraftEventFactory.callBlockIgniteEvent(level, pos, this.silkard$sourcePos).isCancelled()) {
            return false;
        }
        return CraftEventFactory.handleBlockSpreadEvent(level, this.silkard$sourcePos, pos, state, flags);
    }

    // CraftBukkit - BlockBurnEvent before a block burns away
    @Inject(method = "checkBurnOut", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), cancellable = true)
    private void silkard$burnOut(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        BlockPos source = this.silkard$sourcePos == null ? pos : this.silkard$sourcePos;
        org.bukkit.block.Block theBlock = level.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        org.bukkit.block.Block sourceBlock = level.getWorld().getBlockAt(source.getX(), source.getY(), source.getZ());

        BlockBurnEvent event = new BlockBurnEvent(theBlock, sourceBlock);
        level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        BlockState blockstate = level.getBlockState(pos);
        if (blockstate.getBlock() instanceof TntBlock
                && !CraftEventFactory.callTNTPrimeEvent(level, pos, TNTPrimeEvent.PrimeCause.FIRE, null, source)) {
            ci.cancel();
        }
    }
}
