package com.mohistmc.silkard.injected.world.level.block.entity;

import com.mohistmc.silkard.util.ContextStateException;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BrewingFuel;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public interface ContextBrewingStand {

    default NonNullList<ItemStack> silkard$items() {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$brewTime() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$brewTime(int brewTime) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$totalBrewTime() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$totalBrewTime(int totalBrewTime) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$fuel() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$fuel(int fuel) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$totalFuel() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$totalFuel(int totalFuel) {
        throw new ContextStateException("Not implemented");
    }

    default float silkard$speedMultiplier() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$speedMultiplier(float speedMultiplier) {
        throw new ContextStateException("Not implemented");
    }

    default Item silkard$ingredient() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$ingredient(Item ingredient) {
        throw new ContextStateException("Not implemented");
    }

    default boolean[] silkard$lastPotionCount() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$lastPotionCount(boolean[] lastPotionCount) {
        throw new ContextStateException("Not implemented");
    }

    default boolean[] silkard$potionBits() {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$lastTick() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$lastTick(int lastTick) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$getUses(ServerLevel level, BrewingFuel fuel) {
        throw new ContextStateException("Not implemented");
    }

    default float silkard$getSpeedMultiplier(ServerLevel level, BrewingFuel fuel) {
        throw new ContextStateException("Not implemented");
    }

    default Optional<RecipeHolder<BrewingRecipe>> silkard$quickCheck(BrewingInput input, ServerLevel level) {
        throw new ContextStateException("Not implemented");
    }
}
