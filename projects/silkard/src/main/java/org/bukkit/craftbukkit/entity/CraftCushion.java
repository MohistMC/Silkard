package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Cushion;

public class CraftCushion extends CraftBlockAttachedEntity implements Cushion {

    public CraftCushion(CraftServer server, net.minecraft.world.entity.decoration.Cushion entity) {
        super(server, entity);
    }

    @Override
    public boolean setFacingDirection(BlockFace face, boolean force) {
        Preconditions.checkArgument(face == BlockFace.UP, "%s is not a valid facing direction", face);

        return force || getHandle().silkard_generation() || getHandle().survives();
    }

    @Override
    public BlockFace getFacing() {
        return BlockFace.UP;
    }

    @Override
    public BlockFace getAttachedFace() {
        return BlockFace.UP;
    }

    @Override
    public void setFacingDirection(BlockFace face) {
    }

    @Override
    public net.minecraft.world.entity.decoration.Cushion getHandle() {
        return (net.minecraft.world.entity.decoration.Cushion) entity;
    }

    @Override
    public String toString() {
        return "CraftCushion";
    }
}
