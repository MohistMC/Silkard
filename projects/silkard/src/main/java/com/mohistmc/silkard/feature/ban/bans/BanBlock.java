package com.mohistmc.silkard.feature.ban.bans;

import com.mohistmc.silkard.SilkardConfig;
import com.mohistmc.silkard.feature.ban.BanConfig;
import com.mohistmc.silkard.feature.ban.BanType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul
 * {@code @date} 2025/11/1 19:00:23
 */
public class BanBlock {

    public static boolean check(BlockState p_46606_) {
        if (!SilkardConfig.ban_block_enable) return false;
        if (p_46606_.isAir()) return false;
        var list = BanConfig.getListByType(BanType.BLOCK);
        if (list.isEmpty()) return false;
        Block block = p_46606_.getBlock();
        ItemStack bukkitBlock = CraftItemStack.asBukkitCopy(block.asItem().getDefaultInstance());
        return list.contains(bukkitBlock.getType().getKey().toString());
    }
}
