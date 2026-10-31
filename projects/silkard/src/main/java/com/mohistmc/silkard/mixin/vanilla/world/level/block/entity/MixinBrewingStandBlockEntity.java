package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.world.ContextContainer;
import com.mohistmc.silkard.injected.world.level.block.entity.ContextBrewingStand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BrewingFuel;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(value = BrewingStandBlockEntity.class, priority = 900)
public abstract class MixinBrewingStandBlockEntity implements ContextBrewingStand {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    @Shadow private int brewTime;
    @Shadow private int totalBrewTime;
    @Shadow private int fuel;
    @Shadow private int totalFuel;
    @Shadow private float speedMultiplier;
    @Shadow private Item ingredient;
    @Shadow private boolean[] lastPotionCount;
    @Shadow @Final private RecipeManager.CachedCheck<BrewingInput, BrewingRecipe> quickCheck;

    @Shadow protected abstract NonNullList<ItemStack> getItems();
    @Shadow protected abstract int getUses(ServerLevel level, BrewingFuel fuel);
    @Shadow protected abstract float getSpeedMultiplier(ServerLevel level, BrewingFuel fuel);

    @Shadow
    private static void doBrew(ServerLevel level, BlockPos pos, BrewingStandBlockEntity entity) {
        throw new AssertionError();
    }

    @Unique public List<HumanEntity> transaction = new ArrayList<>();
    @Unique private int maxStack = ContextContainer.MAX_STACK;
    @Unique private int silkard$lastTick;
    // @formatter:on

