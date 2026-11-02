package com.mohistmc.silkard.feature.ban.bans;

import com.mohistmc.tools.ListUtils;
import com.mohistmc.silkard.SilkardConfig;
import com.mohistmc.silkard.feature.ban.BanConfig;
import com.mohistmc.silkard.feature.ban.BanType;
import com.mohistmc.silkard.feature.ban.ClickType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.bukkit.entity.Player;

public class BanRecipe {

    public static Set<Identifier> CACHE = new HashSet<>();

    public static void addBan(Player player, String key) {
        List<String> old = BanConfig.getListByType(BanType.RECIPE);
        ListUtils.isDuplicate(old, key);
        BanConfig.saveToYaml(player, ClickType.ADD, old, BanType.RECIPE);
    }

    public static boolean checkBan(Identifier resourceLocation) {
        if (!SilkardConfig.yml.getBoolean("bans.recipe", false)) return false;
        CACHE.add(resourceLocation);
        var list = BanConfig.getListByType(BanType.RECIPE);
        if (list.isEmpty()) return false;
        return list.contains(resourceLocation.toString());
    }
}
