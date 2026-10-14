package com.cleanroommc.tickflow.mixin;

import com.cleanroommc.tickflow.collision.FastCollisionEngine;
import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(World.class)
public abstract class WorldCollisionMixin {

    @Inject(method = "getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;ZLjava/util/List;)Z", at = @At("HEAD"), cancellable = true)
    private void voxelCollision$onGetCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb, boolean p_191504_3_, @Nullable List<AxisAlignedBB> outList, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.COLLISION.enabled) {
            return;
        }

        World world = (World) (Object) this;
        boolean collided = FastCollisionEngine.getCollisionBoxesFast(world, entityIn, aabb, p_191504_3_, outList);
        cir.setReturnValue(collided);
    }
}
