package com.mohistmc.silkard.mixin.vanilla.world.level;

import com.mohistmc.silkard.injected.world.entity.ContextEntity;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public class MixinServerExplosion {

    @Shadow private Vec3 center;
    @Shadow public float radius() {
        throw new AssertionError();
    }
    @Shadow public Explosion.BlockInteraction getBlockInteraction() {
        throw new AssertionError();
    }
    @Shadow public Entity getDirectSourceEntity() {
        throw new AssertionError();
    }
    @Unique private float silkard$yield;
    @Unique private boolean silkard$wasCanceled;
    @Unique private List<Entity> silkard$entityList;
    @Unique private double silkard$knockbackForce;

    public float silkard$getYield() {
        return this.silkard$yield;
    }

    public void silkard$setYield(float yield) {
        this.silkard$yield = yield;
    }

    public boolean silkard$wasCanceled() {
        return this.silkard$wasCanceled;
    }

    public void silkard$wasCanceled(boolean wasCanceled) {
        this.silkard$wasCanceled = wasCanceled;
    }

    // CraftBukkit start - clamp bad radius values
    @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/level/ServerExplosion;radius:F"))
    private float silkard$clampRadius(float value) {
        return Math.max(value, 0.0F);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void silkard$initYield(CallbackInfo ci) {
        this.silkard$yield = this.getBlockInteraction() == Explosion.BlockInteraction.DESTROY_WITH_DECAY ? 1.0F / this.radius() : 1.0F;
    }
    // CraftBukkit end

    // CraftBukkit start - don't explode if radius is too small
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void silkard$explodeGuard(CallbackInfoReturnable<Integer> cir) {
        if (this.radius() < 0.1F) {
            cir.setReturnValue(0);
        }
    }
    // CraftBukkit end

    // CraftBukkit start - capture entity list for EnderDragon handling
    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> silkard$captureEntities(ServerLevel instance, Entity entity, AABB aabb) {
        this.silkard$entityList = instance.getEntities(entity, aabb);
        return this.silkard$entityList;
    }
    // CraftBukkit end

    // CraftBukkit start - EnderDragon special case + lastDamageCancelled skip
    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean silkard$hurtServer(Entity entity, ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (entity instanceof EnderDragonPart) {
            return false;
        }
        ContextEntity ctx = (ContextEntity) entity;
        ctx.silkard$lastDamageCancelled(false);
        if (entity instanceof EnderDragon dragon) {
            for (EnderDragonPart part : dragon.subEntities) {
                if (this.silkard$entityList.contains(part)) {
                    part.hurtServer(level, source, amount);
                }
            }
            return false;
        }
        entity.hurtServer(level, source, amount);
        return !ctx.silkard$lastDamageCancelled();
    }
    // CraftBukkit end

    // CraftBukkit start - capture knockback force for EntityKnockbackEvent
    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 silkard$captureForce(Vec3 instance, double d) {
        this.silkard$knockbackForce = d;
        return instance.scale(d);
    }
    // CraftBukkit end

    // CraftBukkit start - EntityKnockbackEvent
    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pushFromExplosion(Lnet/minecraft/world/phys/Vec3;)V"))
    private void silkard$knockback(Entity entity, Vec3 vec32) {
        if (((ContextEntity) entity).silkard$lastDamageCancelled()) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            Vec3 result = entity.getDeltaMovement().add(vec32);
            EntityKnockbackEvent event = CraftEventFactory.callEntityKnockbackEvent(
                (CraftLivingEntity) living.getBukkitEntity(),
                this.getDirectSourceEntity(),
                EntityKnockbackEvent.KnockbackCause.EXPLOSION,
                this.silkard$knockbackForce, vec32,
                result.x, result.y, result.z);
            vec32 = event.isCancelled() ? Vec3.ZERO
                : new Vec3(event.getFinalKnockback().getX(), event.getFinalKnockback().getY(), event.getFinalKnockback().getZ()).subtract(entity.getDeltaMovement());
        }
        entity.pushFromExplosion(vec32);
    }
    // CraftBukkit end

    // CraftBukkit start - handleExplodeEvent + TNTPrimeEvent
    @Inject(method = "interactWithBlocks", at = @At("HEAD"), cancellable = true)
    private void silkard$blockExplodeEvent(List<BlockPos> targetBlocks, CallbackInfo ci) {
        List<org.bukkit.block.Block> bukkitBlocks = CraftEventFactory.handleExplodeEvent((ServerExplosion) (Object) this, targetBlocks);
        targetBlocks.clear();
        targetBlocks.addAll(bukkitBlocks.stream().map(b -> new BlockPos(b.getX(), b.getY(), b.getZ())).toList());
        if (this.silkard$wasCanceled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "interactWithBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V"))
    private void silkard$tntPrime(BlockState instance, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> callback) {
        Block block = instance.getBlock();
        if (block instanceof TntBlock) {
            Entity source = this.getDirectSourceEntity();
            BlockPos sourcePos = source == null ? BlockPos.containing(this.center) : null;
            if (!CraftEventFactory.callTNTPrimeEvent(level, pos, TNTPrimeEvent.PrimeCause.EXPLOSION, source, sourcePos)) {
                level.sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), instance, 3);
                return;
            }
        }
        instance.onExplosionHit(level, pos, explosion, callback);
    }
    // CraftBukkit end

    // CraftBukkit start - callBlockIgniteEvent
    @Redirect(method = "createFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean silkard$ignite(ServerLevel instance, BlockPos pos, BlockState state) {
        if (!CraftEventFactory.callBlockIgniteEvent(instance, pos, (ServerExplosion) (Object) this).isCancelled()) {
            return instance.setBlockAndUpdate(pos, state);
        }
        return false;
    }
    // CraftBukkit end

    // CraftBukkit start - SPIGOT-5425
    @Inject(method = "addOrAppendStack", at = @At("HEAD"), cancellable = true)
    private static void silkard$emptyStack(List<?> stacks, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        if (stack.isEmpty()) {
            ci.cancel();
        }
    }
    // CraftBukkit end
}
