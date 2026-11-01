package com.mohistmc.silkard.mixin.vanilla.world.level.saveddata.maps;

import com.mohistmc.silkard.injected.world.level.saveddata.maps.ContextMapItemSavedData;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.map.CraftMapCursor;
import org.bukkit.craftbukkit.map.RenderData;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.map.MapCursor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(MapItemSavedData.HoldingPlayer.class)
public abstract class MixinMapItemSavedData$HoldingPlayer {

    @Shadow @Final MapItemSavedData this$0;
    @Shadow public Player player;
    @Shadow private boolean dirtyData;
    @Shadow private int minDirtyX;
    @Shadow private int minDirtyY;
    @Shadow private int maxDirtyX;
    @Shadow private int maxDirtyY;
    @Shadow private boolean dirtyDecorations;
    @Shadow private int tick;

    // CraftBukkit start - render through the CraftMapView so plugins can customize maps
    @Overwrite
    private Packet<?> nextUpdatePacket(MapId id) {
        RenderData render = ((ContextMapItemSavedData) this.this$0).silkard_mapView().render((CraftPlayer) this.player.getBukkitEntity());
        MapItemSavedData.MapPatch mapPatch;

        if (this.dirtyData) {
            this.dirtyData = false;
            mapPatch = this.silkard$createPatch(render.buffer);
        } else {
            mapPatch = null;
        }

        Collection<MapDecoration> collection;

        if (this.tick++ % 5 == 0) {
            this.dirtyDecorations = false;
            Collection<MapDecoration> icons = new ArrayList<>();

            for (MapCursor cursor : render.cursors) {
                if (cursor.isVisible()) {
                    icons.add(new MapDecoration(CraftMapCursor.CraftType.bukkitToMinecraftHolder(cursor.getType()), cursor.getX(), cursor.getY(), cursor.getDirection(), CraftChatMessage.fromStringOrOptional(cursor.getCaption())));
                }
            }
            collection = icons;
        } else {
            collection = null;
        }

        if (collection == null && mapPatch == null) {
            return null;
        }
        return new ClientboundMapItemDataPacket(id, this.this$0.scale, this.this$0.locked, collection, mapPatch);
    }

    @Unique
    private MapItemSavedData.MapPatch silkard$createPatch(byte[] buffer) {
        int i = this.minDirtyX;
        int j = this.minDirtyY;
        int k = this.maxDirtyX + 1 - this.minDirtyX;
        int l = this.maxDirtyY + 1 - this.minDirtyY;
        byte[] abyte = new byte[k * l];

        for (int i1 = 0; i1 < k; ++i1) {
            for (int j1 = 0; j1 < l; ++j1) {
                abyte[i1 + j1 * k] = buffer[i + i1 + (j + j1) * 128];
            }
        }

        return new MapItemSavedData.MapPatch(i, j, k, l, abyte);
    }
    // CraftBukkit end
}