    @Unique
    private static void silkard$setChanged(Level level, BlockPos pos, BlockState state) {
        level.blockEntityChanged(pos);
        if (!state.isAir()) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    @Unique
    private static boolean silkard$isBrewable(ServerLevel level, BrewingStandBlockEntity entity) {
        ContextBrewingStand stand = (ContextBrewingStand) entity;
        NonNullList<ItemStack> items = stand.silkard$items();
        ItemStack ingredientStack = items.get(3);
        if (ingredientStack.isEmpty()) {
            return false;
        }
        RecipeManager recipeManager = level.recipeAccess();
        if (!recipeManager.propertySet(RecipePropertySet.BREWING_REAGENTS).test(ingredientStack)) {
            return false;
        }
        for (int i = 0; i < 3; ++i) {
            ItemStack bottleStack = items.get(i);
            if (bottleStack.isEmpty()) {
                continue;
            }
            if (stand.silkard$quickCheck(new BrewingInput(bottleStack, ingredientStack), level).isPresent()) {
                return true;
            }
        }
        return false;
    }

    // CraftBukkit start
    public List<ItemStack> getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
    // CraftBukkit end

    @Override
    public NonNullList<ItemStack> silkard$items() {
        return this.items;
    }

    @Override
    public int silkard$brewTime() {
        return this.brewTime;
    }

    @Override
    public void silkard$brewTime(int brewTime) {
        this.brewTime = brewTime;
    }

    @Override
    public int silkard$totalBrewTime() {
        return this.totalBrewTime;
    }

    @Override
    public void silkard$totalBrewTime(int totalBrewTime) {
        this.totalBrewTime = totalBrewTime;
    }

    @Override
    public int silkard$fuel() {
        return this.fuel;
    }

    @Override
    public void silkard$fuel(int fuel) {
        this.fuel = fuel;
    }

    @Override
    public int silkard$totalFuel() {
        return this.totalFuel;
    }

    @Override
    public void silkard$totalFuel(int totalFuel) {
        this.totalFuel = totalFuel;
    }

    @Override
    public float silkard$speedMultiplier() {
        return this.speedMultiplier;
    }

    @Override
    public void silkard$speedMultiplier(float speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    public Item silkard$ingredient() {
        return this.ingredient;
    }

    @Override
    public void silkard$ingredient(Item ingredient) {
        this.ingredient = ingredient;
    }

    @Override
    public boolean[] silkard$lastPotionCount() {
        return this.lastPotionCount;
    }

    @Override
    public void silkard$lastPotionCount(boolean[] lastPotionCount) {
        this.lastPotionCount = lastPotionCount;
    }

    @Override
    public boolean[] silkard$potionBits() {
        boolean[] potionBits = new boolean[3];
        for (int i = 0; i < 3; ++i) {
            potionBits[i] = !this.items.get(i).isEmpty();
        }
        return potionBits;
    }

    @Override
    public int silkard$lastTick() {
        return this.silkard$lastTick;
    }

    @Override
    public void silkard$lastTick(int lastTick) {
        this.silkard$lastTick = lastTick;
    }

    @Override
    public int silkard$getUses(ServerLevel level, BrewingFuel fuel) {
        return this.getUses(level, fuel);
    }

    @Override
    public float silkard$getSpeedMultiplier(ServerLevel level, BrewingFuel fuel) {
        return this.getSpeedMultiplier(level, fuel);
    }

    @Override
    public Optional<RecipeHolder<BrewingRecipe>> silkard$quickCheck(BrewingInput input, ServerLevel level) {
        return this.quickCheck.getRecipeFor(input, level);
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - fuel / brewing start events and wall time
     */
    @Overwrite
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity) {
        ContextBrewingStand stand = (ContextBrewingStand) entity;
        NonNullList<ItemStack> items = stand.silkard$items();
        ItemStack fuelStack = items.get(4);
        BrewingFuel brewingFuel = fuelStack.get(DataComponents.BREWING_FUEL);

        if (stand.silkard$fuel() <= 0 && brewingFuel != null) {
            BrewingStandFuelEvent event = new BrewingStandFuelEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(fuelStack), stand.silkard$getUses(level, brewingFuel));
            level.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            stand.silkard$fuel(event.getFuelPower());
            stand.silkard$totalFuel(stand.silkard$fuel());
            stand.silkard$speedMultiplier(stand.silkard$getSpeedMultiplier(level, brewingFuel));
            ItemStackTemplate remainder = fuelStack.getItem().getCraftingRemainder();
            ItemStack newFuelStack = fuelStack;
            if (stand.silkard$fuel() > 0 && event.isConsuming()) {
                fuelStack.shrink(1);
            }
            if (remainder != null) {
                if (fuelStack.isEmpty()) {
                    newFuelStack = remainder.create();
                } else {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
                }
            }
            items.set(4, newFuelStack);
            silkard$setChanged(level, pos, state);
        }

        int currentTick = ((ContextMinecraftServer) (Object) level.getServer()).silkard$currentTick();
        int previousTick = stand.silkard$lastTick();
        int elapsedTicks = currentTick - (previousTick == 0 ? currentTick : previousTick);
        stand.silkard$lastTick(currentTick);

        boolean brewable = silkard$isBrewable(level, entity);
        boolean brewing = stand.silkard$brewTime() > 0;
        ItemStack ingredientStack = stand.silkard$items().get(3);

        if (brewing) {
            stand.silkard$brewTime(stand.silkard$brewTime() - elapsedTicks);
            boolean done = stand.silkard$brewTime() <= 0;
            if (done && brewable) {
                doBrew(level, pos, entity);
            } else if (!brewable || !ingredientStack.is(stand.silkard$ingredient())) {
                stand.silkard$brewTime(0);
            }
            silkard$setChanged(level, pos, state);
        } else if (brewable && stand.silkard$fuel() > 0) {
            float speed = stand.silkard$speedMultiplier() > 0.0F ? stand.silkard$speedMultiplier() : 1.0F;
            stand.silkard$fuel(stand.silkard$fuel() - 1);
            BrewingStartEvent event = new BrewingStartEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(ingredientStack), (int) Math.ceil(400.0F / speed));
            level.getCraftServer().getPluginManager().callEvent(event);
            stand.silkard$brewTime(event.getTotalBrewTime());
            stand.silkard$totalBrewTime(stand.silkard$brewTime());
            stand.silkard$ingredient(ingredientStack.getItem());
            silkard$setChanged(level, pos, state);
        }

        boolean[] potionBits = stand.silkard$potionBits();
        if (!Arrays.equals(potionBits, stand.silkard$lastPotionCount())) {
            stand.silkard$lastPotionCount(potionBits);
            BlockState blockstate = state;
            if (!(blockstate.getBlock() instanceof BrewingStandBlock)) {
                return;
            }
            for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; ++i) {
                blockstate = blockstate.setValue(BrewingStandBlock.HAS_BOTTLE[i], potionBits[i]);
            }
            level.setBlock(pos, blockstate, 2);
        }
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - BrewEvent with plugin-visible results
     */
    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void silkard$brewEvent(ServerLevel level, BlockPos pos, BrewingStandBlockEntity entity, CallbackInfo ci,
                                          @Share("brewResults") LocalRef<List<org.bukkit.inventory.ItemStack>> brewResults) {
        brewResults.set(null);
        InventoryHolder owner = ((ContextContainer) entity).getOwner();
        if (owner == null) {
            return;
        }

        ContextBrewingStand stand = (ContextBrewingStand) entity;
        NonNullList<ItemStack> items = stand.silkard$items();
        ItemStack ingredientStack = items.get(3);
        List<org.bukkit.inventory.ItemStack> results = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            ItemStack bottleStack = items.get(i);
            BrewingInput input = new BrewingInput(bottleStack, ingredientStack);
            Optional<RecipeHolder<BrewingRecipe>> optional = stand.silkard$quickCheck(input, level);
            results.add(i, CraftItemStack.asCraftMirror(optional.isPresent() ? optional.get().value().assemble(input) : bottleStack));
        }

        BrewEvent event = new BrewEvent(CraftBlock.at(level, pos), (BrewerInventory) owner.getInventory(), results, stand.silkard$fuel());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        brewResults.set(results);
    }

    // CraftBukkit - apply plugin-modified results, validating the index in case it is cleared
    @Inject(method = "doBrew", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private static void silkard$applyBrewResults(ServerLevel level, BlockPos pos, BrewingStandBlockEntity entity, CallbackInfo ci,
                                                 @Share("brewResults") LocalRef<List<org.bukkit.inventory.ItemStack>> brewResults) {
        List<org.bukkit.inventory.ItemStack> results = brewResults.get();
        if (results == null) {
            return;
        }
        NonNullList<ItemStack> items = ((ContextBrewingStand) entity).silkard$items();
        for (int i = 0; i < 3; ++i) {
            items.set(i, i < results.size() ? CraftItemStack.asNMSCopy(results.get(i)) : ItemStack.EMPTY);
        }
    }
}
