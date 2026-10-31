package com.mohistmc.silkard.mixin.vanilla.world.level.levelgen.structure;

import com.mohistmc.silkard.bukkit.StructurePieceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.CraftLootTable;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.block.CraftChest;
import org.bukkit.craftbukkit.block.CraftDispenser;
import org.bukkit.craftbukkit.util.TransformerGeneratorAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(StructurePiece.class)
public abstract class MixinStructurePiece {

    @Shadow
    private Mirror mirror;
    @Shadow
    private Rotation rotation;

    @Shadow
    protected abstract BlockPos.MutableBlockPos getWorldPos(int x, int y, int z);

    @Shadow
    protected abstract boolean canBeReplaced(LevelReader level, int x, int y, int z, BoundingBox chunkBB);

    // CraftBukkit - fluid handling is already done by the transformer access
    @Inject(method = "placeBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER), cancellable = true)
    private void silkard$skipFluid(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox chunkBB, CallbackInfo ci) {
        if (level instanceof TransformerGeneratorAccess) {
            ci.cancel();
        }
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - filled containers must expose the loot table to plugins
     */
    @Overwrite
    protected boolean createChest(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable, BlockState blockState) {
        if (chunkBB.isInside(pos) && !level.getBlockState(pos).is(Blocks.CHEST)) {
            if (blockState == null) {
                blockState = StructurePiece.reorient(level, pos, Blocks.CHEST.defaultBlockState());
            }

            CraftChest chestState = (CraftChest) CraftBlockStates.getBlockState(level, pos, blockState, null);
            chestState.setLootTable(CraftLootTable.minecraftToBukkit(lootTable));
            chestState.setSeed(random.nextLong());
            StructurePieceHelper.placeCraftBlockEntity(level, pos, chestState, 2);
            return true;
        }
        return false;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - filled dispensers must expose the loot table to plugins
     */
    @Overwrite
    protected boolean createDispenser(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, int x, int y, int z, Direction facing, ResourceKey<LootTable> lootTable) {
        BlockPos pos = this.getWorldPos(x, y, z);
        if (chunkBB.isInside(pos) && !level.getBlockState(pos).is(Blocks.DISPENSER)) {
            if (!this.canBeReplaced(level, x, y, z, chunkBB)) {
                return true;
            }
            BlockState blockState = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing);
            if (this.mirror != Mirror.NONE) {
                blockState = blockState.mirror(this.mirror);
            }
            if (this.rotation != Rotation.NONE) {
                blockState = blockState.rotate(this.rotation);
            }

            CraftDispenser dispenserState = (CraftDispenser) CraftBlockStates.getBlockState(level, pos, blockState, null);
            dispenserState.setLootTable(CraftLootTable.minecraftToBukkit(lootTable));
            dispenserState.setSeed(random.nextLong());
            StructurePieceHelper.placeCraftBlockEntity(level, pos, dispenserState, 2);
            return true;
        }
        return false;
    }
}
