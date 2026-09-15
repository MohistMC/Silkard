package org.bukkit.craftbukkit.inventory.components;

import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.craftbukkit.inventory.SerializableMeta;
import org.bukkit.inventory.meta.components.SwingAnimationComponent;

@SerializableAs("SwingAnimation")
public class CraftSwingAnimationComponent implements SwingAnimationComponent {

    private SwingAnimation handle;

    public CraftSwingAnimationComponent(SwingAnimation handle) {
        this.handle = handle;
    }

    public CraftSwingAnimationComponent(CraftSwingAnimationComponent component) {
        this.handle = component.handle;
    }

    public CraftSwingAnimationComponent(Map<String, Object> map) {
        Type type = Type.valueOf(SerializableMeta.getString(map, "type", false));
        Integer duration = SerializableMeta.getObject(Integer.class, map, "duration", false);

        handle = new SwingAnimation(CraftSwingAnimationType.bukkitToMinecraft(type), duration);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("type", getType().name());
        result.put("duration", getDuration());

        return result;
    }

    public SwingAnimation getHandle() {
        return handle;
    }

    @Override
    public Type getType() {
        return CraftSwingAnimationType.minecraftToBukkit(handle.type());
    }

    @Override
    public void setType(Type type) {
        Preconditions.checkArgument(type != null, "Type cannot be null");

        handle = new SwingAnimation(CraftSwingAnimationType.bukkitToMinecraft(type), handle.duration());
    }

    @Override
    public int getDuration() {
        return handle.duration();
    }

    @Override
    public void setDuration(int ticks) {
        handle = new SwingAnimation(handle.type(), ticks);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.handle);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CraftSwingAnimationComponent other = (CraftSwingAnimationComponent) obj;
        return Objects.equals(this.handle, other.handle);
    }

    @Override
    public String toString() {
        return "CraftSwingAnimationComponent{" + "handle=" + handle + '}';
    }

    public static class CraftSwingAnimationType {

        public static SwingAnimationComponent.Type minecraftToBukkit(SwingAnimationType minecraft) {
            return SwingAnimationComponent.Type.valueOf(minecraft.name());
        }

        public static SwingAnimationType bukkitToMinecraft(SwingAnimationComponent.Type bukkit) {
            return SwingAnimationType.valueOf(bukkit.name());
        }
    }
}
