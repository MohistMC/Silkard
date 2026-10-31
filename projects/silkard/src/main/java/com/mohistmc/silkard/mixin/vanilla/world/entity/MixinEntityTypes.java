package com.mohistmc.silkard.mixin.vanilla.world.entity;

import java.lang.reflect.Field;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(EntityTypes.class)
public abstract class MixinEntityTypes {

    // SPIGOT-3729: track area effect clouds
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void silkard$areaEffectCloudInterval(CallbackInfo ci) {
        try {
            Field field = EntityType.class.getDeclaredField("updateInterval");
            field.setAccessible(true);
            field.setInt(EntityTypes.AREA_EFFECT_CLOUD, 10);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new RuntimeException(reflectiveOperationException);
        }
    }
}
