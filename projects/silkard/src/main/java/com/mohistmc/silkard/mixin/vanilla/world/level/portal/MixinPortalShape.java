package com.mohistmc.silkard.mixin.vanilla.world.level.portal;

import com.mohistmc.silkard.injected.world.level.ContextLevelAccessor;
import com.mohistmc.silkard.injected.world.level.portal.ContextPortalShape;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(PortalShape.class)
public abstract class MixinPortalShape implements ContextPortalShape {

    @Shadow
    @Final
    private Direction.Axis axis;
    @Shadow
    @Final
    private Direction rightDir;
    @Shadow
    @Final
    private BlockPos bottomLeft;
    @Shadow
    @Final
    private int height;
    @Shadow
    @Final
    private int width;

    // CraftBukkit - PortalCreateEvent(FIRE) with frame and portal blocks
    @Override
    public boolean createPortalBlocks(LevelAccessor level, Entity entity) {
        World bworld = ((ContextLevelAccessor) level).getMinecraftWorld().getWorld();
        BlockStateListPopulator blocks = new BlockStateListPopulator(level);
        BlockState blockstate = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);

        for (int k = 0; k < this.width; ++k) {
            BlockPos pos = this.bottomLeft.relative(this.rightDir, k).below();
            blocks.setBlock(pos, level.getBlockState(pos), 18);
        }
        for (int j = 0; j < this.height; ++j) {
            BlockPos left = this.bottomLeft.relative(Direction.UP, j).relative(this.rightDir, -1);
            BlockPos right = this.bottomLeft.relative(Direction.UP, j).relative(this.rightDir, this.width);
            blocks.setBlock(left, level.getBlockState(left), 18);
            blocks.setBlock(right, level.getBlockState(right), 18);
        }
        for (int k = 0; k < this.width; ++k) {
            BlockPos pos = this.bottomLeft.relative(Direction.UP, this.height).relative(this.rightDir, k);
            blocks.setBlock(pos, level.getBlockState(pos), 18);
        }
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                .forEach(pos -> blocks.setBlock(pos, blockstate, 18));

        PortalCreateEvent event = new PortalCreateEvent(
                (List<org.bukkit.block.BlockState>) (List<?>) blocks.getList(),
                bworld,
                entity == null ? null : entity.getBukkitEntity(),
                PortalCreateEvent.CreateReason.FIRE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                .forEach(pos -> level.setBlock(pos, blockstate, 18));
        return true;
    }
}
