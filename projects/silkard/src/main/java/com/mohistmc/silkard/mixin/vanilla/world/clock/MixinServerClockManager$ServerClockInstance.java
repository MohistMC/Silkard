package com.mohistmc.silkard.mixin.vanilla.world.clock;

import com.mohistmc.silkard.injected.server.level.ContextServerPlayer;
import com.mohistmc.silkard.injected.world.clock.ContextServerClockInstance;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(ServerClockManager.ServerClockInstance.class)
public abstract class MixinServerClockManager$ServerClockInstance implements ContextServerClockInstance {

    @Shadow private long totalTicks;
    @Shadow private float partialTick;

    @Shadow public abstract boolean isPaused();
    @Shadow public abstract float rate();

    @Override
    public void silkard$totalTicks(long totalTicks) {
        this.totalTicks = totalTicks;
    }

    @Override
    public void silkard$partialTick(float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public ClockNetworkState silkard$packNetworkState(Holder<WorldClock> clock, ServerPlayer player) {
        boolean advanceTime = player.level().getGameRules().get(GameRules.ADVANCE_TIME);
        boolean paused = this.isPaused() || !advanceTime;
        boolean isDefault = player.level().dimensionType().defaultClock().map(defaultClock -> clock.is(defaultClock)).orElse(false);
        long ticks = isDefault ? ((ContextServerPlayer) player).getPlayerTime(this.totalTicks) : this.totalTicks;
        return new ClockNetworkState(ticks, this.partialTick, paused ? 0.0F : this.rate());
    }
}
