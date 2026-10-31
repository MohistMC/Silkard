package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.level.block.ContextBaseFireBlock;
import com.mohistmc.silkard.injected.world.level.portal.ContextPortalShape;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalShape;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(BaseFireBlock.class)
public class MixinBaseFireBlock implements ContextBaseFireBlock {

    @Shadow
    private static boolean inPortalDimension(Level level) {
        throw new AssertionError();
    }

    @Override
    public void fireExtinguished(LevelAccessor world, BlockPos position) {
        if (!CraftEventFactory.callBlockFadeEvent(world, position, Blocks.AIR.defaultBlockState()).isCancelled()) {
            world.removeBlock(position, false);
        }
    }

    // CraftBukkit - EntityCombustByBlockEvent
    @Redirect(method = "fireIgnite", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private static void silkard$ignite(Entity entity, float seconds) {
        EntityCombustByBlockEvent event = new EntityCombustByBlockEvent(entity.getBukkitEntity().getLocation().getBlock(), entity.getBukkitEntity(), seconds);
        entity.level().getCraftServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            ((ContextEntity) (Object) entity).igniteForSeconds(event.getDuration(), false);
        }
    }

    // CraftBukkit - context aware placement
    @Overwrite
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        this.silkard$onPlace(state, level, pos, oldState, movedByPiston, null);
    }

    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, UseOnContext context) {
        this.silkard$onPlace(state, level, pos, oldState, movedByPiston, context);
    }

    @Unique
    private void silkard$onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, UseOnContext context) {
        if (!oldState.is(state.getBlock())) {
            if (inPortalDimension(level)) {
                Optional<PortalShape> optional = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
                if (optional.isPresent()) {
                    ((ContextPortalShape) (Object) optional.get()).createPortalBlocks(level, context == null ? null : context.getPlayer());
                    return;
                }
            }

            if (!state.canSurvive(level, pos)) {
                this.fireExtinguished(level, pos);
            }
        }
    }

    // CraftBukkit - custom dimensions
    @Redirect(method = "inPortalDimension", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private static ResourceKey<Level> silkard$dimension(Level level) {
        if (level.getTypeKey() == LevelStem.OVERWORLD) {
            return Level.OVERWORLD;
        }
        if (level.getTypeKey() == LevelStem.NETHER) {
            return Level.NETHER;
        }
        return level.dimension();
    }
}
