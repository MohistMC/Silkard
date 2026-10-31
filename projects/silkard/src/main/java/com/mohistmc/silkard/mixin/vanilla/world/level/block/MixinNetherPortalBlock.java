package com.mohistmc.silkard.mixin.vanilla.world.level.block;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextEntityType;
import com.mohistmc.silkard.injected.world.level.portal.ContextPortalForcer;
import com.mohistmc.silkard.injected.world.level.portal.ContextTeleportTransition;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.event.CraftPortalEvent;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/24
 */
@Mixin(NetherPortalBlock.class)
public abstract class MixinNetherPortalBlock {

    @Unique
    private int silkard$searchRadius;
    @Unique
    private int silkard$createRadius;
    @Unique
    private boolean silkard$canCreatePortal;

    @Shadow
    private static TeleportTransition getDimensionTransitionFromExit(Entity entity, BlockPos portalEntryPos, BlockUtil.FoundRectangle rectangle, ServerLevel newLevel, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        throw new AssertionError();
    }

    // CraftBukkit start - EntityPortalEnterEvent
    @Inject(method = "entityInside", at = @At("HEAD"))
    protected void silkard$portalEnter(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise, CallbackInfo ci) {
        if (entity.canUsePortal(false)) {
            EntityPortalEnterEvent event = new EntityPortalEnterEvent(entity.getBukkitEntity(), new Location(level.getWorld(), pos.getX(), pos.getY(), pos.getZ()));
            level.getCraftServer().getPluginManager().callEvent(event);
        }
    }
    // CraftBukkit end

    // CraftBukkit - set spawn reason to NETHER_PORTAL
    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    private Entity silkard$zombifiedPiglin(EntityType<?> type, ServerLevel level, BlockPos pos, EntitySpawnReason reason) {
        return ((ContextEntityType<?>) type).spawn(level, pos, reason, CreatureSpawnEvent.SpawnReason.NETHER_PORTAL);
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - custom dimensions and EntityPortalEvent
     */
    @Overwrite
    public TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        ResourceKey<Level> resourcekey = currentLevel.getTypeKey() == LevelStem.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel serverlevel1 = currentLevel.getServer().getLevel(resourcekey);
        if (serverlevel1 == null) {
            TeleportTransition transition = new TeleportTransition(null, Vec3.ZERO, Vec3.ZERO, 0.0F, 0.0F, Set.of(), TeleportTransition.DO_NOTHING);
            ((ContextTeleportTransition) (Object) transition).silkard$cause(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
            return transition;
        }
        boolean flag = serverlevel1.getTypeKey() == LevelStem.NETHER;
        WorldBorder worldborder = serverlevel1.getWorldBorder();
        double scale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), serverlevel1.dimensionType());
        BlockPos blockpos1 = worldborder.clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);
        CraftPortalEvent event = ((ContextEntity) entity).callPortalEvent(entity, CraftLocation.toBukkit(blockpos1, serverlevel1.getWorld()), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, flag ? 16 : 128, 16);
        if (event == null) {
            return null;
        }
        Location to = event.getTo();
        serverlevel1 = ((CraftWorld) to.getWorld()).getHandle();
        worldborder = serverlevel1.getWorldBorder();
        blockpos1 = worldborder.clampToBounds(to.getX(), to.getY(), to.getZ());
        this.silkard$searchRadius = event.getSearchRadius();
        this.silkard$canCreatePortal = event.getCanCreatePortal();
        this.silkard$createRadius = event.getCreationRadius();
        return this.getExitPortal(serverlevel1, entity, portalEntryPos, blockpos1, flag, worldborder);
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - configurable search radius and portal creation
     */
    @Overwrite
    private TeleportTransition getExitPortal(ServerLevel newLevel, Entity entity, BlockPos portalEntryPos, BlockPos approximateExitPos, boolean toNether, WorldBorder worldBorder) {
        PortalForcer portalForcer = newLevel.getPortalForcer();
        ((ContextPortalForcer) portalForcer).pushSearchRadius(this.silkard$searchRadius);
        Optional<BlockPos> optional = portalForcer.findClosestPortalPosition(approximateExitPos, toNether, worldBorder);
        BlockUtil.FoundRectangle rectangle;
        TeleportTransition.PostTeleportTransition postTeleportTransition;
        if (optional.isPresent()) {
            BlockPos blockpos2 = optional.get();
            BlockState blockstate = newLevel.getBlockState(blockpos2);
            rectangle = BlockUtil.getLargestRectangleAround(blockpos2, blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, pos -> newLevel.getBlockState(pos) == blockstate);
            postTeleportTransition = TeleportTransition.PLAY_PORTAL_SOUND.then(entity1 -> entity1.placePortalTicket(blockpos2));
        } else if (this.silkard$canCreatePortal && !entity.isSpectator()) {
            Direction.Axis axis = entity.level().getBlockState(portalEntryPos).getOptionalValue(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
            ((ContextPortalForcer) portalForcer).pushPortalCreate(entity, this.silkard$createRadius);
            Optional<BlockUtil.FoundRectangle> optional1 = portalForcer.createPortal(approximateExitPos, axis);
            if (optional1.isEmpty()) {
                return null;
            }
            rectangle = optional1.get();
            postTeleportTransition = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        } else {
            return null;
        }
        return getDimensionTransitionFromExit(entity, portalEntryPos, rectangle, newLevel, postTeleportTransition);
    }

    // CraftBukkit - carry the teleport cause
    @Inject(method = "getDimensionTransitionFromExit", at = @At("RETURN"))
    private static void silkard$portalCause(Entity entity, BlockPos portalEntryPos, BlockUtil.FoundRectangle rectangle, ServerLevel newLevel, TeleportTransition.PostTeleportTransition postTeleportTransition, CallbackInfoReturnable<TeleportTransition> cir) {
        TeleportTransition transition = cir.getReturnValue();
        if (transition != null) {
            ((ContextTeleportTransition) (Object) transition).silkard$cause(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
        }
    }
}
