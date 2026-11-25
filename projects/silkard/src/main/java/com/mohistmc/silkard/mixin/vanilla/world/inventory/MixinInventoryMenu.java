package com.mohistmc.silkard.mixin.vanilla.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// craftSlots / resultSlots 由 AbstractCraftingMenu 声明（protected），@Shadow 命中不了
// 继承成员，改为直接 extends AbstractCraftingMenu 取值。
@Mixin(InventoryMenu.class)
public abstract class MixinInventoryMenu extends AbstractCraftingMenu {

    @Shadow @Final private Player owner;

    // CraftBukkit start
    @Unique
    private CraftInventoryView bukkitEntity = null;

    protected MixinInventoryMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId, 0, 0);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        bukkitEntity = new CraftInventoryView(this.owner.getBukkitEntity(), inventory, (InventoryMenu) (Object) this);
        return bukkitEntity;
    }
    // CraftBukkit end

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"))
    private void silkard$init(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        ((InventoryMenu) (Object) this).setTitle(net.minecraft.network.chat.Component.translatable("container.crafting"));
        ((com.mohistmc.silkard.injected.world.inventory.ContextAbstractContainerMenu) (Object) this).containerOwner(owner);
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    public void silkard$stillValid(net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!silkard$checkReachable()) {
            cir.setReturnValue(true);
        }
    }
}
