package com.mohistmc.silkard.bukkit.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;

public class SilkardModsBlockEntityState extends CraftBlockEntityState<BlockEntity> {

    public SilkardModsBlockEntityState(World world, BlockEntity blockEntity) {
        super(world, blockEntity);
    }

    protected SilkardModsBlockEntityState(SilkardModsBlockEntityState state, Location location) {
        super(state, location);
    }

    @Override
    public SilkardModsBlockEntityState copy() {
        return new SilkardModsBlockEntityState(this, null);
    }

    @Override
    public SilkardModsBlockEntityState copy(Location location) {
        return new SilkardModsBlockEntityState(this, location);
    }
}
