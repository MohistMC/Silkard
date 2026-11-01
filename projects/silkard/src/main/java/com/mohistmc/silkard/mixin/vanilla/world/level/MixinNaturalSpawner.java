package com.mohistmc.silkard.mixin.vanilla.world.level;

import com.mohistmc.silkard.injected.world.level.ContextServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(NaturalSpawner.class)
public class MixinNaturalSpawner {

    // CraftBukkit - natural spawn reason, SPIGOT-7045 ocelot baby reason
    @Redirect(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void silkard$addNatural(ServerLevel level, Entity entity) {
        CreatureSpawnEvent.SpawnReason reason = (entity instanceof net.minecraft.world.entity.animal.feline.Ocelot && !((org.bukkit.entity.Ageable) entity.getBukkitEntity()).isAdult())
                ? CreatureSpawnEvent.SpawnReason.OCELOT_BABY : CreatureSpawnEvent.SpawnReason.NATURAL;
        ((ContextServerLevelAccessor) level).addFreshEntityWithPassengers(entity, reason);
    }

    // CraftBukkit - chunk generation spawn reason
    @Redirect(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void silkard$addChunkGen(ServerLevelAccessor level, Entity entity) {
        ((ContextServerLevelAccessor) level).addFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
