package org.bukkit.craftbukkit.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.DecoratedPot;
import org.bukkit.craftbukkit.inventory.CraftInventoryDecoratedPot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.inventory.DecoratedPotInventory;

public class CraftDecoratedPot extends CraftBlockEntityState<DecoratedPotBlockEntity> implements DecoratedPot {

    public CraftDecoratedPot(World world, DecoratedPotBlockEntity tileEntity) {
        super(world, tileEntity);
    }

    protected CraftDecoratedPot(CraftDecoratedPot state, Location location) {
        super(state, location);
    }

    @Override
    public DecoratedPotInventory getSnapshotInventory() {
        return new CraftInventoryDecoratedPot(this.getSnapshot());
    }

    @Override
    public DecoratedPotInventory getInventory() {
        if (!this.isPlaced()) {
            return this.getSnapshotInventory();
        }

        return new CraftInventoryDecoratedPot(this.getTileEntity());
    }

    @Override
    public void setSherd(Side face, Material sherd) {
        Preconditions.checkArgument(face != null, "face must not be null");
        Preconditions.checkArgument(sherd == null || sherd == Material.BRICK || Tag.ITEMS_DECORATED_POT_SHERDS.isTagged(sherd), "sherd is not a valid sherd material: %s", sherd);

        Optional<ItemStackTemplate> sherdItem = Optional.of(ItemStackTemplate.fromStack(new ItemStack((sherd != null) ? CraftItemType.bukkitToMinecraft(sherd) : Items.BRICK)));
        PotDecorations decorations = getSnapshot().getDecorations();

        switch (face) {
            case BACK -> getSnapshot().decorations = new PotDecorations(sherdItem, decorations.left(), decorations.right(), decorations.front());
            case LEFT -> getSnapshot().decorations = new PotDecorations(decorations.back(), sherdItem, decorations.right(), decorations.front());
            case RIGHT -> getSnapshot().decorations = new PotDecorations(decorations.back(), decorations.left(), sherdItem, decorations.front());
            case FRONT -> getSnapshot().decorations = new PotDecorations(decorations.back(), decorations.left(), decorations.right(), sherdItem);
            default -> throw new IllegalArgumentException("Unexpected value: " + face);
        }
    }

    private static Material getSherdItem(Optional<ItemStackTemplate> sherdItem) {
        return sherdItem.map(CraftItemStack::asCraftMirror).map(CraftItemStack::getType).orElse(Material.BRICK);
    }

    @Override
    public Material getSherd(Side face) {
        Preconditions.checkArgument(face != null, "face must not be null");

        PotDecorations decorations = getSnapshot().getDecorations();
        Optional<ItemStackTemplate> sherdItem = switch (face) {
            case BACK -> decorations.back();
            case LEFT -> decorations.left();
            case RIGHT -> decorations.right();
            case FRONT -> decorations.front();
            default -> throw new IllegalArgumentException("Unexpected value: " + face);
        };

        return getSherdItem(sherdItem);
    }

    @Override
    public Map<Side, Material> getSherds() {
        PotDecorations decorations = getSnapshot().getDecorations();

        Map<Side, Material> sherds = new EnumMap<>(Side.class);
        sherds.put(Side.BACK, getSherdItem(decorations.back()));
        sherds.put(Side.LEFT, getSherdItem(decorations.left()));
        sherds.put(Side.RIGHT, getSherdItem(decorations.right()));
        sherds.put(Side.FRONT, getSherdItem(decorations.front()));
        return sherds;
    }

    @Override
    public List<Material> getShards() {
        return ImmutableList.<Material>builder().addAll(getSherds().values()).build();
    }

    @Override
    public CraftDecoratedPot copy() {
        return new CraftDecoratedPot(this, null);
    }

    @Override
    public CraftDecoratedPot copy(Location location) {
        return new CraftDecoratedPot(this, location);
    }
}
