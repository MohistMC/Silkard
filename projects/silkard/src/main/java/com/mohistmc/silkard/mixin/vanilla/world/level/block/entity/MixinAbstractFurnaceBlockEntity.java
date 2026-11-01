package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mohistmc.silkard.injected.world.level.block.entity.ContextAbstractFurnaceBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.CookingRecipe;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity implements ContextAbstractFurnaceBlockEntity {

    // CraftBukkit start
    @Shadow
    protected NonNullList<ItemStack> items;

    @Shadow
    @Final
    private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;

    @Shadow
    private int cookingTimer;

    @Shadow
    private int cookingTotalTime;

    @Shadow
    @Final
    private RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

    @Shadow
    protected abstract int getBurnDuration(ServerLevel level, ItemStack stack);

    @Shadow
    private static void consumeFuel(ServerLevel level, BlockPos pos, NonNullList<ItemStack> items, ItemStack fuel) {
        throw new AssertionError();
    }

    // serverTick is static, so its handlers cannot use `this`; keep the ticked furnace and the CB tick state here
    @Unique
    private static AbstractFurnaceBlockEntity silkard$currentFurnace;

    @Unique
    private static boolean silkard$burnCancelled;

    @Unique
    private static boolean silkard$burning;

    @Unique
    private static boolean silkard$smeltCancelled;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void silkard$beginTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        silkard$currentFurnace = entity;
        silkard$burnCancelled = false;
        silkard$burning = false;
        silkard$smeltCancelled = false;
    }

    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int silkard$furnaceBurnEvent(AbstractFurnaceBlockEntity entity, ServerLevel level, ItemStack fuel) {
        int burnTime = ((ContextAbstractFurnaceBlockEntity) entity).silkard$getBurnDuration(level, fuel);
        FurnaceBurnEvent event = new FurnaceBurnEvent(CraftBlock.at(level, entity.getBlockPos()), CraftItemStack.asCraftMirror(fuel), burnTime);
        level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            silkard$burnCancelled = true;
            return burnTime;
        }
        silkard$burning = event.isBurning();
        return event.getBurnTime();
    }

    @Inject(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getSpeedMultiplier(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)F"), cancellable = true)
    private static void silkard$cancelBurnTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        if (silkard$burnCancelled) {
            ci.cancel();
        }
    }

    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;consumeFuel(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void silkard$consumeFuel(ServerLevel level, BlockPos pos, NonNullList<ItemStack> items, ItemStack fuel) {
        if (!silkard$burning) {
            return;
        }
        consumeFuel(level, pos, items, fuel);
    }

    @Inject(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingTimer:I", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.BEFORE))
    private static void silkard$furnaceStartSmelt(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        ((ContextAbstractFurnaceBlockEntity) entity).silkard$fireFurnaceStartSmelt(level, pos);
    }

    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void silkard$smelt(NonNullList<ItemStack> items, ItemStack input, ItemStack result) {
        AbstractFurnaceBlockEntity entity = silkard$currentFurnace;
        ServerLevel level = (ServerLevel) entity.getLevel();
        BlockPos pos = entity.getBlockPos();

        org.bukkit.inventory.ItemStack bukkitResult = CraftItemStack.asBukkitCopy(result);
        FurnaceSmeltEvent event = new FurnaceSmeltEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(input), bukkitResult);
        level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            silkard$smeltCancelled = true;
            return;
        }

        bukkitResult = event.getResult();
        result = CraftItemStack.asNMSCopy(bukkitResult);

        ItemStack output = items.get(2);
        if (!result.isEmpty()) {
            if (output.isEmpty()) {
                items.set(2, result.copy());
            } else if (CraftItemStack.asCraftMirror(output).isSimilar(bukkitResult)) {
                output.grow(result.getCount());
            } else {
                silkard$smeltCancelled = true;
                return;
            }
        }

        if (input.is(Items.WET_SPONGE) && !items.get(1).isEmpty() && items.get(1).is(Items.BUCKET)) {
            items.set(1, new ItemStack(Items.WATER_BUCKET));
        }
        input.shrink(1);
    }

    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;setRecipeUsed(Lnet/minecraft/world/item/crafting/RecipeHolder;)V"))
    private static void silkard$setRecipeUsed(AbstractFurnaceBlockEntity entity, RecipeHolder<?> holder) {
        if (silkard$smeltCancelled) {
            return;
        }
        entity.setRecipeUsed(holder);
    }

    @Override
    public int silkard$getBurnDuration(ServerLevel world, ItemStack itemStack) {
        return this.getBurnDuration(world, itemStack);
    }

    @Override
    public void silkard$fireFurnaceStartSmelt(ServerLevel world, BlockPos pos) {
        if (this.cookingTimer != 0) {
            return;
        }
        ItemStack input = this.items.get(0);
        if (input.isEmpty()) {
            return;
        }
        RecipeHolder<?> holder = this.quickCheck.getRecipeFor(new SingleRecipeInput(input), world).orElse(null);
        if (holder == null) {
            return;
        }
        FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(CraftBlock.at(world, pos), CraftItemStack.asCraftMirror(input), (CookingRecipe<?>) holder.toBukkitRecipe());
        world.getCraftServer().getPluginManager().callEvent(event);
        this.cookingTotalTime = event.getTotalCookTime();
    }

    @Override
    public Object2IntOpenHashMap<Identifier> getRecipesUsed() {
        Object2IntOpenHashMap<Identifier> converted = new Object2IntOpenHashMap<>();
        for (var entry : this.recipesUsed.reference2IntEntrySet()) {
            converted.put(entry.getKey().identifier(), entry.getIntValue());
        }
        return converted;
    }

    @Override
    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel world, Vec3 position, BlockPos pos, @Nullable Player player, @Nullable ItemStack itemStack, int amount) {
        List<RecipeHolder<?>> list = Lists.newArrayList();
        for (var entry : this.recipesUsed.reference2IntEntrySet()) {
            ResourceKey<Recipe<?>> key = entry.getKey();
            world.recipeAccess().byKey(key).ifPresent(holder -> {
                list.add(holder);
                this.silkard$createExperience(world, position, entry.getIntValue(), ((AbstractCookingRecipe) holder.value()).experience(), pos, player, itemStack, amount);
            });
        }
        return list;
    }

    @Override
    public List<RecipeHolder<?>> silkard$dropExp(ServerPlayer player, ItemStack itemStack, int amount) {
        AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(player.level(), player.position(), self.getBlockPos(), player, itemStack, amount);
        player.awardRecipes(list);
        for (RecipeHolder<?> holder : list) {
            player.triggerRecipeCrafted(holder, this.items);
        }
        this.recipesUsed.clear();
        return list;
    }

    @Overwrite
    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel world, Vec3 position) {
        AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
        return this.getRecipesToAwardAndPopExperience(world, position, self.getBlockPos(), null, null, 0);
    }

    private void silkard$createExperience(ServerLevel level, Vec3 position, int amount, float value, BlockPos pos, @Nullable Player player, @Nullable ItemStack itemStack, int itemAmount) {
        int j = Mth.floor((float) amount * value);
        float f1 = Mth.frac((float) amount * value);
        if (f1 != 0.0F && level.getRandom().nextFloat() < f1) {
            ++j;
        }

        BlockExpEvent event;
        if (itemAmount != 0) {
            event = new FurnaceExtractEvent((org.bukkit.entity.Player) player.getBukkitEntity(), CraftBlock.at(level, pos), CraftItemType.minecraftToBukkit(itemStack.getItem()), itemAmount, j);
        } else {
            event = new BlockExpEvent(CraftBlock.at(level, pos), j);
        }
        level.getCraftServer().getPluginManager().callEvent(event);
        j = event.getExpToDrop();

        ExperienceOrb.award(level, position, j);
    }
    // CraftBukkit end
}
