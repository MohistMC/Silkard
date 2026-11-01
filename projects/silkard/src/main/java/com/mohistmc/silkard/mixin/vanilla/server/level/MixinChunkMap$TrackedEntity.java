package com.mohistmc.silkard.mixin.vanilla.server.level;

import com.mohistmc.silkard.injected.server.level.ContextServerEntity;
import java.util.Set;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class MixinChunkMap$TrackedEntity {

    @Shadow
    @Final
    private ServerEntity serverEntity;

    @Shadow
    @Final
    private Set<ServerPlayerConnection> seenBy;

    // CraftBukkit - hand the tracked player set to the entity tracker
    @Inject(method = "<init>", at = @At("TAIL"))
    private void silkard$trackedPlayers(CallbackInfo ci) {
        ((ContextServerEntity) (Object) this.serverEntity).setTrackedPlayers(this.seenBy);
    }

    // CraftBukkit - respect the vanish API
    @Redirect(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean silkard$canSee(Entity self, ServerPlayer player) {
        if (!player.getBukkitEntity().canSee(self.getBukkitEntity())) {
            return false;
        }
        return self.broadcastToPlayer(player);
    }
}
