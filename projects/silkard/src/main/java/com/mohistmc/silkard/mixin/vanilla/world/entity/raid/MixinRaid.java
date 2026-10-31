package com.mohistmc.silkard.mixin.vanilla.world.entity.raid;

import com.mohistmc.silkard.injected.world.entity.raid.ContextRaid;
import com.mohistmc.silkard.injected.world.level.ContextServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(Raid.class)
public abstract class MixinRaid implements ContextRaid {

    @Shadow private Set<UUID> heroesOfTheVillage;
    @Shadow private java.util.Map<Integer, Set<Raider>> groupRaiderMap;
    @Shadow public abstract boolean isOver();
    @Shadow public abstract boolean isStopped();
    @Shadow public abstract boolean isVictory();
    @Shadow public abstract boolean isLoss();

    private transient Raider silkard$waveLeader;
    private transient List<Raider> silkard$waveRaiders;
    private transient boolean silkard$finishFired;

    @Override
    public boolean isInProgress() {
        return !isOver() && !isStopped();
    }

    @Override
    public Collection<Raider> getRaiders() {
        return groupRaiderMap.values().stream().flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
    }

    // 26.3 derives wave count from difficulty (numGroups is final); this is a no-op
    @Override
    public void setTotalWaves(int waves) {
    }

    @Inject(method = "spawnGroup", at = @At("HEAD"))
    private void silkard$resetWave(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        this.silkard$waveLeader = null;
        this.silkard$waveRaiders = new ArrayList<>();
    }

    @Redirect(method = "spawnGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setLeader(ILnet/minecraft/world/entity/raid/Raider;)V"))
    private void silkard$captureLeader(Raid self, int group, Raider raider) {
        this.silkard$waveLeader = raider;
        self.setLeader(group, raider);
    }

    @Redirect(method = "spawnGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;joinRaid(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/entity/raid/Raider;Lnet/minecraft/core/BlockPos;Z)V"))
    private void silkard$collectWaveRaider(Raid self, ServerLevel level, int group, Raider raider, BlockPos pos, boolean exists) {
        this.silkard$waveRaiders.add(raider);
        self.joinRaid(level, group, raider, pos, exists);
    }

    @Inject(method = "spawnGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setDirty(Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.BEFORE))
    private void silkard$fireSpawnWave(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (this.silkard$waveLeader != null) {
            CraftEventFactory.callRaidSpawnWaveEvent((Raid) (Object) this, level, this.silkard$waveLeader, this.silkard$waveRaiders);
        }
    }

    @Redirect(method = "joinRaid", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void silkard$spawnRaiderWithReason(ServerLevel level, Entity entity) {
        ((ContextServerLevelAccessor) (Object) level).addFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.RAID);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 0))
    private void silkard$stopPeace(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, org.bukkit.event.raid.RaidStopEvent.Reason.PEACE);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 1))
    private void silkard$stopNotInVillage(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, org.bukkit.event.raid.RaidStopEvent.Reason.NOT_IN_VILLAGE);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 2))
    private void silkard$stopTimeout(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, org.bukkit.event.raid.RaidStopEvent.Reason.TIMEOUT);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 3))
    private void silkard$stopUnspawnable(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, org.bukkit.event.raid.RaidStopEvent.Reason.UNSPAWNABLE);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 4))
    private void silkard$stopFinished(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, org.bukkit.event.raid.RaidStopEvent.Reason.FINISHED);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setDirty(Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.BEFORE))
    private void silkard$fireFinish(ServerLevel level, CallbackInfo ci) {
        if (!this.silkard$finishFired && (isVictory() || isLoss())) {
            this.silkard$finishFired = true;
            List<Player> winners = new ArrayList<>();
            if (isVictory()) {
                for (UUID uuid : heroesOfTheVillage) {
                    Entity entity = level.getEntity(uuid);
                    if (entity instanceof ServerPlayer) {
                        winners.add(((ServerPlayer) entity).getBukkitEntity());
                    }
                }
            }
            CraftEventFactory.callRaidFinishEvent((Raid) (Object) this, level, winners);
        }
    }
}
