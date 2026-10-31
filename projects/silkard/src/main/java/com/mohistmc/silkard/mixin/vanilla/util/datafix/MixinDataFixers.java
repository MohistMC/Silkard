package com.mohistmc.silkard.mixin.vanilla.util.datafix;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import com.mohistmc.silkard.bukkit.PlayerCustomNameDataFix;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.EntityCustomNameToComponentFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(DataFixers.class)
public abstract class MixinDataFixers {

    // CraftBukkit - register the player CustomName fix before the entity custom name fix
    @Redirect(method = "addFixers", at = @At(value = "NEW", target = "Lnet/minecraft/util/datafix/fixes/EntityCustomNameToComponentFix;"))
    private static EntityCustomNameToComponentFix silkard$playerCustomName(Schema schema, @Local(argsOnly = true) DataFixerBuilder builder) {
        builder.addFixer(new PlayerCustomNameDataFix(schema));
        return new EntityCustomNameToComponentFix(schema);
    }
}
