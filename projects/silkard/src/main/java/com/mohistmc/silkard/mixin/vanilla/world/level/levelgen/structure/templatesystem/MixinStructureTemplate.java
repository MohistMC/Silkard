package com.mohistmc.silkard.mixin.vanilla.world.level.levelgen.structure.templatesystem;

import com.mohistmc.silkard.injected.world.level.levelgen.structure.templatesystem.ContextStructureTemplate;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/4/2 17:35
 */
@Mixin(StructureTemplate.class)
public class MixinStructureTemplate implements ContextStructureTemplate {

    // CraftBukkit start - data containers
    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public CraftPersistentDataContainer persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);
    // CraftBukkit end

    @Override
    public CraftPersistentDataContainer silkard_persistentDataContainer() {
        return persistentDataContainer;
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void silkard_bukkitSave(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        // CraftBukkit start - PDC
        if (!this.persistentDataContainer.isEmpty()) {
            tag.put("BukkitValues", this.persistentDataContainer.toTagCompound());
        }
        // CraftBukkit end
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void silkard_bukkitLoad(HolderGetter<Block> blockLookup, CompoundTag tag, CallbackInfo ci) {
        // CraftBukkit start - PDC
        Tag base = tag.get("BukkitValues");
        if (base instanceof CompoundTag) {
            this.persistentDataContainer.putAll((CompoundTag) base);
        }
        // CraftBukkit end
    }

    // CraftBukkit start - no swallowed exception
    @Inject(method = "createEntityIgnoreException", at = @At("HEAD"), cancellable = true)
    private static void silkard$createEntity(ProblemReporter reporter, ServerLevelAccessor level, CompoundTag tag, CallbackInfoReturnable<Optional<Entity>> cir) {
        cir.setReturnValue(EntityType.create(TagValueInput.create(reporter, level.registryAccess(), tag), level.getLevel(), new EntitySpawnRequest(EntitySpawnReason.STRUCTURE, false)));
    }
    // CraftBukkit end
}
