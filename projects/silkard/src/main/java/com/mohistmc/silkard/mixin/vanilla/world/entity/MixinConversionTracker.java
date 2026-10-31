package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import com.mohistmc.silkard.injected.world.entity.ContextMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(ConversionTracker.class)
public abstract class MixinConversionTracker {

    @Shadow @Final private Mob mob;

    @Shadow private int afflictionTime;

    // CraftBukkit start
    @Redirect(method = "doConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> T silkard$convertTo(Mob mob, EntityType<T> entityType, ConversionParams params, ConversionParams.AfterConversion<T> afterConversion) {
        if (mob instanceof Skeleton && entityType == EntityTypes.STRAY) {
            T converted = ((ContextMob) mob).convertTo(entityType, params, afterConversion, EntityTransformEvent.TransformReason.FROZEN, CreatureSpawnEvent.SpawnReason.FROZEN);
            if (converted == null) {
                // SPIGOT-7997: End conversion to stop event spam
                ((org.bukkit.entity.Skeleton) mob.getBukkitEntity()).setConversionTime(-1);
            }
            return converted;
        }
        if (mob instanceof Zombie && entityType == EntityTypes.DROWNED) {
            T converted = ((ContextMob) mob).convertTo(entityType, params, afterConversion, EntityTransformEvent.TransformReason.DROWNED, CreatureSpawnEvent.SpawnReason.DROWNED);
            if (converted == null) {
                // SPIGOT-5208: End conversion to stop event spam
                ((org.bukkit.entity.Zombie) mob.getBukkitEntity()).setConversionTime(-1);
            }
            return converted;
        }
        return mob.convertTo(entityType, params, afterConversion);
    }
    // CraftBukkit end

    // CraftBukkit start - use wall time instead of ticks for conversion
    @Unique
    private int silkard$lastTick;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ConversionTracker;conversionTime:I", opcode = Opcodes.PUTFIELD))
    private void silkard$conversionTime(ConversionTracker<?> self, int value) {
        self.conversionTime = value - this.silkard$elapsedTicks() + 1;
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ConversionTracker;afflictionTime:I", opcode = Opcodes.PUTFIELD))
    private void silkard$afflictionTime(ConversionTracker<?> self, int value) {
        this.afflictionTime = value + this.silkard$elapsedTicks() - 1;
    }

    @Inject(method = "startConversion", at = @At("HEAD"))
    private void silkard$startConversion(int time, CallbackInfo ci) {
        this.silkard$lastTick = this.silkard$currentTick();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void silkard$tickEnd(CallbackInfo ci) {
        this.silkard$lastTick = this.silkard$currentTick();
    }

    private int silkard$elapsedTicks() {
        int current = this.silkard$currentTick();
        if (this.silkard$lastTick == 0) {
            this.silkard$lastTick = current - 1;
        }
        int elapsed = current - this.silkard$lastTick;
        return elapsed < 1 ? 1 : elapsed;
    }

    private int silkard$currentTick() {
        Level level = this.mob.level();
        if (level == null) {
            return 0;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return 0;
        }
        return ((ContextMinecraftServer) (Object) server).silkard$currentTick();
    }
    // CraftBukkit end
}
