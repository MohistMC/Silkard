package com.mohistmc.silkard.mixin.vanilla.world.entity.boss.enderdragon;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import com.mohistmc.silkard.injected.world.entity.ContextLivingEntity;
import com.mohistmc.silkard.injected.world.level.ContextLevelAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(EnderDragon.class)
public abstract class MixinEnderDragon implements ContextLivingEntity {

    @Shadow private EnderDragonFight dragonFight;

    // CraftBukkit start - reusable source for CraftTNTPrimed.getSource()
    @Unique private Explosion silkard$explosionSource;
    @Unique private final List<org.bukkit.block.Block> silkard$destroyedBlocks = new ArrayList<>();
    // CraftBukkit end

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void silkard$initExplosionSource(EntityType<? extends EnderDragon> type, Level level, CallbackInfo ci) {
        this.silkard$explosionSource = new ServerExplosion(((ContextLevelAccessor) level).getMinecraftWorld(), (Entity) (Object) this, null, null, new Vec3(Double.NaN, Double.NaN, Double.NaN), Float.NaN, true, Explosion.BlockInteraction.DESTROY);
    }

    // CraftBukkit - don't move when hovering
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 silkard$hoverTarget(DragonPhaseInstance phase) {
        return phase.getPhase() == EnderDragonPhase.HOVERING ? null : phase.getFlyTargetLocation();
    }

    // CraftBukkit start - crystal heal event
    @Redirect(method = "checkCrystals", at = @At(value = "INVOKE", target = "setHealth(F)V"))
    private void silkard$crystalHeal(EnderDragon self, float health) {
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(self.getBukkitEntity(), 1.0F, EntityRegainHealthEvent.RegainReason.ENDER_CRYSTAL);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            self.setHealth((float) (self.getHealth() + event.getAmount()));
        }
    }
    // CraftBukkit end

    // CraftBukkit start - collect blocks and fire EntityExplodeEvent instead of removing them directly
    @Inject(method = "checkWalls", at = @At("HEAD"))
    private void silkard$clearDestroyed(ServerLevel level, AABB aabb, CallbackInfoReturnable<Boolean> cir) {
        this.silkard$destroyedBlocks.clear();
    }

    @Redirect(method = "checkWalls", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean silkard$collectDestroyed(ServerLevel level, BlockPos pos, boolean flag) {
        this.silkard$destroyedBlocks.add(CraftBlock.at(level, pos));
        return true;
    }

    @Redirect(method = "checkWalls", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void silkard$explodeDestroyed(ServerLevel level, int eventId, BlockPos pos, int data) {
        if (this.silkard$processDestroyed(level)) {
            level.levelEvent(eventId, pos, data);
        }
    }

    @Unique
    private boolean silkard$processDestroyed(ServerLevel level) {
        if (this.silkard$destroyedBlocks.isEmpty()) {
            return true;
        }
        EntityExplodeEvent event = CraftEventFactory.callEntityExplodeEvent((Entity) (Object) this, this.silkard$destroyedBlocks, 0F, this.silkard$explosionSource.getBlockInteraction());
        if (event.isCancelled()) {
            return false;
        }
        if (event.getYield() == 0F) {
            for (org.bukkit.block.Block block : event.blockList()) {
                level.removeBlock(new BlockPos(block.getX(), block.getY(), block.getZ()), false);
            }
        } else {
            for (org.bukkit.block.Block block : event.blockList()) {
                if (block.getType().isAir()) {
                    continue;
                }
                CraftBlock craftBlock = (CraftBlock) block;
                BlockPos blockpos = craftBlock.getPosition();
                BlockState blockstate = craftBlock.getNMS();
                Block nmsBlock = blockstate.getBlock();
                if (nmsBlock.dropFromExplosion(this.silkard$explosionSource)) {
                    BlockEntity blockentity = blockstate.hasBlockEntity() ? level.getBlockEntity(blockpos) : null;
                    LootParams.Builder builder = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0F / event.getYield()).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity);
                    blockstate.getDrops(builder).forEach(stack -> Block.popResource(level, blockpos, stack));
                    blockstate.spawnAfterBreak(level, blockpos, ItemStack.EMPTY, false);
                }
                nmsBlock.wasExploded(level, blockpos, this.silkard$explosionSource);
                level.removeBlock(blockpos, false);
            }
        }
        return true;
    }
    // CraftBukkit end

    // CraftBukkit - add Bukkit remove cause
    @Redirect(method = "kill", at = @At(value = "INVOKE", target = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void silkard$killRemove(EnderDragon self, Entity.RemovalReason reason) {
        ((ContextEntity) self).remove(reason, EntityRemoveEvent.Cause.DEATH);
    }

    // CraftBukkit start - SPIGOT-2420: special xp reward
    @Override
    public int getExpReward(ServerLevel level, Entity entity) {
        boolean flag = level.getGameRules().get(GameRules.MOB_DROPS);
        short exp;
        if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
            exp = 12000;
        } else {
            exp = 500;
        }
        return flag ? exp : 0;
    }

    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object silkard$mobDrops(GameRules rules, GameRule<?> rule) {
        return Boolean.TRUE;
    }

    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V", ordinal = 0))
    private void silkard$awardFirst(ServerLevel level, Vec3 pos, int amount) {
        ExperienceOrb.award(level, pos, Mth.floor((float) ((ContextLivingEntity) (Object) this).silkard$expToDrop() * 0.08F));
    }

    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V", ordinal = 1))
    private void silkard$awardSecond(ServerLevel level, Vec3 pos, int amount) {
        ExperienceOrb.award(level, pos, Mth.floor((float) ((ContextLivingEntity) (Object) this).silkard$expToDrop() * 0.2F));
    }

    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void silkard$tickDeathRemove(EnderDragon self, Entity.RemovalReason reason) {
        ((ContextEntity) self).remove(reason, EntityRemoveEvent.Cause.DEATH);
    }
    // CraftBukkit end

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void silkard$saveExpToDrop(ValueOutput output, CallbackInfo ci) {
        output.putInt("Bukkit.expToDrop", ((ContextLivingEntity) (Object) this).silkard$expToDrop());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void silkard$loadExpToDrop(ValueInput input, CallbackInfo ci) {
        ((ContextLivingEntity) (Object) this).silkard$expToDrop(input.getIntOr("Bukkit.expToDrop", ((ContextLivingEntity) (Object) this).silkard$expToDrop()));
    }
}
