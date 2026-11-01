package com.mohistmc.silkard.mixin.vanilla.world.clock;

import com.mohistmc.silkard.injected.world.clock.ContextServerClockInstance;
import com.mohistmc.silkard.injected.world.clock.ContextServerClockManager;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(ServerClockManager.class)
public abstract class MixinServerClockManager implements ContextServerClockManager {

    @Shadow private MinecraftServer server;
    @Shadow @Final private Map<Holder<WorldClock>, ServerClockManager.ServerClockInstance> clocks;

    @Shadow public abstract ServerClockManager.ServerClockInstance getInstance(Holder<WorldClock> clock);
    @Shadow public abstract void setTotalTicks(Holder<WorldClock> clock, long totalTicks);

    @Unique private ServerLevel silkard$level;
    @Unique private TimeSkipEvent.SkipReason silkard$reason;

    @Override
    public void silkard$init(ServerLevel level) {
        this.silkard$level = level;
    }

    private static boolean silkard$isDefaultClock(ServerLevel level, Holder<WorldClock> clock) {
        return level.dimensionType().defaultClock().map(defaultClock -> clock.is(defaultClock)).orElse(false);
    }

    // CraftBukkit - per-world game rules
    @Overwrite
    public void tick() {
        boolean advanceTime = this.silkard$level.getGameRules().get(GameRules.ADVANCE_TIME);
        if (advanceTime) {
            this.clocks.values().forEach(ServerClockManager.ServerClockInstance::tick);
            ((net.minecraft.world.level.saveddata.SavedData) (Object) this).setDirty();
        }
    }

    // CraftBukkit - per-world game time
    @Overwrite
    private long getGameTime() {
        return this.silkard$level.getGameTime();
    }

    @Overwrite
    private void modifyClock(Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action) {
        this.silkard$modifyClock(clock, action, null);
    }

    @Override
    public void setTotalTicks(Holder<WorldClock> clock, long totalTicks, TimeSkipEvent.SkipReason reason) {
        this.silkard$reason = reason;
        try {
            this.setTotalTicks(clock, totalTicks);
        } finally {
            this.silkard$reason = null;
        }
    }

    @Unique
    private void silkard$modifyClock(Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action, TimeSkipEvent.SkipReason reason) {
        ServerClockManager.ServerClockInstance instance = this.getInstance(clock);
        long ticksBefore = instance.totalTicks();
        action.accept(instance);
        long tickDelta = instance.totalTicks() - ticksBefore;
        // CraftBukkit start - TimeSkipEvent
        if (tickDelta != 0 && reason != null && silkard$isDefaultClock(this.silkard$level, clock)) {
            TimeSkipEvent event = new TimeSkipEvent(this.silkard$level.getWorld(), reason, tickDelta);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                ((ContextServerClockInstance) instance).silkard$totalTicks(ticksBefore + event.getSkipAmount());
            }
        }
        // CraftBukkit end

        for (ServerPlayer player : this.silkard$level.players()) {
            ClockNetworkState state = ((ContextServerClockInstance) instance).silkard$packNetworkState(clock, player);
            player.connection.send(new ClientboundSetTimePacket(this.getGameTime(), Map.of(clock, state)));
        }
        ((net.minecraft.world.level.saveddata.SavedData) (Object) this).setDirty();
        this.silkard$level.environmentAttributes().invalidateTickCache();
    }

    // CraftBukkit start - reason plumbing
    @Redirect(method = "setTotalTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V"))
    private void silkard$setTotalTicksReason(ServerClockManager self, Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action) {
        this.silkard$modifyClock(clock, action, this.silkard$reason);
    }

    @Redirect(method = "addTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V"))
    private void silkard$addTicksReason(ServerClockManager self, Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action) {
        this.silkard$modifyClock(clock, action, TimeSkipEvent.SkipReason.COMMAND);
    }

    @Redirect(method = "setPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V"))
    private void silkard$setPausedReason(ServerClockManager self, Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action) {
        this.silkard$modifyClock(clock, action, TimeSkipEvent.SkipReason.COMMAND);
    }

    @Redirect(method = "setRate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V"))
    private void silkard$setRateReason(ServerClockManager self, Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action) {
        this.silkard$modifyClock(clock, action, TimeSkipEvent.SkipReason.COMMAND);
    }

    @Redirect(method = "moveToTimeMarker", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V"))
    private void silkard$moveToTimeMarkerReason(ServerClockManager self, Holder<WorldClock> clock, Consumer<? super ServerClockManager.ServerClockInstance> action) {
        this.silkard$modifyClock(clock, action, null);
    }
    // CraftBukkit end
}
