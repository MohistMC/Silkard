package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.server.level.ContextServerPlayerGameMode;
import java.util.ArrayList;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(value = ServerPlayerGameMode.class, priority = 900) // applied after Fabric's injectors
public abstract class MixinServerPlayerGameMode implements ContextServerPlayerGameMode {

    @Shadow
    private ServerLevel level;

    @Shadow
    @Final
    private ServerPlayer player;

    @Shadow
    private GameType gameModeForPlayer;

    @Shadow
    public boolean isCreative() {
        throw new AssertionError();
    }

    @Unique
    private BlockBreakEvent silkard$breakEvent;

    @Unique
    private org.bukkit.block.Block silkard$breakBukkitBlock;

    @Unique
    private org.bukkit.block.BlockState silkard$breakBukkitState;

    @Unique
    private BlockState silkard$breakNmsState;

    @Unique
    private int silkard$breakAck = -1;

    @Unique
    private boolean silkard$breakActive;

    @Unique
    private boolean silkard$breakRemoved;

    @Unique
    private boolean silkard$breakPreventsDrops;

    // CB 自增字段，26.3 vanilla 无此成员
    @Unique
    public boolean firedInteract;

    @Unique
    public boolean interactResult;

    @Unique
    public BlockPos interactPosition;

    @Unique
    public InteractionHand interactHand;

    @Unique
    public ItemStack interactItemStack;

    @Override
    public boolean silkard$isFiredInteract() {
        return firedInteract;
    }

    @Override
    public void silkard$setFiredInteract(boolean firedInteract) {
        this.firedInteract = firedInteract;
    }

    @Override
    public boolean silkard$getInteractResult() {
        return interactResult;
    }

    @Override
    public BlockPos silkard$getinteractPosition() {
        return interactPosition;
    }

    @Override
    public InteractionHand silkard$getinteractHand() {
        return interactHand;
    }

    @Override
    public ItemStack silkard$getinteractItemStack() {
        return interactItemStack;
    }

    // CraftBukkit start - BlockBreakEvent
    @Inject(method = "destroyAndAck", at = @At("HEAD"))
    private void silkard$captureBreakAck(BlockPos pos, int sequence, String exitId, CallbackInfo ci) {
        this.silkard$breakAck = sequence;
    }

