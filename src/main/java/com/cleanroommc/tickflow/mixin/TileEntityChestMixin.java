package com.cleanroommc.tickflow.mixin;

import com.cleanroommc.tickflow.dormancy.ChestDormancyManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityChest.class)
public abstract class TileEntityChestMixin extends TileEntity implements ITickable {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void teDormancy$onChestUpdate(CallbackInfo ci) {
        TileEntityChest chest = (TileEntityChest) (Object) this;
        if (ChestDormancyManager.isDormant(chest)) {
            ci.cancel();
        }
    }
}
