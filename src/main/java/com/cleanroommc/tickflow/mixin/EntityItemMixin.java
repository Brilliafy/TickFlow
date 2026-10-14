package com.cleanroommc.tickflow.mixin;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import net.minecraft.item.ItemStack;

@Mixin(EntityItem.class)
public abstract class EntityItemMixin extends Entity {

    public EntityItemMixin(World worldIn) {
        super(worldIn);
    }

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private boolean combineItems(EntityItem other) {
        return false;
    }

    @Inject(method = "searchForOtherItemsNearby", at = @At("HEAD"), cancellable = true)
    private void lootStacker$onSearchForOtherItemsNearby(CallbackInfo ci) {
        if (ci.isCancelled() || !ModConfig.ITEM_STACKING.enabled || this.world.isRemote || !this.isEntityAlive()) {
            return;
        }

        ItemStack currentStack = this.getItem();
        if (currentStack.isEmpty() || currentStack.getCount() >= currentStack.getMaxStackSize()) {
            ci.cancel();
            return;
        }

        double rx = ModConfig.ITEM_STACKING.mergeRadiusHorizontal;
        double ry = ModConfig.ITEM_STACKING.mergeRadiusVertical;
        double rz = ModConfig.ITEM_STACKING.mergeRadiusHorizontal;

        AxisAlignedBB box = this.getEntityBoundingBox().grow(rx, ry, rz);
        List<EntityItem> nearby = this.world.getEntitiesWithinAABB(EntityItem.class, box);

        for (EntityItem other : nearby) {
            if (other != (Object) this && other.isEntityAlive()) {
                this.combineItems(other);
                if (!this.isEntityAlive()) {
                    break;
                }
            }
        }

        ci.cancel();
    }
}
