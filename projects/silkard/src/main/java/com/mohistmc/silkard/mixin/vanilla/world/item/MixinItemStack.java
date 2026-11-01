package com.mohistmc.silkard.mixin.vanilla.world.item;

import com.mohistmc.silkard.bukkit.BukkitUtils;
import com.mohistmc.silkard.injected.world.item.ContextItemStack;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import com.mohistmc.silkard.injected.world.level.block.state.ContextBlockBehaviour;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/5/1 17:38
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack implements ContextItemStack {

    @Shadow
    private @Nullable Holder<Item> item;

    @Shadow
    @Final
    private PatchedDataComponentMap components;

    @Deprecated
    @Override
    public void setItem(Holder<Item> item) {
        this.item = item;
    }

    // CraftBukkit start
    @Override
    public void restorePatch(DataComponentPatch patch) {
        this.components.restorePatch(patch);
    }

    @Override
    public DataComponentPatch asPatch() {
        return this.components.asPatch();
    }

    @Shadow
    private int count;

    @Shadow
    public abstract Item getItem();

    @Shadow
    public abstract int getCount();

    @Shadow
    public abstract void setCount(int count);

    @Invoker("processDurabilityChange")
    abstract int silkard$processDurabilityChange(int amount, ServerLevel level, @Nullable ServerPlayer player);

    @Redirect(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;)I"))
    private int silkard$playerItemDamage(ItemStack stack, int amount, ServerLevel level, @Nullable ServerPlayer player) {
        int j = this.silkard$processDurabilityChange(amount, level, player);
        if (player != null) {
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(player.getBukkitEntity(), CraftItemStack.asCraftMirror(stack), j);
            event.getPlayer().getServer().getPluginManager().callEvent(event);
            if (j != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                return 0;
            }
            j = event.getDamage();
        }
        return j;
    }

    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void silkard$itemBreak(int newDamage, @Nullable ServerPlayer player, Consumer<ItemStack> onBreak, CallbackInfo ci) {
        if (this.count == 1 && player != null) {
            CraftEventFactory.callPlayerItemBreakEvent(player, (ItemStack) (Object) this);
        }
    }

    @Inject(method = "consume", at = @At("HEAD"), cancellable = true)
    private void silkard$consumeEmpty(int amount, LivingEntity owner, CallbackInfo ci) {
        if ((Object) this == ItemStack.EMPTY) {
            ci.cancel();
        }
    }

    // CraftBukkit start - handle all block place event logic here
    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult silkard$useOn(Item item, UseOnContext context) {
        DataComponentPatch oldData = this.components.asPatch();
        int oldCount = this.getCount();
        ServerLevel world = (ServerLevel) context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Player player = context.getPlayer();
        ContextLevel levelHandle = (ContextLevel) world;

        if (!(item instanceof BucketItem || item instanceof SolidBucketItem)) {
            levelHandle.silkard$captureBlockStates(true);
            if (item == Items.BONE_MEAL) {
                levelHandle.silkard$captureTreeGeneration(true);
            }
        }
        InteractionResult interactionresult;
        try {
            interactionresult = item.useOn(context);
        } finally {
            levelHandle.silkard$captureBlockStates(false);
        }
        DataComponentPatch newData = this.components.asPatch();
        int newCount = this.getCount();
        this.setCount(oldCount);
        this.restorePatch(oldData);
        if (interactionresult.consumesAction() && levelHandle.silkard$captureTreeGeneration() && !levelHandle.silkard$capturedBlockStates().isEmpty()) {
            levelHandle.silkard$captureTreeGeneration(false);
            Location location = CraftLocation.toBukkit(blockpos, world.getWorld());
            TreeType treeType = BukkitUtils.treeType;
            BukkitUtils.treeType = null;
            List<CraftBlockState> blocks = new ArrayList<>(levelHandle.silkard$capturedBlockStates().values());
            levelHandle.silkard$capturedBlockStates().clear();
            StructureGrowEvent structureEvent = null;
            if (treeType != null) {
                boolean isBonemeal = this.getItem() == Items.BONE_MEAL;
                structureEvent = new StructureGrowEvent(location, treeType, isBonemeal, (org.bukkit.entity.Player) player.getBukkitEntity(), (List) blocks);
                Bukkit.getPluginManager().callEvent(structureEvent);
            }

            BlockFertilizeEvent fertilizeEvent = new BlockFertilizeEvent(CraftBlock.at(world, blockpos), (org.bukkit.entity.Player) player.getBukkitEntity(), (List) blocks);
            fertilizeEvent.setCancelled(structureEvent != null && structureEvent.isCancelled());
            Bukkit.getPluginManager().callEvent(fertilizeEvent);

            if (!fertilizeEvent.isCancelled()) {
                if (this.getCount() == oldCount && Objects.equals(this.components.asPatch(), oldData)) {
                    this.restorePatch(newData);
                    this.setCount(newCount);
                }
                for (CraftBlockState blockstate : blocks) {
                    CapturedBlockState.setBlockState(blockstate);
                }
                player.awardStat(Stats.ITEM_USED.get(item));
            }
            return interactionresult;
        }
        levelHandle.silkard$captureTreeGeneration(false);

        if (player != null && interactionresult instanceof InteractionResult.Success success) {
            if (success.wasItemInteraction()) {
                InteractionHand interactionhand = context.getHand();
                BlockPlaceEvent placeEvent = null;
                List<org.bukkit.block.BlockState> blocks = new ArrayList<>(levelHandle.silkard$capturedBlockStates().values());
                levelHandle.silkard$capturedBlockStates().clear();
                if (blocks.size() > 1) {
                    placeEvent = CraftEventFactory.callBlockMultiPlaceEvent(world, player, interactionhand, blocks, blockpos.getX(), blockpos.getY(), blockpos.getZ());
                } else if (blocks.size() == 1) {
                    placeEvent = CraftEventFactory.callBlockPlaceEvent(world, player, interactionhand, blocks.get(0), blockpos.getX(), blockpos.getY(), blockpos.getZ());
                }

                if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                    interactionresult = InteractionResult.FAIL;
                    placeEvent.getPlayer().updateInventory();
                    levelHandle.silkard$setPreventPoiUpdated(true);
                    for (org.bukkit.block.BlockState blockstate : blocks) {
                        blockstate.update(true, false);
                    }
                    levelHandle.silkard$setPreventPoiUpdated(false);

                    BlockPos placedPos = ((CraftBlock) placeEvent.getBlock()).getPosition();
                    for (Direction dir : Direction.values()) {
                        ((ServerPlayer) player).connection.send(new ClientboundBlockUpdatePacket(world, placedPos.relative(dir)));
                    }
                } else {
                    if (this.getCount() == oldCount && Objects.equals(this.components.asPatch(), oldData)) {
                        this.restorePatch(newData);
                        this.setCount(newCount);
                    }

                    for (Map.Entry<BlockPos, BlockEntity> entry : levelHandle.silkard$capturedTileEntities().entrySet()) {
                        world.setBlockEntity(entry.getValue());
                    }

                    for (org.bukkit.block.BlockState blockstate : blocks) {
                        int updateFlag = ((CraftBlockState) blockstate).getFlag();
                        BlockState oldBlock = ((CraftBlockState) blockstate).getHandle();
                        BlockPos newblockposition = ((CraftBlockState) blockstate).getPosition();
                        BlockState block = world.getBlockState(newblockposition);

                        if (!(block.getBlock() instanceof BaseEntityBlock)) {
                            ((ContextBlockBehaviour) (Object) block.getBlock()).onPlace(block, world, newblockposition, oldBlock, true, context);
                        }

                        levelHandle.notifyAndUpdatePhysics(newblockposition, null, oldBlock, block, world.getBlockState(newblockposition), updateFlag, 512);
                    }

                    // Special case skulls to allow wither spawns to be cancelled
                    if (this.getItem() == Items.WITHER_SKELETON_SKULL) {
                        BlockPos bp = blockpos;
                        if (!world.getBlockState(blockpos).canBeReplaced()) {
                            if (!world.getBlockState(blockpos).isSolid()) {
                                bp = null;
                            } else {
                                bp = bp.relative(context.getClickedFace());
                            }
                        }
                        if (bp != null) {
                            BlockEntity te = world.getBlockEntity(bp);
                            if (te instanceof SkullBlockEntity skull) {
                                WitherSkullBlock.checkSpawn(world, bp, skull);
                            }
                        }
                    }

                    // SPIGOT-7315: moved from BlockBed#setPlacedBy
                    if (placeEvent != null && this.getItem() instanceof BlockItem bedItem && bedItem.getBlock() instanceof BedBlock) {
                        BlockPos position = ((CraftBlock) placeEvent.getBlock()).getPosition();
                        BlockState blockData = world.getBlockState(position);

                        if (blockData.getBlock() instanceof BedBlock) {
                            world.updateNeighborsAt(position, Blocks.AIR);
                            blockData.updateNeighbourShapes(world, position, 3);
                        }
                    }

                    if (this.getItem() instanceof BlockItem blockItem) {
                        SoundType soundtype = blockItem.getBlock().defaultBlockState().getSoundType();
                        world.playSound(player, blockpos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                    }

                    player.awardStat(Stats.ITEM_USED.get(item));
                }
            }
        }
        levelHandle.silkard$capturedTileEntities().clear();
        levelHandle.silkard$capturedBlockStates().clear();
        return interactionresult;
    }

    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;)V"))
    private void silkard$awardStat(Player player, Stat<?> stat) {
        // handled inside silkard$useOn
    }
    // CraftBukkit end
}
