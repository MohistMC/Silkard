package com.mohistmc.silkard.mixin.vanilla.core.cauldron;

import com.mohistmc.silkard.bukkit.BukkitUtils;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(CauldronInteractions.class)
public abstract class MixinCauldronInteractions {

    @Unique
    private static final String SILKARD$CREATE_FILLED = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;";
    @Unique
    private static final String SILKARD$TRANSMUTE = "Lnet/minecraft/world/item/ItemStack;transmuteCopy(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/item/ItemStack;";
    @Unique
    private static final String SILKARD$COPY_WITH_COUNT = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;";
    @Unique
    private static final String SILKARD$REMOVE = "Lnet/minecraft/world/item/ItemStack;remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;";
    @Unique
    private static final String SILKARD$SET_BLOCK = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z";
    @Unique
    private static final String SILKARD$LOWER_FILL = "Lnet/minecraft/world/level/block/LayeredCauldronBlock;lowerFillLevel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V";

    @Inject(method = "fillBucket", at = @At(value = "INVOKE", target = SILKARD$CREATE_FILLED), cancellable = true)
    private static void silkard$fillBucket(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                           ItemStack itemInHand, ItemStack newItem, Predicate<BlockState> canFill,
                                           SoundEvent soundEvent, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.changeCauldronLevel(level, pos, Blocks.CAULDRON.defaultBlockState(), player, CauldronLevelChangeEvent.ChangeReason.BUCKET_FILL)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "emptyBucket", at = @At(value = "INVOKE", target = SILKARD$CREATE_FILLED), cancellable = true)
    private static void silkard$emptyBucket(Level level, BlockPos pos, Player player, InteractionHand hand,
                                            ItemStack itemInHand, BlockState newState, SoundEvent soundEvent,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.changeCauldronLevel(level, pos, newState, player, CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "lambda$bootStrap$0", at = @At(value = "INVOKE", target = SILKARD$CREATE_FILLED), cancellable = true)
    private static void silkard$emptyBottle(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                            ItemStack itemStack, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.changeCauldronLevel(level, pos, Blocks.WATER_CAULDRON.defaultBlockState(), player, CauldronLevelChangeEvent.ChangeReason.BOTTLE_EMPTY)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "lambda$bootStrap$4", at = @At(value = "INVOKE", target = SILKARD$CREATE_FILLED), cancellable = true)
    private static void silkard$addBottle(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack itemStack, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.changeCauldronLevel(level, pos, state.cycle(LayeredCauldronBlock.LEVEL), player, CauldronLevelChangeEvent.ChangeReason.BOTTLE_EMPTY)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "lambda$bootStrap$3", at = @At(value = "INVOKE", target = SILKARD$CREATE_FILLED), cancellable = true)
    private static void silkard$fillBottle(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                           ItemStack itemStack, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.lowerCauldronFillLevel(state, level, pos, player, CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "shulkerBoxInteraction", at = @At(value = "INVOKE", target = SILKARD$TRANSMUTE), cancellable = true)
    private static void silkard$washShulkerBox(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                               ItemStack itemStack, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.lowerCauldronFillLevel(state, level, pos, player, CauldronLevelChangeEvent.ChangeReason.SHULKER_WASH)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "bannerInteraction", at = @At(value = "INVOKE", target = SILKARD$COPY_WITH_COUNT), cancellable = true)
    private static void silkard$washBanner(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                           ItemStack itemStack, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.lowerCauldronFillLevel(state, level, pos, player, CauldronLevelChangeEvent.ChangeReason.BANNER_WASH)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "dyedItemIteration", at = @At(value = "INVOKE", target = SILKARD$REMOVE), cancellable = true)
    private static void silkard$washArmor(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack itemStack, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BukkitUtils.lowerCauldronFillLevel(state, level, pos, player, CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Redirect(method = {"fillBucket", "emptyBucket", "lambda$bootStrap$0", "lambda$bootStrap$4"},
            at = @At(value = "INVOKE", target = SILKARD$SET_BLOCK))
    private static boolean silkard$skipSetBlock(Level level, BlockPos pos, BlockState newState) {
        return true;
    }

    @Redirect(method = {"lambda$bootStrap$3", "shulkerBoxInteraction", "bannerInteraction", "dyedItemIteration"},
            at = @At(value = "INVOKE", target = SILKARD$LOWER_FILL))
    private static void silkard$skipLowerFill(BlockState state, Level level, BlockPos pos) {
    }
}
