package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import com.mohistmc.silkard.injected.world.ContextContainer;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
// priority 900 < fabric-api 的 1000：@Overwrite 会给目标方法打 @MixinMerged，
// 低 priority 先应用，fabric 后续注入时 checkPriority(900, 1000) 才能放行
@Mixin(value = CrafterBlock.class, priority = 900)
public abstract class MixinCrafterBlock {

    // CraftBukkit start - CrafterCraftEvent
    @Overwrite
    protected void dispenseFrom(final BlockState state, final ServerLevel level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrafterBlockEntity blockEntity) {
            CraftingInput input = blockEntity.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipe = CrafterBlock.getPotentialResults(level, input);
            if (recipe.isEmpty()) {
                level.levelEvent(1050, pos, 0);
            } else {
                RecipeHolder<CraftingRecipe> pickedRecipe = recipe.get();
                ItemStack results = pickedRecipe.value().assemble(input);

                CrafterCraftEvent event = CraftEventFactory.callCrafterCraftEvent(pos, level, blockEntity, results,
                        pickedRecipe.value().getRemainingItems(input), pickedRecipe);
                if (event.isCancelled()) {
                    return;
                }
                results = CraftItemStack.asNMSCopy(event.getResult());

                if (results.isEmpty()) {
                    level.levelEvent(1050, pos, 0);
                } else {
                    blockEntity.setCraftingTicksRemaining(6);
                    level.setBlock(pos, state.setValue(BlockStateProperties.CRAFTING, true), 2);
                    results.onCraftedBySystem(level);
                    this.dispenseItem(level, pos, blockEntity, results, state, pickedRecipe);

                    for (ItemStack remainingItem : event.getRemainingItems().stream().map(CraftItemStack::asNMSCopy).toList()) {
                        if (!remainingItem.isEmpty()) {
                            this.dispenseItem(level, pos, blockEntity, remainingItem, state, pickedRecipe);
                        }
                    }

                    blockEntity.getItems().forEach(it -> {
                        if (!it.isEmpty()) {
                            it.shrink(1);
                        }
                    });
                    blockEntity.setChanged();
                }
            }
        }
    }
    // CraftBukkit end

    // CraftBukkit start - InventoryMoveItemEvent
    @Overwrite
    private void dispenseItem(
            final ServerLevel level,
            final BlockPos pos,
            final CrafterBlockEntity blockEntity,
            final ItemStack results,
            final BlockState blockState,
            final RecipeHolder<?> recipe
    ) {
        Direction direction = blockState.getValue(BlockStateProperties.ORIENTATION).front();
        Container into = HopperBlockEntity.getContainerAt(level, pos.relative(direction));
        ItemStack remaining = results.copy();
        if (into != null && (into instanceof CrafterBlockEntity || results.getCount() > into.getMaxStackSize(results))) {
            InventoryMoveItemEvent event = this.silkard$moveItemEvent(level, blockEntity, into, remaining);
            remaining = CraftItemStack.asNMSCopy(event.getItem());
            while (!remaining.isEmpty()) {
                if (event.isCancelled()) {
                    break;
                }

                ItemStack copy = remaining.copyWithCount(1);
                ItemStack itemStack = HopperBlockEntity.addItem(blockEntity, into, copy, direction.getOpposite());
                if (!itemStack.isEmpty()) {
                    break;
                }

                remaining.shrink(1);
            }
        } else if (into != null) {
            InventoryMoveItemEvent event = this.silkard$moveItemEvent(level, blockEntity, into, remaining);
            remaining = CraftItemStack.asNMSCopy(event.getItem());
            while (!remaining.isEmpty()) {
                if (event.isCancelled()) {
                    break;
                }

                int oldSize = remaining.getCount();
                remaining = HopperBlockEntity.addItem(blockEntity, into, remaining, direction.getOpposite());
                if (oldSize == remaining.getCount()) {
                    break;
                }
            }
        }

        if (!remaining.isEmpty()) {
            net.minecraft.world.phys.Vec3 centerPos = net.minecraft.world.phys.Vec3.atCenterOf(pos);
            net.minecraft.world.phys.Vec3 itemSpawnOffset = centerPos.relative(direction, 0.7);
            net.minecraft.core.dispenser.DefaultDispenseItemBehavior.spawnItem(level, remaining, 6, direction, itemSpawnOffset);

            for (net.minecraft.server.level.ServerPlayer player : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, net.minecraft.world.phys.AABB.ofSize(centerPos, 17.0, 17.0, 17.0))) {
                net.minecraft.advancements.triggers.CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.trigger(player, recipe.id(), blockEntity.getItems());
            }

            level.levelEvent(1049, pos, 0);
            level.levelEvent(2010, pos, direction.get3DDataValue());
        }
    }
    // CraftBukkit end

    private InventoryMoveItemEvent silkard$moveItemEvent(ServerLevel level, CrafterBlockEntity blockEntity, Container into, ItemStack item) {
        CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(item);

        Inventory destinationInventory;
        // Have to special case large chests as they work oddly
        if (into instanceof CompoundContainer) {
            destinationInventory = new CraftInventoryDoubleChest((CompoundContainer) into);
        } else {
            destinationInventory = ((ContextContainer) into).getOwner().getInventory();
        }

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(((ContextContainer) blockEntity).getOwner().getInventory(), craftItemStack, destinationInventory, true);
        ((ContextLevel) (Object) level).getCraftServer().getPluginManager().callEvent(event);
        return event;
    }
}
