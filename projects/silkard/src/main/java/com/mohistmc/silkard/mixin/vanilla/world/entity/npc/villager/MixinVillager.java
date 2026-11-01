package com.mohistmc.silkard.mixin.vanilla.world.entity.npc.villager;

import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import com.mohistmc.silkard.injected.world.entity.ContextMob;
import com.mohistmc.silkard.injected.world.entity.ai.gossip.ContextGossipContainer;
import com.mohistmc.silkard.injected.world.entity.npc.villager.ContextVillager;
import com.mohistmc.silkard.injected.util.ContextSpawnUtil;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.spongepowered.asm.mixin.Mixin;
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
 * @date 2026/6/8
 */
@Mixin(Villager.class)
public class MixinVillager implements ContextVillager {

    @Shadow private GossipContainer gossips;

    // CB 自增字段，26.3 vanilla 无此成员；初值取 vanilla 原硬编码 24000L
    @Unique
    public long gossipDecayInterval = 24000L;

    @Override
    public long silkard$gossipDecayInterval() {
        return gossipDecayInterval;
    }

    @Override
    public void silkard$gossipDecayInterval(long gossipDecayInterval) {
        this.gossipDecayInterval = gossipDecayInterval;
    }

    // CraftBukkit start - add constructor parameter in Reputation
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void silkard$initGossips(EntityType<? extends Villager> entityType, Level level, CallbackInfo ci) {
        ((ContextGossipContainer) this.gossips).villager((Villager) (Object) this);
    }
    // CraftBukkit end

    @Redirect(method = "rewardTradeXp", at = @At(value = "INVOKE", target = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean silkard$addEffect(Villager self, MobEffectInstance effect) {
        return ((ContextLivingEntity) self).addEffect(effect, EntityPotionEffectEvent.Cause.VILLAGER_TRADE);
    }

    @Redirect(method = "restock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void silkard$restockOffer(MerchantOffer offer) {
        silkard$replenish(offer);
    }

    @Redirect(method = "catchUpDemand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void silkard$catchUpOffer(MerchantOffer offer) {
        silkard$replenish(offer);
    }

    @Unique
    private void silkard$replenish(MerchantOffer offer) {
        VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((org.bukkit.entity.Villager) ((Entity) (Object) this).getBukkitEntity(), offer.asBukkit());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            offer.resetUses();
        }
    }

    @ModifyConstant(method = "maybeDecayGossip", constant = @Constant(longValue = 24000L))
    private long silkard$decayInterval(long original) {
        return this.gossipDecayInterval;
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob silkard$convertToWitch(Villager self, EntityType<?> entityType, ConversionParams params, ConversionParams.AfterConversion<?> afterConversion) {
        return ((ContextMob) self).convertTo((EntityType) entityType, params, (ConversionParams.AfterConversion) afterConversion, EntityTransformEvent.TransformReason.LIGHTNING, CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Redirect(method = "spawnGolemIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;"))
    private static Optional<Mob> silkard$spawnGolem(EntityType<Mob> entityType, EntitySpawnReason spawnReason, ServerLevel level, BlockPos pos, int spawnAttempts, int spawnRangeXZ, int spawnRangeY, SpawnUtil.Strategy strategy, boolean checkCollisions) {
        ContextSpawnUtil.SPAWN_REASON.set(CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE);
        try {
            return SpawnUtil.trySpawnMob(entityType, spawnReason, level, pos, spawnAttempts, spawnRangeXZ, spawnRangeY, strategy, checkCollisions);
        } finally {
            ContextSpawnUtil.SPAWN_REASON.remove();
        }
    }

    @org.spongepowered.asm.mixin.Overwrite
    public void onReputationEventFrom(ReputationEventType type, Entity source) {
        org.bukkit.entity.Villager.ReputationEvent bukkitReputationEvent = CraftVillager.CraftReputationEvent.minecraftToBukkit(type);
        ContextGossipContainer container = (ContextGossipContainer) this.gossips;
        if (type == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
            container.add(source.getUUID(), GossipType.MAJOR_POSITIVE, 20, bukkitReputationEvent);
            container.add(source.getUUID(), GossipType.MINOR_POSITIVE, 25, bukkitReputationEvent);
        } else if (type == ReputationEventType.TRADE) {
            container.add(source.getUUID(), GossipType.TRADING, 2, bukkitReputationEvent);
        } else if (type == ReputationEventType.VILLAGER_HURT) {
            container.add(source.getUUID(), GossipType.MINOR_NEGATIVE, 25, bukkitReputationEvent);
        } else if (type == ReputationEventType.VILLAGER_KILLED) {
            container.add(source.getUUID(), GossipType.MAJOR_NEGATIVE, 25, bukkitReputationEvent);
        }
    }
}
