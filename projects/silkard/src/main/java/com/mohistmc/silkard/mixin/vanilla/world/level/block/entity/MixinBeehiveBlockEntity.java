package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mohistmc.silkard.injected.world.level.ContextLevelWriter;
import com.mohistmc.silkard.injected.world.level.block.entity.ContextBeehiveBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEnterBlockEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(BeehiveBlockEntity.class)
public class MixinBeehiveBlockEntity implements ContextBeehiveBlockEntity {

    @Shadow @Mutable private List<?> stored;

    @Shadow private List<Entity> releaseAllOccupants(BlockState state, BeehiveBlockEntity.BeeReleaseStatus status) {
        return null;
    }

    @Unique
    public int maxBees = 3;

    @Unique
    private static final ThreadLocal<Boolean> silkard$FORCE_RELEASE = ThreadLocal.withInitial(() -> false);

    public int silkard$maxBees() {
        return maxBees;
    }

    public void silkard$maxBees(int maxBees) {
        this.maxBees = maxBees;
    }

    // CraftBukkit - CB releaseBees API; CraftBeehive.releaseEntities depends on it (force bypasses BEES_STAY_IN_HIVE)
    @Override
    public List<Entity> releaseBees(BlockState state, BeehiveBlockEntity.BeeReleaseStatus status, boolean force) {
        silkard$FORCE_RELEASE.set(force);
        try {
            return this.releaseAllOccupants(state, status);
        } finally {
            silkard$FORCE_RELEASE.set(false);
        }
    }

    // CraftBukkit - isFull uses configurable maxBees
    @ModifyConstant(method = "isFull", constant = @Constant(intValue = 3))
    private int silkard$isFullMax(int constant) {
        return this.maxBees;
    }

    // CraftBukkit - addOccupant uses configurable maxBees
    @ModifyConstant(method = "addOccupant", constant = @Constant(intValue = 3))
    private int silkard$addOccupantMax(int constant) {
        return this.maxBees;
    }

    // CraftBukkit - EntityEnterBlockEvent on bee enter attempt
    @Inject(method = "addOccupant", at = @At("HEAD"), cancellable = true)
    private void silkard$enterBlock(Bee bee, CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && this.stored.size() < this.maxBees) {
            EntityEnterBlockEvent event = new EntityEnterBlockEvent(bee.getBukkitEntity(), CraftBlock.at(level, ((BlockEntity) (Object) this).getBlockPos()));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                bee.setStayOutOfHiveCountdown(400);
                ci.cancel();
            }
        }
    }

    // CraftBukkit - discard with Bukkit remove cause
    @Redirect(method = "addOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;discard()V"))
    private void silkard$discardBee(Bee bee) {
        bee.discard(EntityRemoveEvent.Cause.ENTER_BLOCK);
    }

    // CraftBukkit - EntityTargetEvent on emptyAllLivingFromHive
    @Redirect(method = "emptyAllLivingFromHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void silkard$setTarget(Bee bee, LivingEntity target) {
        bee.setTarget(target, EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
    }

    // CraftBukkit - force bypasses BEES_STAY_IN_HIVE check in releaseOccupant
    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"))
    private static Object silkard$bypassStayInHive(EnvironmentAttributeSystem system, EnvironmentAttribute attribute, BlockPos pos) {
        if (silkard$FORCE_RELEASE.get()) {
            return Boolean.FALSE;
        }
        return system.getValue(attribute, pos);
    }

    // CraftBukkit - SpawnReason.BEEHIVE on bee release
    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean silkard$spawnBee(Level level, Entity entity) {
        return ((ContextLevelWriter) level).addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.BEEHIVE);
    }

    // CraftBukkit - SPIGOT-7790: reassign stored list on load
    @Redirect(method = "loadAdditional", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"))
    private void silkard$loadNewStored(List<?> stored) {
        this.stored = Lists.newArrayList();
    }

    // CraftBukkit - persist maxBees
    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void silkard$loadMaxBees(ValueInput input, CallbackInfo ci) {
        this.maxBees = input.getIntOr("Bukkit.MaxEntities", this.maxBees);
    }

    // CraftBukkit - persist maxBees
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void silkard$saveMaxBees(ValueOutput output, CallbackInfo ci) {
        output.putInt("Bukkit.MaxEntities", this.maxBees);
    }

    // CraftBukkit - SPIGOT-7790: reassign stored list on applyImplicitComponents
    @Redirect(method = "applyImplicitComponents", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"))
    private void silkard$applyNewStored(List<?> stored) {
        this.stored = Lists.newArrayList();
    }
}
