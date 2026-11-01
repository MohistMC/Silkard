package com.mohistmc.silkard.mixin.vanilla.core.dispenser;

import com.mohistmc.silkard.bukkit.DispenseEventHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CraftBukkit - anonymous dispenser behaviors (armor stand, buckets, bone meal, TNT, skull, pumpkin, beehive, armadillo).
 *
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(targets = {
        "net.minecraft.core.dispenser.DispenseItemBehavior$1",
        "net.minecraft.core.dispenser.DispenseItemBehavior$3",
        "net.minecraft.core.dispenser.DispenseItemBehavior$4",
        "net.minecraft.core.dispenser.DispenseItemBehavior$5",
        "net.minecraft.core.dispenser.DispenseItemBehavior$6",
        "net.minecraft.core.dispenser.DispenseItemBehavior$7",
        "net.minecraft.core.dispenser.DispenseItemBehavior$8",
        "net.minecraft.core.dispenser.DispenseItemBehavior$9",
        "net.minecraft.core.dispenser.DispenseItemBehavior$11"
})
public abstract class MixinDispenseItemBehavior$Anonymous {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void silkard$blockDispenseEvent(BlockSource source, ItemStack dispensed, CallbackInfoReturnable<ItemStack> cir) {
        // CraftBukkit start
        BlockDispenseEvent event = DispenseEventHooks.callBlockDispenseEvent(source.level(), source, dispensed, new org.bukkit.util.Vector(0, 0, 0));
        if (event.isCancelled()) {
            cir.setReturnValue(dispensed);
            return;
        }

        if (DispenseEventHooks.chainToChangedItem(source, dispensed, event, this)) {
            cir.setReturnValue(dispensed);
        }
        // CraftBukkit end
    }
}
