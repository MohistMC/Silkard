package com.mohistmc.silkard.mixin.vanilla.world.level.block.entity;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.event.block.SignChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/30
 */
@Mixin(SignBlockEntity.class)
public abstract class MixinSignBlockEntity {

    @Shadow
    private UUID playerWhoMayEdit;

    @Unique
    private Player silkard$editor;

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

    // CraftBukkit start - SignChangeEvent
    @Inject(method = "updateSignText", at = @At("HEAD"))
    private void silkard$captureEditor(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        this.silkard$editor = player;
    }

    @Redirect(method = "updateSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;updateText(Ljava/util/function/UnaryOperator;Lnet/minecraft/world/level/block/entity/SignTextSlot;)Z"))
    private boolean silkard$signChange(SignBlockEntity self, java.util.function.UnaryOperator<SignText> operator, SignTextSlot slot) {
        SignText originalText = self.getText(slot);
        SignText updated = operator.apply(originalText);
        Player player = this.silkard$editor;

        if (player == null || self.getLevel() == null) {
            return originalText != updated;
        }

        String[] linesArray = new String[4];
        List<Component> messages = updated.getMessages(player.isTextFilteringEnabled());
        for (int i = 0; i < linesArray.length && i < messages.size(); ++i) {
            linesArray[i] = CraftChatMessage.fromComponent(messages.get(i));
        }

        SignChangeEvent event = new SignChangeEvent(CraftBlock.at(self.getLevel(), self.getBlockPos()), (org.bukkit.entity.Player) player.getBukkitEntity(), linesArray.clone(), slot == SignTextSlot.FRONT ? Side.FRONT : Side.BACK);
        self.getLevel().getCraftServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        Component[] components = CraftSign.sanitizeLines(event.getLines());
        SignText.Mutable mutable = updated.asMutable();
        for (int i = 0; i < components.length; i++) {
            if (!Objects.equals(linesArray[i], event.getLine(i))) {
                mutable.setLine(i, components[i]);
            }
        }
        self.setText(mutable.asImmutable(), slot);
        return true;
    }

    // CraftBukkit - resend the current text to a player who may no longer edit
    @Inject(method = "updateSignText", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void silkard$resendUpdate(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(((SignBlockEntity) (Object) this).getUpdatePacket());
        }
    }

    // CraftBukkit - lazy clear instead of ticking
    @Inject(method = "getPlayerWhoMayEdit", at = @At("HEAD"))
    private void silkard$lazyClearEditor(CallbackInfoReturnable<UUID> cir) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getLevel() != null && this.playerWhoMayEdit != null && self.playerIsTooFarAwayToEdit(this.playerWhoMayEdit)) {
            self.setAllowedPlayerEditor(null);
        }
    }

    // CraftBukkit - SPIGOT-5122
    @Redirect(method = "markUpdated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V"))
    private void silkard$markUpdated(Level level, BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        if (level != null) {
            level.sendBlockUpdated(pos, oldState, newState, flags);
        }
    }

    // CraftBukkit - pass the player to custom click actions
    @Redirect(method = "executeClickCommandsIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"))
    private void silkard$customClick(MinecraftServer server, Identifier id, Optional<Tag> payload, @Local(argsOnly = true) Player player) {
        CraftEventFactory.callPlayerCustomClickEvent(id, payload, (ServerPlayer) player);
        server.handleCustomClickAction(id, payload);
    }

    // CraftBukkit - custom command sender
    @Redirect(method = "createCommandSourceStack", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/CommandSource;NULL:Lnet/minecraft/commands/CommandSource;"))
    private static CommandSource silkard$commandSource() {
        return SILKARD$COMMAND_SOURCE;
    }
    // CraftBukkit end
}
