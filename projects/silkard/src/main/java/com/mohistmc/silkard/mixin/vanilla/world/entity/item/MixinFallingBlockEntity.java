package com.mohistmc.silkard.mixin.vanilla.world.entity.item;

import net.minecraft.world.entity.item.FallingBlockEntity;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public class MixinFallingBlockEntity {

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void silkard$discardDespawnAir(FallingBlockEntity self) {
        self.discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void silkard$discardDespawnPlaced(FallingBlockEntity self) {
        self.discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void silkard$discardDropPlaceFail(FallingBlockEntity self) {
        self.discard(EntityRemoveEvent.Cause.DROP);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void silkard$discardDropConditional(FallingBlockEntity self) {
        self.discard(EntityRemoveEvent.Cause.DROP);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void silkard$discardDespawnBroken(FallingBlockEntity self) {
        self.discard(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 5, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void silkard$discardDropTimeout(FallingBlockEntity self) {
        self.discard(EntityRemoveEvent.Cause.DROP);
    }
}
