package com.mohistmc.silkard.util;

import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
public final class BukkitUtils {

    private BukkitUtils() {
    }

    public static boolean callVehicleEntityCollisionEvent(Entity vehicle, Entity target) {
        VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent((Vehicle) vehicle.getBukkitEntity(), target.getBukkitEntity());
        vehicle.level().getCraftServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
