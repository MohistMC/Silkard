package com.mohistmc.silkard.mixin.vanilla.world.entity.ai.gossip;

import com.google.common.collect.Sets;
import com.mohistmc.silkard.injected.world.entity.ai.gossip.ContextGossipContainer;
import com.mohistmc.silkard.injected.world.entity.ai.gossip.ContextGossipContainerEntityGossips;
import it.unimi.dsi.fastutil.ints.IntBinaryOperator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.bukkit.event.entity.VillagerReputationChangeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/5/13 21:49
 */
@Mixin(GossipContainer.class)
public class MixinGossipContainer implements ContextGossipContainer {

    @Shadow @Final public Map<UUID, GossipContainer.EntityGossips> gossips;

    @Shadow private GossipContainer.EntityGossips getOrCreate(UUID uuid) {
        throw new AssertionError();
    }

    @Shadow private int mergeValuesForAddition(GossipType type, int value, int add) {
        throw new AssertionError();
    }

    @Unique private UUID silkard$transferTarget;

    // CraftBukkit start - store reference to villager entity
    public Villager villager;

    public GossipContainer villager(Villager villager) {
        this.villager = villager;
        return (GossipContainer) (Object) this;
    }
    // CraftBukkit end

    @Overwrite
    public int getReputation(UUID entity, Predicate<GossipType> types) {
        return this.getReputation(entity, types, true);
    }

    public int getReputation(UUID entity, Predicate<GossipType> types, boolean weighted) {
        GossipContainer.EntityGossips entry = this.gossips.get(entity);
        // CraftBukkit start - handle weighted parameter
        return entry != null ? (weighted ? entry.weightedValue(types) : ((ContextGossipContainerEntityGossips) entry).unweightedValue(types)) : 0;
        // CraftBukkit end
    }

    @Overwrite
    public void add(UUID target, GossipType type, int amountToAdd) {
        this.add(target, type, amountToAdd, org.bukkit.entity.Villager.ReputationEvent.UNSPECIFIED);
    }

    public void add(UUID target, GossipType type, int amountToAdd, org.bukkit.entity.Villager.ReputationEvent changeReason) {
        GossipContainer.EntityGossips entityGossips = this.getOrCreate(target);
        ContextGossipContainerEntityGossips handle = (ContextGossipContainerEntityGossips) entityGossips;
        // CraftBukkit start - fire reputation change event
        int oldValue = handle.getRawValue(type);
        int mergedValue = this.mergeValuesForAddition(type, oldValue, amountToAdd);
        handle.setRawValue(type, mergedValue);
        int newValue = Math.max(0, Math.min(mergedValue, type.max));
        handle.setRawValue(type, oldValue);
        VillagerReputationChangeEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callVillagerReputationChangeEvent(villager, target, changeReason, org.bukkit.craftbukkit.entity.CraftVillager.CraftReputationType.minecraftToBukkit(type), oldValue, newValue, type.max);
        if (!event.isCancelled()) {
            handle.setRawValue(type, event.getNewValue());
            entityGossips.makeSureValueIsntTooLowOrTooHigh(type);
        }
        // CraftBukkit end
        if (entityGossips.isEmpty()) {
            this.gossips.remove(target);
        }
    }

    // CraftBukkit start
    public void set(UUID target, GossipType type, int amount, org.bukkit.entity.Villager.ReputationEvent changeReason) {
        int addAmount = amount - getReputation(target, Predicate.isEqual(type), false);
        if (addAmount == 0) {
            return;
        }
        this.add(target, type, addAmount, changeReason);
    }
    // CraftBukkit end

    // CraftBukkit start - add change reason parameter
    public void remove(UUID target, GossipType type, int amountToRemove, org.bukkit.entity.Villager.ReputationEvent changeReason) {
        this.add(target, type, -amountToRemove, changeReason);
    }
    // CraftBukkit end

    @Overwrite
    public void remove(UUID target, GossipType type) {
        GossipContainer.EntityGossips entityGossips = this.gossips.get(target);
        if (entityGossips != null) {
            this.set(target, type, 0, org.bukkit.entity.Villager.ReputationEvent.UNSPECIFIED);
            if (entityGossips.isEmpty()) {
                this.gossips.remove(target);
            }
        }
    }

    @Overwrite
    public void remove(GossipType type) {
        Set<UUID> uuids = Sets.newHashSet(this.gossips.keySet());
        for (UUID uuid : uuids) {
            this.remove(uuid, type);
        }
    }

    @Overwrite
    public void decay() {
        Iterator<Map.Entry<UUID, GossipContainer.EntityGossips>> iterator = this.gossips.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GossipContainer.EntityGossips> entry = iterator.next();
            ((ContextGossipContainerEntityGossips) entry.getValue()).decay(villager, entry.getKey());
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Overwrite
    public GossipContainer copy() {
        GossipContainer gossipcontainer = new GossipContainer();
        ((ContextGossipContainer) gossipcontainer).villager(this.villager);
        gossipcontainer.putAll((GossipContainer) (Object) this);
        return gossipcontainer;
    }

    @Redirect(method = "lambda$transferFrom$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/gossip/GossipContainer;getOrCreate(Ljava/util/UUID;)Lnet/minecraft/world/entity/ai/gossip/GossipContainer$EntityGossips;"))
    private GossipContainer.EntityGossips silkard$captureTransferTarget(GossipContainer self, UUID target) {
        this.silkard$transferTarget = target;
        return this.getOrCreate(target);
    }

    @Redirect(method = "lambda$transferFrom$0", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;mergeInt(Ljava/lang/Object;ILit/unimi/dsi/fastutil/ints/IntBinaryOperator;)I"))
    private int silkard$mergeTransfer(Object2IntMap<GossipType> map, Object key, int value, IntBinaryOperator remappingFunction) {
        GossipType type = (GossipType) key;
        UUID target = this.silkard$transferTarget;
        this.silkard$transferTarget = null;
        int merged = remappingFunction.applyAsInt(this.getReputation(target, Predicate.isEqual(type), false), value);
        this.set(target, type, merged, org.bukkit.entity.Villager.ReputationEvent.GOSSIP);
        return 0;
    }
}
