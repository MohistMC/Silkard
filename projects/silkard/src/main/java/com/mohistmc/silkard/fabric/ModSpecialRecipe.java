package com.mohistmc.silkard.fabric;

import com.mohistmc.silkard.api.ServerAPI;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ModSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public ModSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new ItemStack(Material.AIR), null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asBukkitCopy(
            ((Recipe) this.recipe).assemble(new SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY)));
    }

    public void addToRecipeManager() {
        ServerAPI.getNMSServer().getRecipeManager().addRecipe(
            new RecipeHolder<>(CraftRecipe.toMinecraft(this.getKey()), this.recipe));
    }
}
