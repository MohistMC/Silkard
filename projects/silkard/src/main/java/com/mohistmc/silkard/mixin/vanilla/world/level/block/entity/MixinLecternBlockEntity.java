package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.mohistmc.silkard.bukkit.inventory.BukkitLecternInventory;
import com.mohistmc.silkard.injected.world.inventory.ContextLecternMenu;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.command.CraftBlockCommandSender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.sugar.Local;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(LecternBlockEntity.class)
public abstract class MixinLecternBlockEntity {

    @Shadow
    @Final
    @Mutable
    private Container bookAccess;

    @Unique
    private static final CommandSource SILKARD$COMMAND_SOURCE = new CommandSource() {
        @Override
        public void sendSystemMessage(Component component) {
        }

        public CommandSender getBukkitSender(CommandSourceStack wrapper) {
            return wrapper.getEntity() != null ? wrapper.getEntity().getBukkitEntity()
                    : new CraftBlockCommandSender(wrapper, wrapper.getLevel().getBlockEntity(BlockPos.containing(wrapper.getPosition())));
        }

        @Override
        public boolean acceptsSuccess() {
            return false;
        }

        @Override
        public boolean acceptsFailure() {
            return false;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    };

    // CraftBukkit - expose the lectern container
    @Inject(method = "<init>", at = @At("TAIL"))
    private void silkard$lecternInventory(BlockPos pos, BlockState state, CallbackInfo ci) {
        this.bookAccess = new BukkitLecternInventory((LecternBlockEntity) (Object) this);
    }

    // CraftBukkit - skip signal when the world is null
    @Redirect(method = "setPage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LecternBlock;signalPageChange(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void silkard$signalPageChange(Level level, BlockPos pos, BlockState state) {
        if (level != null) {
            LecternBlock.signalPageChange(level, pos, state);
        }
    }

    // CraftBukkit - SPIGOT-5500
    @Inject(method = "preRemoveSideEffects", at = @At(value = "NEW", target = "Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void silkard$dropBook(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (((LecternBlockEntity) (Object) this).getBook().copy().isEmpty()) {
            ci.cancel();
        }
    }

    // CraftBukkit - pass the player inventory to the menu
    @Redirect(method = "createMenu", at = @At(value = "NEW", target = "Lnet/minecraft/world/inventory/LecternMenu;"))
    private LecternMenu silkard$createMenu(int containerId, Container container, ContainerData data, @Local(argsOnly = true) Inventory inventory) {
        LecternMenu menu = new LecternMenu(containerId, container, data);
        ((ContextLecternMenu) menu).silkard$player(inventory);
        return menu;
    }

    // CraftBukkit - custom command sender
    @Redirect(method = "createCommandSourceStack", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/CommandSource;NULL:Lnet/minecraft/commands/CommandSource;"))
    private static CommandSource silkard$commandSource() {
        return SILKARD$COMMAND_SOURCE;
    }
}
