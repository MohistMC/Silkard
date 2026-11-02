package com.mohistmc.silkard.api;

import com.mohistmc.silkard.feature.ban.BanConfig;
import com.mohistmc.silkard.feature.ban.BanType;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.EntityType;

public class EntityAPI {

    public static EntityType entityType(String entityName) {
        EntityType type = EntityType.fromName(entityName);
        return Objects.requireNonNullElse(type, EntityType.UNKNOWN);
    }

    public static EntityType entityType(String entityName, EntityType defType) {
        EntityType type = EntityType.fromName(entityName);
        return type != null ? type : defType;
    }

    public static net.minecraft.world.entity.EntityType<?> getType(String resourceLocation) {
        return BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(resourceLocation)).get().value();
    }

    public static String resourceLocation(Entity nmsEntity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(nmsEntity.getType());
        return key.toString();
    }

    public static boolean isBan(Entity entity) {
        if (entity == null || BanConfig.ENTITY == null) return false;
        var list = BanConfig.getListByType(BanType.ENTITY);
        if (list.isEmpty()) return false;
        return list.contains(resourceLocation(entity));
    }
}
