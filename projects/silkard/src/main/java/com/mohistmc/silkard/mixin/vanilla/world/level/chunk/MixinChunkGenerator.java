package com.mohistmc.silkard.mixin.vanilla.world.level.chunk;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mohistmc.silkard.injected.world.level.ContextLevelAccessor;
import com.mohistmc.silkard.injected.world.level.chunk.ContextChunkGenerator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.generator.CraftLimitedRegion;
import org.bukkit.craftbukkit.generator.structure.CraftStructure;
import org.bukkit.craftbukkit.util.RandomSourceWrapper;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(ChunkGenerator.class)
public abstract class MixinChunkGenerator implements ContextChunkGenerator {

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, boolean vanilla) {
        if (vanilla) {
            ((ChunkGenerator) (Object) this).applyBiomeDecoration(level, chunk, structureManager);
        } else {
            this.silkard$runPopulators(level, chunk);
        }
    }

    // CraftBukkit - run Bukkit block populators after vanilla decoration
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void silkard$runPopulators(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, CallbackInfo ci) {
        this.silkard$runPopulators(level, chunk);
    }

    @Unique
    private void silkard$runPopulators(WorldGenLevel level, ChunkAccess chunk) {
        World world = ((ContextLevelAccessor) level).getMinecraftWorld().getWorld();
        if (world.getPopulators().isEmpty()) {
            return;
        }

        CraftLimitedRegion limitedRegion = new CraftLimitedRegion(level, chunk.getPos());
        int x = chunk.getPos().x();
        int z = chunk.getPos().z();
        for (BlockPopulator populator : world.getPopulators()) {
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
            random.setDecorationSeed(level.getSeed(), x, z);
            populator.populate(world, new RandomSourceWrapper.RandomWrapper(random), x, z, limitedRegion);
        }
        limitedRegion.saveEntities();
        limitedRegion.breakLink();
    }

    // CraftBukkit - AsyncStructureSpawnEvent
    @WrapWithCondition(method = "tryGenerateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;setStartForStructure(Lnet/minecraft/world/level/levelgen/structure/Structure;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/StructureAccess;)V"))
    private static boolean silkard$structureSpawn(StructureManager structureManager, Structure structure, StructureStart structureStart, StructureAccess access,
                                                  @Local(argsOnly = true) ChunkPos sourceChunkPos) {
        net.minecraft.world.level.levelgen.structure.BoundingBox box = structureStart.getBoundingBox();
        AsyncStructureSpawnEvent event = new AsyncStructureSpawnEvent(
                structureManager.level.getMinecraftWorld().getWorld(),
                CraftStructure.minecraftToBukkit(structure),
                new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()),
                sourceChunkPos.x(), sourceChunkPos.z());
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
