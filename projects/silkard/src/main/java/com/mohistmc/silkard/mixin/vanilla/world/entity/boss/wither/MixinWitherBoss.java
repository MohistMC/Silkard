package com.mohistmc.silkard.mixin.vanilla.world.entity.boss.wither;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(WitherBoss.class)
public abstract class MixinWitherBoss extends Monster {

    protected MixinWitherBoss(net.minecraft.world.entity.EntityType<? extends WitherBoss> type, Level level) {
        super(type, level);
    }

    // CraftBukkit start - ExplosionPrimeEvent
    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void silkard$explode(ServerLevel level, Entity entity, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction) {
        ExplosionPrimeEvent event = CraftEventFactory.callExplosionPrimeEvent((WitherBoss) (Object) this, radius, fire);
        if (!event.isCancelled()) {
            level.explode(entity, x, y, z, event.getRadius(), event.getFire(), interaction);
        }
    }
    // CraftBukkit end

    // CraftBukkit start - EntityChangeBlockEvent
    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean silkard$destroyBlock(ServerLevel level, BlockPos pos, boolean drop, Entity entity) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((WitherBoss) (Object) this, pos, Blocks.AIR.defaultBlockState())) {
            return false;
        }
        return level.destroyBlock(pos, drop, entity);
    }
    // CraftBukkit end

    // CraftBukkit start - use relative location for far away sounds
    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;globalLevelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void silkard$globalLevelEvent(ServerLevel level, int eventId, BlockPos pos, int data) {
        int viewDistance = level.getCraftServer().getViewDistance() * 16;
        for (ServerPlayer player : level.getCraftServer().getServer().getPlayerList().players) {
            double deltaX = this.getX() - player.getX();
            double deltaZ = this.getZ() - player.getZ();
            double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (distanceSquared > viewDistance * viewDistance) {
                double deltaLength = Math.sqrt(distanceSquared);
                double relativeX = player.getX() + (deltaX / deltaLength) * viewDistance;
                double relativeZ = player.getZ() + (deltaZ / deltaLength) * viewDistance;
                player.connection.send(new ClientboundLevelEventPacket(eventId, new BlockPos((int) relativeX, (int) this.getY(), (int) relativeZ), data, true));
            } else {
                player.connection.send(new ClientboundLevelEventPacket(eventId, this.blockPosition(), data, true));
            }
        }
    }
    // CraftBukkit end

    // CraftBukkit start - EntityRegainHealthEvent
    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;heal(F)V", ordinal = 0))
    private void silkard$healSpawn(WitherBoss entity, float amount) {
        ((ContextLivingEntity) (Object) this).heal(amount, EntityRegainHealthEvent.RegainReason.WITHER_SPAWN);
    }

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;heal(F)V", ordinal = 1))
    private void silkard$healRegen(WitherBoss entity, float amount) {
        ((ContextLivingEntity) (Object) this).heal(amount, EntityRegainHealthEvent.RegainReason.REGEN);
    }
    // CraftBukkit end

    // CraftBukkit start - EntityTargetLivingEntityEvent
    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getId()I", ordinal = 0))
    private int silkard$targetId(LivingEntity target) {
        if (CraftEventFactory.callEntityTargetLivingEvent((WitherBoss) (Object) this, target, EntityTargetEvent.TargetReason.CLOSEST_ENTITY).isCancelled()) {
            return 0;
        }
        return target.getId();
    }
    // CraftBukkit end

    // CraftBukkit start - add Bukkit remove cause
    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;discard()V"))
    private void silkard$discard(WitherBoss entity) {
        ((ContextEntity) (Object) this).discard(EntityRemoveEvent.Cause.DESPAWN);
    }
    // CraftBukkit end
}
