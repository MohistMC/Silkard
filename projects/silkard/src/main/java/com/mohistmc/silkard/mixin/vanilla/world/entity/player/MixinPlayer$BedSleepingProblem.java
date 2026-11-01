package com.mohistmc.silkard.mixin.vanilla.world.entity.player;

import com.mohistmc.silkard.injected.world.entity.player.ContextPlayerBedSleepingProblem;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Mgazul
 * @date 2026/6/8
 */
@Mixin(Player.BedSleepingProblem.class)
public class MixinPlayer$BedSleepingProblem implements ContextPlayerBedSleepingProblem {

    // CraftBukkit start - CB adds a PlayerBedEnterEvent.BedEnterResult component to the record
    @Unique
    private PlayerBedEnterEvent.BedEnterResult silkard$bukkit;

    @Override
    public PlayerBedEnterEvent.BedEnterResult bukkit() {
        PlayerBedEnterEvent.BedEnterResult result = this.silkard$bukkit;
        if (result != null) {
            return result;
        }
        Player.BedSleepingProblem self = (Player.BedSleepingProblem) (Object) this;
        if (self == Player.BedSleepingProblem.TOO_FAR_AWAY) {
            return PlayerBedEnterEvent.BedEnterResult.TOO_FAR_AWAY;
        }
        if (self == Player.BedSleepingProblem.NOT_SAFE) {
            return PlayerBedEnterEvent.BedEnterResult.NOT_SAFE;
        }
        // OBSTRUCTED and OTHER_PROBLEM both map to OTHER_PROBLEM in CB
        return PlayerBedEnterEvent.BedEnterResult.OTHER_PROBLEM;
    }

    @Override
    public void silkard$setBukkit(PlayerBedEnterEvent.BedEnterResult result) {
        this.silkard$bukkit = result;
    }
    // CraftBukkit end
}
