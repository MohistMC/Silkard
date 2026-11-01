package com.mohistmc.silkard.mixin.vanilla.world.inventory;

import com.mohistmc.silkard.injected.world.level.block.entity.ContextAbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public class MixinFurnaceResultSlot {

    @Shadow
    private int removeCount;

    @Unique
    private ItemStack silkard$craftedStack;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void silkard$captureStack(ItemStack stack, CallbackInfo ci) {
        this.silkard$craftedStack = stack;
    }

    @Redirect(method = "checkTakeAchievements", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;awardUsedRecipesAndPopExperience(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void silkard$awardUsedRecipesAndPopExperience(AbstractFurnaceBlockEntity entity, ServerPlayer player) {
        ((ContextAbstractFurnaceBlockEntity) entity).silkard$dropExp(player, this.silkard$craftedStack, this.removeCount);
    }
}
