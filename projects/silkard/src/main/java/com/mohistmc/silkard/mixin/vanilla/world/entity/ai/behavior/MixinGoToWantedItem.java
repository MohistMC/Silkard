package com.mohistmc.silkard.mixin.vanilla.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder.Instance;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * @author Mgazul
 * @date 2026/10/24
 */
@Mixin(GoToWantedItem.class)
public class MixinGoToWantedItem {

    /**
     * @author Mgazul
     * @reason CraftBukkit - EntityTargetEvent for Allay
     */
    @Overwrite
    private static boolean lambda$create$3(Instance instance, MemoryAccessor wantedItem, MemoryAccessor cooldown, Predicate predicate, int maxDistToWalk, float speedModifier, MemoryAccessor lookTarget, MemoryAccessor walkTarget, ServerLevel level, LivingEntity body, long timestamp) {
        ItemEntity item = (ItemEntity) instance.get(wantedItem);
        if (instance.tryGet(cooldown).isEmpty()
                && predicate.test(body)
                && item.closerThan(body, maxDistToWalk)
                && body.level().getWorldBorder().isWithinBounds(item.blockPosition())
                && body.canPickUpLoot()) {
            // CraftBukkit start
            if (body instanceof net.minecraft.world.entity.animal.allay.Allay) {
                EntityTargetEvent event = CraftEventFactory.callEntityTargetEvent(body, item, EntityTargetEvent.TargetReason.CLOSEST_ENTITY);
                if (event.isCancelled()) {
                    return false;
                }
                if (!(event.getTarget() instanceof ItemEntity)) {
                    wantedItem.erase();
                    lookTarget.erase();
                    return true;
                }
                item = (ItemEntity) ((CraftItem) event.getTarget()).getHandle();
            }
            // CraftBukkit end
            net.minecraft.world.entity.ai.memory.WalkTarget target = new net.minecraft.world.entity.ai.memory.WalkTarget(new net.minecraft.world.entity.ai.behavior.EntityTracker(item, false), speedModifier, 0);
            lookTarget.set(new net.minecraft.world.entity.ai.behavior.EntityTracker(item, true));
            walkTarget.set(target);
            return true;
        } else {
            return false;
        }
    }
}
