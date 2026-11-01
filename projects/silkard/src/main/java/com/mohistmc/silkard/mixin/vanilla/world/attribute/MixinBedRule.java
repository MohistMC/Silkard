package com.mohistmc.silkard.mixin.vanilla.world.attribute;

import com.mohistmc.silkard.injected.world.attribute.ContextBedRule;
import com.mohistmc.silkard.injected.world.entity.player.ContextPlayerBedSleepingProblem;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(BedRule.class)
public class MixinBedRule implements ContextBedRule {

    // CraftBukkit start - CB adds a PlayerBedEnterEvent.BedEnterResult component to the record
    @Unique
    private PlayerBedEnterEvent.BedEnterResult silkard$bukkit;

    @Override
    public PlayerBedEnterEvent.BedEnterResult bukkit() {
        PlayerBedEnterEvent.BedEnterResult result = this.silkard$bukkit;
        if (result != null) {
            return result;
        }
        BedRule self = (BedRule) (Object) this;
        if (self.destroyOnUse() || self.destroyOnLeave()) {
            return PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_HERE;
        }
        if (self.canSleep() == BedRule.Rule.WHEN_DARK) {
            return PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_NOW;
        }
        return PlayerBedEnterEvent.BedEnterResult.OTHER_PROBLEM;
    }

    @Overwrite
    public Player.BedSleepingProblem asProblem() {
        BedRule self = (BedRule) (Object) this;
        Player.BedSleepingProblem problem = new Player.BedSleepingProblem(self.errorMessage().orElse(null));
        ((ContextPlayerBedSleepingProblem) problem).silkard$setBukkit(this.bukkit());
        return problem;
    }
    // CraftBukkit end
}
