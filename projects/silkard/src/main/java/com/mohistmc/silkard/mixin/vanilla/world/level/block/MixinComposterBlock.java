package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import com.mohistmc.silkard.injected.world.ContextContainer;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(ComposterBlock.class)
public abstract class MixinComposterBlock {

    @Unique
    private static int silkard$layerCount(Entity sourceEntity, BlockState state, ServerLevel level, BlockPos pos, Compostable compostable) {
        LootContext lootcontext = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, sourceEntity)
                .create(LootContextParamSets.BLOCK_INTERACT)).create(Optional.empty());
        return compostable.layers().get(lootcontext, 0);
    }

    // CraftBukkit - EntityChangeBlockEvent before the layer is applied
    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private static void silkard$insertItem(Entity sourceEntity, BlockState state, ServerLevel level, ItemStack itemStack, BlockPos pos,
                                           CallbackInfoReturnable<BlockState> cir) {
        int i = state.getValue(ComposterBlock.LEVEL);
        Compostable compostable = itemStack.get(DataComponents.COMPOSTABLE);
        if (i >= 7 || compostable == null) {
            cir.setReturnValue(state);
            return;
        }
        int j = silkard$layerCount(sourceEntity, state, level, pos, compostable);
        if (j <= 0) {
            cir.setReturnValue(state);
            return;
        }
        BlockState blockstate1 = state.setValue(ComposterBlock.LEVEL, Mth.clamp(i + j, 0, 7));
        if (sourceEntity != null && !CraftEventFactory.callEntityChangeBlockEvent(sourceEntity, pos, blockstate1)) {
            cir.setReturnValue(state);
            return;
        }
        level.setBlockAndUpdate(pos, blockstate1);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, blockstate1));
        if (blockstate1.getValue(ComposterBlock.LEVEL) == 7) {
            level.scheduleTick(pos, state.getBlock(), 20);
        }
        itemStack.shrink(1);
        cir.setReturnValue(blockstate1);
    }

    // CraftBukkit - EntityChangeBlockEvent for non-player extraction
    @Inject(method = "extractProduce", at = @At("HEAD"), cancellable = true)
    private static void silkard$extractProduce(Entity sourceEntity, BlockState state, Level level, BlockPos pos,
                                               CallbackInfoReturnable<BlockState> cir) {
        if (sourceEntity != null && !(sourceEntity instanceof Player)) {
            BlockState blockstate1 = state.setValue(ComposterBlock.LEVEL, 0);
            if (!CraftEventFactory.callEntityChangeBlockEvent(sourceEntity, pos, blockstate1)) {
                cir.setReturnValue(state);
            }
        }
    }

    // CraftBukkit - attach the block inventory holder
    @Inject(method = "getContainer", at = @At("RETURN"))
    private static void silkard$containerOwner(BlockState state, LevelAccessor level, BlockPos pos,
                                               CallbackInfoReturnable<WorldlyContainer> cir) {
        if (cir.getReturnValue() instanceof net.minecraft.world.SimpleContainer simpleContainer) {
            ((ContextContainer) (Object) simpleContainer).setOwner(new CraftBlockInventoryHolder(level, pos, simpleContainer));
        }
    }
}
