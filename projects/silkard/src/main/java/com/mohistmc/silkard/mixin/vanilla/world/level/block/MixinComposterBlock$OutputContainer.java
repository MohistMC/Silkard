package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/10/31
 */
@Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
public abstract class MixinComposterBlock$OutputContainer {

    @Shadow
    @Final
    private BlockState state;
    @Shadow
    @Final
    private LevelAccessor level;
    @Shadow
    @Final
    private BlockPos pos;
    @Shadow
    private boolean changed;

    // CraftBukkit - allow putting items back (eg cancelled InventoryMoveItemEvent)
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void silkard$setChanged(CallbackInfo ci) {
        Container container = (Container) (Object) this;
        if (container.isEmpty()) {
            BlockState blockstate1 = this.state.setValue(ComposterBlock.LEVEL, 0);
            this.level.setBlockAndUpdate(this.pos, blockstate1);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.pos, GameEvent.Context.of((Entity) null, blockstate1));
            this.changed = true;
        } else {
            this.level.setBlock(this.pos, this.state, 3);
            this.changed = false;
        }
        ci.cancel();
    }
}
