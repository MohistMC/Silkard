package com.mohistmc.silkard.mixin.vanilla.world.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.level.ContextLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(Leashable.class)
public interface MixinLeashable {

    // CraftBukkit - SPIGOT-7487: don't save leash when the holder was removed by a plugin
    @Inject(method = "writeLeashData", at = @At("HEAD"), cancellable = true)
    default void silkard$skipRemovedHolder(ValueOutput output, Leashable.LeashData leashData, CallbackInfo ci) {
        if (leashData != null) {
            Entity holder = leashData.leashHolder;
            if (holder != null && ((ContextEntity) holder).silkard_pluginRemoved()) {
                ci.cancel();
            }
        }
    }

    // CraftBukkit - EntityUnleashEvent on distance
    @Inject(method = "leashTooFarBehaviour", at = @At("HEAD"))
    default void silkard$tooFar(CallbackInfo ci) {
        if (this instanceof Entity entity) {
            ((ContextLevel) entity.level()).getCraftServer().getPluginManager().callEvent(new EntityUnleashEvent(entity.getBukkitEntity(), EntityUnleashEvent.UnleashReason.DISTANCE));
        }
    }

    // CraftBukkit - EntityUnleashEvent when the holder is gone
    @Redirect(method = "tickLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private static Object silkard$unleash(GameRules rules, GameRule rule, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) Entity entity) {
        ((ContextLevel) level).getCraftServer().getPluginManager().callEvent(new EntityUnleashEvent(entity.getBukkitEntity(), entity.isAlive() ? EntityUnleashEvent.UnleashReason.HOLDER_GONE : EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH));
        return (Boolean) rules.get(rule) && !((ContextEntity) entity).silkard_pluginRemoved();
    }

    // CraftBukkit - force drops when the lead item is spawned on restore
    @Redirect(method = "restoreLeashFromSave(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Leashable$LeashData;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private static ItemEntity silkard$dropLeadOnRestore(Entity entity, ServerLevel level, ItemLike item) {
        ((ContextEntity) entity).silkard$forceDrops(true);
        ItemEntity result = entity.spawnAtLocation(level, item);
        ((ContextEntity) entity).silkard$forceDrops(false);
        return result;
    }

    // CraftBukkit - force drops when unleashing
    @Redirect(method = "dropLeash(Lnet/minecraft/world/entity/Entity;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private static ItemEntity silkard$dropLeadOnUnleash(Entity entity, ServerLevel level, ItemLike item) {
        ((ContextEntity) entity).silkard$forceDrops(true);
        ItemEntity result = entity.spawnAtLocation(level, item);
        ((ContextEntity) entity).silkard$forceDrops(false);
        return result;
    }
}
