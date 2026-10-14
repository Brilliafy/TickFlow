package com.cleanroommc.tickflow.mixin;

import com.cleanroommc.tickflow.dormancy.SpawnerDormancyManager;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityMobSpawner.class)
public abstract class TileEntityMobSpawnerMixin extends TileEntity implements ITickable {

    @Shadow
    public abstract MobSpawnerBaseLogic getSpawnerBaseLogic();

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void teDormancy$onSpawnerUpdate(CallbackInfo ci) {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        MobSpawnerBaseLogic logic = this.getSpawnerBaseLogic();
        int range = (logic instanceof MobSpawnerBaseLogicAccessor) ? ((MobSpawnerBaseLogicAccessor) logic).getActivatingRangeFromPlayer() : 16;
        if (SpawnerDormancyManager.isDormant(this.world, this.pos, range)) {
            ci.cancel();
        }
    }
}
