package com.mohistmc.silkard.mixin.vanilla.world.level.storage;

import com.mohistmc.silkard.SilkardMod;
import com.mohistmc.silkard.injected.world.level.storage.ContextLevelStorageAccess;
import com.mohistmc.silkard.injected.world.level.storage.ContextLevelStorageSource;
import com.mohistmc.silkard.injected.world.level.storage.ContextPrimaryLevelData;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.ContentValidationException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(LevelStorageSource.class)
public class MixinLevelStorageSource implements ContextLevelStorageSource {

    // CraftBukkit start
    @Override
    public LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String levelId, ResourceKey<LevelStem> dimensionType) throws IOException, ContentValidationException {
        LevelStorageSource.LevelStorageAccess access = ((LevelStorageSource) (Object) this).validateAndCreateAccess(levelId);
        ((ContextLevelStorageAccess) access).silkard$setDimType(dimensionType);
        return access;
    }

    public LevelStorageSource.LevelStorageAccess createAccess(String levelId, ResourceKey<LevelStem> dimensionType) throws IOException {
        LevelStorageSource.LevelStorageAccess access = ((LevelStorageSource) (Object) this).createAccess(levelId);
        ((ContextLevelStorageAccess) access).silkard$setDimType(dimensionType);
        return access;
    }

    private static Path getPre261StorageFolder(Path path, ResourceKey<LevelStem> dimensionType) {
        if (dimensionType == LevelStem.OVERWORLD) {
            return path;
        } else if (dimensionType == LevelStem.NETHER) {
            return path.resolve("DIM-1");
        } else if (dimensionType == LevelStem.END) {
            return path.resolve("DIM1");
        } else {
            return path.resolve("dimensions").resolve(dimensionType.identifier().getNamespace()).resolve(dimensionType.identifier().getPath());
        }
    }
    // missing settings file: build defaults from presets, the datapack registry has no builtin dimensions
    @Inject(method = "getLevelDataAndDimensions", at = @At("HEAD"), cancellable = true)
    private static void silkard$fallbackDimensions(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, WorldDataConfiguration dataConfiguration, Registry<LevelStem> datapackDimensions, HolderLookup.Provider registryAccess, CallbackInfoReturnable<LevelDataAndDimensions> cir) {
        Path dataFile = WorldGenSettings.TYPE.id().withSuffix(".dat").resolveAgainst(worldAccess.getLevelPath(LevelResource.DATA));
        if (Files.exists(dataFile)) {
            return;
        }
        SilkardMod.LOGGER.warn("World gen settings missing, rebuilding default dimensions");
        WorldGenSettings worldGenSettings = new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(registryAccess));
        Dynamic<?> dataTag = RegistryOps.injectRegistryContext(levelDataTag, registryAccess);
        LevelSettings levelSettings = LevelSettings.parse(dataTag, dataConfiguration);
        WorldDimensions.Complete dimensions = worldGenSettings.dimensions().bake(datapackDimensions);
        Lifecycle lifecycle = dimensions.lifecycle().add(registryAccess.allRegistriesLifecycle());
        PrimaryLevelData worldData = PrimaryLevelData.parse(dataTag, levelSettings, dimensions.specialWorldProperty(), lifecycle);
        ((ContextPrimaryLevelData) worldData).silkard$pdc(((Dynamic<Tag>) dataTag).getElement("BukkitValues", null));
        cir.setReturnValue(LevelDataAndDimensions.create(worldData, worldGenSettings, dimensions));
    }

    // CraftBukkit start - add BukkitValues PDC to the loaded world data
    @Redirect(method = "getLevelDataAndDimensions", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/PrimaryLevelData;parse(Lcom/mojang/serialization/Dynamic;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/world/level/storage/PrimaryLevelData$SpecialWorldProperty;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/world/level/storage/PrimaryLevelData;"))
    private static PrimaryLevelData silkard$bukkitValues(Dynamic<?> dynamic, LevelSettings settings, PrimaryLevelData.SpecialWorldProperty specialWorldProperty, com.mojang.serialization.Lifecycle lifecycle) {
        PrimaryLevelData data = PrimaryLevelData.parse(dynamic, settings, specialWorldProperty, lifecycle);
        ((ContextPrimaryLevelData) data).silkard$pdc(((Dynamic<Tag>) dynamic).getElement("BukkitValues", null));
        return data;
    }
    // CraftBukkit end
}
