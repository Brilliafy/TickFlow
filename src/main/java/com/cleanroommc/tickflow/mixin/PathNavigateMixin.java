package com.cleanroommc.tickflow.mixin;

import com.cleanroommc.tickflow.path.PathBackoffTracker;
import com.cleanroommc.tickflow.path.SwarmPathCache;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigate.class)
public abstract class PathNavigateMixin {

    @Shadow
    protected EntityLiving entity;

    @Shadow
    protected World world;

    private static final ThreadLocal<Boolean> asyncPath$handledAtHead = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "getPathToEntityLiving(Lnet/minecraft/entity/Entity;)Lnet/minecraft/pathfinding/Path;", at = @At("HEAD"), cancellable = true)
    private void asyncPath$onGetPathToEntityHead(Entity entityIn, CallbackInfoReturnable<Path> cir) {
        asyncPath$handledAtHead.set(Boolean.FALSE);

        if (entityIn == null || this.entity == null || this.world == null || this.world.isRemote) {
            return;
        }

        if (PathBackoffTracker.shouldSuppress(this.entity, entityIn.posX, entityIn.posY, entityIn.posZ)) {
            asyncPath$handledAtHead.set(Boolean.TRUE);
            cir.setReturnValue(null);
            return;
        }

        Path cached = SwarmPathCache.tryGetCachedPath(this.entity, entityIn);
        if (cached != null) {
            asyncPath$handledAtHead.set(Boolean.TRUE);
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getPathToEntityLiving(Lnet/minecraft/entity/Entity;)Lnet/minecraft/pathfinding/Path;", at = @At("RETURN"))
    private void asyncPath$onGetPathToEntityReturn(Entity entityIn, CallbackInfoReturnable<Path> cir) {
        if (asyncPath$handledAtHead.get()) {
            asyncPath$handledAtHead.set(Boolean.FALSE);
            return;
        }

        if (entityIn == null || this.entity == null || this.world == null || this.world.isRemote) {
            return;
        }

        Path result = cir.getReturnValue();
        if (result == null) {
            PathBackoffTracker.recordFailure(this.entity, entityIn.posX, entityIn.posY, entityIn.posZ);
        } else {
            PathBackoffTracker.recordSuccess(this.entity);
            SwarmPathCache.putCachedPath(this.entity, entityIn, result);
        }
    }

    @Inject(method = "getPathToPos(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/pathfinding/Path;", at = @At("HEAD"), cancellable = true)
    private void asyncPath$onGetPathToPosHead(BlockPos pos, CallbackInfoReturnable<Path> cir) {
        asyncPath$handledAtHead.set(Boolean.FALSE);

        if (pos == null || this.entity == null || this.world == null || this.world.isRemote) {
            return;
        }

        if (PathBackoffTracker.shouldSuppress(this.entity, pos.getX(), pos.getY(), pos.getZ())) {
            asyncPath$handledAtHead.set(Boolean.TRUE);
            cir.setReturnValue(null);
            return;
        }

        Path cached = SwarmPathCache.tryGetCachedPathPos(this.entity, pos);
        if (cached != null) {
            asyncPath$handledAtHead.set(Boolean.TRUE);
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getPathToPos(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/pathfinding/Path;", at = @At("RETURN"))
    private void asyncPath$onGetPathToPosReturn(BlockPos pos, CallbackInfoReturnable<Path> cir) {
        if (asyncPath$handledAtHead.get()) {
            asyncPath$handledAtHead.set(Boolean.FALSE);
            return;
        }

        if (pos == null || this.entity == null || this.world == null || this.world.isRemote) {
            return;
        }

        Path result = cir.getReturnValue();
        if (result == null) {
            PathBackoffTracker.recordFailure(this.entity, pos.getX(), pos.getY(), pos.getZ());
        } else {
            PathBackoffTracker.recordSuccess(this.entity);
            SwarmPathCache.putCachedPathPos(this.entity, pos, result);
        }
    }
}
