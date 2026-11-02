package com.mohistmc.silkard.feature.ban.bans;

import com.mohistmc.silkard.SilkardConfig;

/**
 * @author Mgazul
 * {@code @date} 2023/8/9 20:09:51
 */
public class BanEvents {

    public static boolean banFireTick() {
        return SilkardConfig.doFireTick;
    }

    public static boolean banExplosion() {
        return SilkardConfig.explosion;
    }

    public static boolean banFarmlandTrample() {
        return SilkardConfig.farmlandTrample;
    }
}
