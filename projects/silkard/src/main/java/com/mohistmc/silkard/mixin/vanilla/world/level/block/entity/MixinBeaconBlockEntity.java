package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.mohistmc.silkard.bukkit.BukkitUtils;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import com.mohistmc.silkard.injected.world.level.block.entity.ContextBeaconBlockEntity;
import io.izzel.arclight.mixin.TransformAccess;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/4/2 17:44
 */
@Mixin(BeaconBlockEntity.class)
public class MixinBeaconBlockEntity implements ContextBeaconBlockEntity {

    @Shadow private int levels;
    @Shadow private @Nullable Holder<MobEffect> primaryPower;
    @Shadow private @Nullable Holder<MobEffect> secondaryPower;

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static List getHumansInRange(Level level, BlockPos worldPosition, int i) {
        return BukkitUtils.getHumansInRange(level, worldPosition, i);
    }

    // CraftBukkit start - Bukkit API beacon effects
    private static int silkard$getEffectDuration(int levels) {
        return (9 + levels * 2) * 20;
    }

    private static int silkard$getAmplification(int levels, @Nullable Holder<MobEffect> primaryPower, @Nullable Holder<MobEffect> secondaryPower) {
        if (levels >= 4 && java.util.Objects.equals(primaryPower, secondaryPower)) {
            return 1;
        }
        return 0;
    }

    private boolean silkard$hasSecondaryEffect(int levels, @Nullable Holder<MobEffect> primaryPower, @Nullable Holder<MobEffect> secondaryPower) {
        if (levels < 4 || secondaryPower == null) {
            return false;
        }
        return true;
    }

    @Override
    public PotionEffect getPrimaryEffect() {
        if (this.primaryPower == null) {
            return null;
        }
        return CraftPotionUtil.toBukkit(new MobEffectInstance(this.primaryPower, silkard$getEffectDuration(this.levels), silkard$getAmplification(this.levels, this.primaryPower, this.secondaryPower), true, true));
    }

    @Override
    public PotionEffect getSecondaryEffect() {
        if (!this.silkard$hasSecondaryEffect(this.levels, this.primaryPower, this.secondaryPower)) {
            return null;
        }
        return CraftPotionUtil.toBukkit(new MobEffectInstance(this.secondaryPower, silkard$getEffectDuration(this.levels), 0, true, true));
    }
    // CraftBukkit end

    // CraftBukkit start - EntityPotionEffectEvent
    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", ordinal = 0))
    private static boolean silkard$primaryEffect(Player player, MobEffectInstance effect) {
        return ((ContextLivingEntity) (Object) player).addEffect(effect, EntityPotionEffectEvent.Cause.BEACON);
    }

    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", ordinal = 1))
    private static boolean silkard$secondaryEffect(Player player, MobEffectInstance effect) {
        return ((ContextLivingEntity) (Object) player).addEffect(effect, EntityPotionEffectEvent.Cause.BEACON);
    }
    // CraftBukkit end

    // CraftBukkit start - persist manually set non-default beacon effects
    @Redirect(method = "loadEffect", at = @At(value = "INVOKE", target = "Ljava/util/Optional;filter(Ljava/util/function/Predicate;)Ljava/util/Optional;"))
    private static Optional<Holder<MobEffect>> silkard$loadEffect(Optional<Holder<MobEffect>> optional, Predicate<Holder<MobEffect>> predicate) {
        return optional;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void silkard$loadLevels(ValueInput input, CallbackInfo ci) {
        this.levels = input.getIntOr("Levels", this.levels);
    }
    // CraftBukkit end
}
