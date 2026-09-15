package org.bukkit.craftbukkit.advancement;

import net.minecraft.advancements.DisplayInfo;
import org.bukkit.advancement.AdvancementDisplayType;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.inventory.ItemStack;

public class CraftAdvancementDisplay implements org.bukkit.advancement.AdvancementDisplay {

    private final DisplayInfo handle;

    public CraftAdvancementDisplay(DisplayInfo handle) {
        this.handle = handle;
    }

    public DisplayInfo getHandle() {
        return handle;
    }

    @Override
    public String getTitle() {
        return CraftChatMessage.fromComponent(handle.title());
    }

    @Override
    public String getDescription() {
        return CraftChatMessage.fromComponent(handle.description());
    }

    @Override
    public ItemStack getIcon() {
        return CraftItemStack.asCraftMirror(handle.icon());
    }

    @Override
    public boolean shouldShowToast() {
        return handle.showToast();
    }

    @Override
    public boolean shouldAnnounceChat() {
        return handle.announceToChat();
    }

    @Override
    public boolean isHidden() {
        return handle.hidden();
    }

    @Override
    public float getX() {
        return 0.0F;
    }

    @Override
    public float getY() {
        return 0.0F;
    }

    @Override
    public AdvancementDisplayType getType() {
        return AdvancementDisplayType.values()[handle.type().ordinal()];
    }
}
