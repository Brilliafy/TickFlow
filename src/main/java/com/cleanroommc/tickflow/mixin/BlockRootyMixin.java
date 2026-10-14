package com.cleanroommc.tickflow.mixin;

import com.cleanroommc.tickflow.trees.TreeDormancyManager;
import com.ferreusveritas.dynamictrees.blocks.BlockRooty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(BlockRooty.class)
public abstract class BlockRootyMixin {

    @Inject(method = "updateTree", at = @At("HEAD"), cancellable = true, remap = false)
    private void dtDormancy$onUpdateTree(IBlockState rootyState, World world, BlockPos rootPos, Random random, boolean natural, CallbackInfo ci) {
        if (TreeDormancyManager.isDormant(world, rootPos, rootyState)) {
            ci.cancel();
        }
    }

    @Inject(method = "setSoilLife", at = @At("HEAD"), remap = false)
    private void dtDormancy$onSetSoilLife(World world, BlockPos rootPos, int life, CallbackInfo ci) {
        TreeDormancyManager.awaken(world, rootPos);
    }

    @Inject(method = "fertilize", at = @At("HEAD"), remap = false)
    private void dtDormancy$onFertilize(World world, BlockPos pos, int amount, CallbackInfoReturnable<Boolean> cir) {
        TreeDormancyManager.awaken(world, pos);
    }
}
