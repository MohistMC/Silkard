package com.mohistmc.silkard.mixin.vanilla.world.entity.ai.gossip;

import com.mohistmc.silkard.injected.world.entity.ai.gossip.ContextGossipContainerEntityGossips;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.bukkit.craftbukkit.entity.CraftVillager.CraftReputationType;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerReputationChangeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(GossipContainer.EntityGossips.class)
public class MixinGossipContainer$EntityGossips implements ContextGossipContainerEntityGossips {

    @Shadow @Final private Object2IntMap<GossipType> entries;

    @Override
    public int unweightedValue(Predicate<GossipType> predicate) {
        return this.entries.object2IntEntrySet().stream().filter(entry -> predicate.test(entry.getKey())).mapToInt(Object2IntMap.Entry::getIntValue).sum();
    }

    @Override
    public void decay(Villager villager, UUID uuid) {
        ObjectIterator<Object2IntMap.Entry<GossipType>> iterator = this.entries.object2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            Object2IntMap.Entry<GossipType> entry = iterator.next();
            int i = entry.getIntValue() - entry.getKey().decayPerDay;
            // CraftBukkit start - fire event
            VillagerReputationChangeEvent event = CraftEventFactory.callVillagerReputationChangeEvent(villager, uuid, org.bukkit.entity.Villager.ReputationEvent.DECAY, CraftReputationType.minecraftToBukkit(entry.getKey()), entry.getIntValue(), i, entry.getKey().max);
            if (event.isCancelled()) {
                continue;
            }
            i = event.getNewValue();
            // CraftBukkit end
            if (i < 2) {
                iterator.remove();
            } else {
                entry.setValue(i);
            }
        }
    }

    @Override
    public int getRawValue(GossipType type) {
        return this.entries.getInt(type);
    }

    @Override
    public void setRawValue(GossipType type, int value) {
        this.entries.replace(type, value);
    }
}