    @Inject(method = "destroyAndAck", at = @At("RETURN"))
    private void silkard$resetBreakAck(BlockPos pos, int sequence, String exitId, CallbackInfo ci) {
        this.silkard$breakAck = -1;
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void silkard$fireBlockBreakEvent(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.silkard$breakActive = false;
        this.silkard$breakRemoved = false;
        this.silkard$breakPreventsDrops = false;
        this.silkard$breakEvent = null;
        this.silkard$breakBukkitBlock = null;
        this.silkard$breakBukkitState = null;

        BlockState blockstate = this.level.getBlockState(pos);
        org.bukkit.block.Block bblock = CraftBlock.at(this.level, pos);
        boolean isSwordNoBreak = !this.player.getMainHandItem().canDestroyBlock(blockstate, this.level, pos, this.player);

        if (this.level.getBlockEntity(pos) == null && !isSwordNoBreak) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
        }

        BlockBreakEvent event = new BlockBreakEvent(bblock, this.player.getBukkitEntity());
        event.setCancelled(isSwordNoBreak);

        BlockState nmsData = this.level.getBlockState(pos);
        Block nmsBlock = nmsData.getBlock();
        ItemStack itemstack = this.player.getItemBySlot(EquipmentSlot.MAINHAND);

        if (nmsBlock != null && !event.isCancelled() && !this.isCreative() && this.player.hasCorrectToolForDrops(nmsBlock.defaultBlockState())) {
            event.setExpToDrop(nmsBlock.getExpDrop(nmsData, this.level, pos, itemstack, true));
        }

        this.level.getCraftServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            if (!isSwordNoBreak) {
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                for (Direction dir : Direction.values()) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos.relative(dir)));
                }
                this.player.connection.send(new ClientboundBlockChangedAckPacket(this.silkard$breakAck));
                BlockEntity blockentity = this.level.getBlockEntity(pos);
                if (blockentity != null) {
                    this.player.connection.send(blockentity.getUpdatePacket());
                }
            }
            cir.setReturnValue(false);
            return;
        }

        BlockState current = this.level.getBlockState(pos);
        if (current.isAir()) {
            // A plugin removed the block without cancelling
            cir.setReturnValue(false);
            return;
        }

        this.silkard$breakEvent = event;
        this.silkard$breakBukkitBlock = bblock;
        this.silkard$breakNmsState = current;
    }

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void silkard$captureDrops(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.silkard$breakBukkitState = this.silkard$breakBukkitBlock.getState();
        this.silkard$breakActive = true;
        this.level.silkard$setCaptureDrops(new ArrayList<>());
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canDestroyBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean silkard$skipCanDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        // The event already handled the sword and creative pre-cancel
        return true;
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean silkard$removeBlock(ServerLevel level, BlockPos pos, boolean moving) {
        boolean removed = level.removeBlock(pos, moving);
        this.silkard$breakRemoved = removed;
        return removed;
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;preventsBlockDrops()Z"))
    private boolean silkard$skipPreventsBlockDrops(ServerPlayer player) {
        this.silkard$breakPreventsDrops = player.preventsBlockDrops();
        // Keep running so the drop event still fires
        return false;
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void silkard$mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, Player player) {
        if (!this.silkard$breakPreventsDrops) {
            stack.mineBlock(level, state, pos, player);
        }
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void silkard$playerDestroy(Block block, ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!this.silkard$breakPreventsDrops && (this.silkard$breakEvent == null || this.silkard$breakEvent.isDropItems())) {
            block.playerDestroy(level, player, pos, state, blockEntity, tool);
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void silkard$afterDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!this.silkard$breakActive) {
            return;
        }
        this.silkard$breakActive = false;

        BlockBreakEvent event = this.silkard$breakEvent;
        if (event == null || event.isDropItems()) {
            CraftEventFactory.handleBlockDropItemEvent(this.silkard$breakBukkitBlock, this.silkard$breakBukkitState, this.player, this.level.silkard$captureDrops());
        }
        this.level.silkard$setCaptureDrops(null);

        if (this.silkard$breakRemoved && event != null && this.silkard$breakNmsState != null) {
            this.silkard$breakNmsState.getBlock().popExperience(this.level, pos, event.getExpToDrop());
        }
    }
    // CraftBukkit end

    // CraftBukkit start - PlayerGameModeChangeEvent
    @Shadow private int gameTicks;

    @Shadow public abstract void destroyAndAck(BlockPos pos, int sequence, String exitId);

    @Unique private BlockPos silkard$breakPos;
    @Unique private int silkard$breakSeq = -1;
    @Unique private Direction silkard$breakDir;
    @Unique private PlayerInteractEvent silkard$breakInteractEvent;
    @Unique private float silkard$destroyProgress = 1.0F;
    @Unique private PlayerInteractEvent silkard$interactEvent;

    @Inject(method = "changeGameModeForPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getAbilities()Lnet/minecraft/world/entity/player/Abilities;"), cancellable = true)
    private void silkard$gameModeChange(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(this.player.getBukkitEntity(), GameMode.getByValue(gameType.getId()));
        this.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void silkard$syncGameTicks(CallbackInfo ci) {
        this.gameTicks = ((ContextMinecraftServer) this.level.getServer()).silkard$currentTick();
    }
    // CraftBukkit end

    // CraftBukkit start - PlayerInteractEvent on block break
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void silkard$captureBreakArgs(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int sequence, int maxY, CallbackInfo ci) {
        this.silkard$breakPos = pos;
        this.silkard$breakSeq = sequence;
        this.silkard$breakDir = direction;
        this.silkard$destroyProgress = 1.0F;
        this.silkard$breakInteractEvent = null;
    }

    @Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean silkard$mayInteract(ServerLevel level, Entity entity, BlockPos pos) {
        boolean result = level.mayInteract(entity, pos);
        if (!result) {
            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, pos, this.silkard$breakDir, this.player.getInventory().getSelectedItem(), InteractionHand.MAIN_HAND);
        }
        return result;
    }

    @Inject(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void silkard$mayNotInteractUpdate(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int sequence, int maxY, CallbackInfo ci) {
        // SPIGOT-8042: need to ACK above changes first
        this.player.connection.send(new ClientboundBlockChangedAckPacket(sequence));
        BlockEntity blockentity = this.level.getBlockEntity(pos);
        if (blockentity != null) {
            this.player.connection.send(blockentity.getUpdatePacket());
        }
    }

    @Inject(method = "handleBlockBreakAction", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z", opcode = org.objectweb.asm.Opcodes.GETFIELD, shift = At.Shift.BEFORE), cancellable = true)
    private void silkard$interactBreak(CallbackInfo ci) {
        PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, this.silkard$breakPos, this.silkard$breakDir, this.player.getInventory().getSelectedItem(), InteractionHand.MAIN_HAND);
        this.silkard$breakInteractEvent = event;
        if (event.isCancelled()) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, this.silkard$breakPos));
            // SPIGOT-8042: need to ACK above changes first
            this.player.connection.send(new ClientboundBlockChangedAckPacket(this.silkard$breakSeq));
            BlockEntity blockentity = this.level.getBlockEntity(this.silkard$breakPos);
            if (blockentity != null) {
                this.player.connection.send(blockentity.getUpdatePacket());
            }
            ci.cancel();
        }
    }

    @Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;onHitBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Consumer;)V"))
    private void silkard$onHitBlock(ServerLevel level, ItemStack stack, net.minecraft.world.entity.LivingEntity owner, Entity entity, EquipmentSlot slot, net.minecraft.world.phys.Vec3 vec, BlockState state, java.util.function.Consumer<ItemStack> brokenCallback) {
        if (this.silkard$isInteractedDenied()) {
            return;
        }
        net.minecraft.world.item.enchantment.EnchantmentHelper.onHitBlock(level, stack, owner, entity, slot, vec, state, brokenCallback);
    }

    @Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;attack(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void silkard$attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (this.silkard$isInteractedDenied()) {
            return;
        }
        state.attack(level, pos, player);
    }

    @Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float silkard$destroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter getter, BlockPos pos) {
        if (this.silkard$isInteractedDenied()) {
            this.silkard$destroyProgress = 1.0F;
            return 1.0F;
        }
        float progress = state.getDestroyProgress(player, getter, pos);
        this.silkard$destroyProgress = progress;
        return progress;
    }

    @Unique
    private boolean silkard$isInteractedDenied() {
        return this.silkard$breakInteractEvent != null && this.silkard$breakInteractEvent.useInteractedBlock() == Event.Result.DENY;
    }

    @Inject(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z", ordinal = 1), cancellable = true)
    private void silkard$blockDamage(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int sequence, int maxY, CallbackInfo ci) {
        PlayerInteractEvent event = this.silkard$breakInteractEvent;
        if (event == null) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            if (this.silkard$destroyProgress > 1.0F) {
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
            }
            ci.cancel();
            return;
        }
        BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.player, pos, this.player.getInventory().getSelectedItem(), this.silkard$destroyProgress >= 1.0F);
        if (blockEvent.isCancelled()) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
            ci.cancel();
            return;
        }
        if (blockEvent.getInstaBreak() && !this.level.getBlockState(pos).isAir()) {
            this.destroyAndAck(pos, sequence, "insta mine");
            ci.cancel();
        }
    }
    // CraftBukkit end

    // CraftBukkit start - abort destroy
    @Redirect(method = "abortDestroyBlock", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void silkard$mismatchDebug(Logger logger, String message, Object arg1, Object arg2) {
        logger.debug(message, arg1, arg2);
    }

    @Inject(method = "abortDestroyBlock", at = @At("TAIL"))
    private void silkard$abortDamage(BlockPos pos, int sequence, CallbackInfo ci) {
        CraftEventFactory.callBlockDamageAbortEvent(this.player, pos, this.player.getInventory().getSelectedItem());
    }
    // CraftBukkit end

    // CraftBukkit start - PlayerInteractEvent on right click
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void silkard$interactOnBlock(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos blockpos = hitResult.getBlockPos();
        BlockState blockstate = level.getBlockState(blockpos);
        boolean cancelledBlock = false;
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider menuprovider = blockstate.getMenuProvider(level, blockpos);
            cancelledBlock = menuprovider == null;
        }
        if (player.getCooldowns().isOnCooldown(itemStack)) {
            cancelledBlock = true;
        }
        PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, blockpos, hitResult.getDirection(), itemStack, cancelledBlock, hand, hitResult.getLocation());
        this.firedInteract = true;
        this.interactResult = event.useItemInHand() == Event.Result.DENY;
        this.interactPosition = blockpos.immutable();
        this.interactHand = hand;
        this.interactItemStack = itemStack.copy();
        this.silkard$interactEvent = event;
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"))
    private OptionalInt silkard$openMenu(ServerPlayer player, MenuProvider menuProvider) {
        if (this.silkard$interactEvent != null && this.silkard$interactEvent.useInteractedBlock() == Event.Result.DENY) {
            return OptionalInt.empty();
        }
        return player.openMenu(menuProvider);
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;isOnCooldown(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean silkard$interactCooldown(ItemCooldowns cooldowns, ItemStack stack) {
        return this.interactResult || cooldowns.isOnCooldown(stack);
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult silkard$useItemOn(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (this.silkard$interactEvent != null && this.silkard$interactEvent.useInteractedBlock() == Event.Result.DENY) {
            this.silkard$correctBlock(state, level, hitResult);
            return InteractionResult.PASS;
        }
        return state.useItemOn(stack, level, player, hand, hitResult);
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult silkard$useWithoutItem(BlockState state, Level level, Player player, BlockHitResult hitResult) {
        if (this.silkard$interactEvent != null && this.silkard$interactEvent.useInteractedBlock() == Event.Result.DENY) {
            this.silkard$correctBlock(state, level, hitResult);
            return InteractionResult.PASS;
        }
        return state.useWithoutItem(level, player, hitResult);
    }

    @Unique
    private void silkard$correctBlock(BlockState state, Level level, BlockHitResult hitResult) {
        BlockPos blockpos = hitResult.getBlockPos();
        if (state.getBlock() instanceof DoorBlock) {
            boolean bottom = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
            this.player.connection.send(new ClientboundBlockUpdatePacket(level, bottom ? blockpos.above() : blockpos.below()));
        } else if (state.getBlock() instanceof CakeBlock) {
            this.player.getBukkitEntity().sendHealthUpdate();
        } else if (this.interactItemStack != null && this.interactItemStack.getItem() instanceof DoubleHighBlockItem) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(level, blockpos.relative(hitResult.getDirection()).above()));
            this.player.connection.send(new ClientboundBlockUpdatePacket(level, blockpos.above()));
        }
        this.player.getBukkitEntity().updateInventory();
    }
    // CraftBukkit end
}
