package com.mohistmc.silkard.mixin.vanilla.world.entity.vehicle.minecart;

import com.mohistmc.silkard.injected.world.entity.vehicle.minecart.ContextMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(MinecartBehavior.class)
public abstract class MixinMinecartBehavior implements ContextMinecartBehavior {

    @Shadow @Final protected AbstractMinecart minecart;

    @Override
    public AbstractMinecart silkard$minecart() {
        return this.minecart;
    }
}
