package com.cleanroommc.tickflow.stacker;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class StackerManager {

    public static final AtomicLong TOTAL_ITEMS_SPAWNED = new AtomicLong(0);
    public static final AtomicLong TOTAL_ITEMS_MERGED = new AtomicLong(0);
    public static final AtomicLong TOTAL_ENTITIES_SAVED = new AtomicLong(0);

    public static boolean areItemsCompatible(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.getItem() != b.getItem()) {
            return false;
        }
        if (a.getMetadata() != b.getMetadata()) {
            return false;
        }
        if (!ItemStack.areItemStackTagsEqual(a, b)) {
            return false;
        }
        if (!a.areCapsCompatible(b)) {
            return false;
        }
        return true;
    }

    public static boolean tryMerge(EntityItem existing, EntityItem incoming) {
        if (existing == null || incoming == null || existing == incoming) {
            return false;
        }
        if (!existing.isEntityAlive() || !incoming.isEntityAlive()) {
            return false;
        }

        ItemStack existingStack = existing.getItem();
        ItemStack incomingStack = incoming.getItem();

        if (!areItemsCompatible(existingStack, incomingStack)) {
            return false;
        }

        int maxStack = existingStack.getMaxStackSize();
        int space = maxStack - existingStack.getCount();
        if (space <= 0) {
            return false;
        }

        int toMove = Math.min(space, incomingStack.getCount());
        existingStack.grow(toMove);
        incomingStack.shrink(toMove);

        existing.setItem(existingStack);
        incoming.setItem(incomingStack);

        TOTAL_ITEMS_MERGED.addAndGet(toMove);

        // Smooth physics / velocity
        existing.motionX = (existing.motionX + incoming.motionX) * 0.5D;
        existing.motionY = (existing.motionY + incoming.motionY) * 0.5D;
        existing.motionZ = (existing.motionZ + incoming.motionZ) * 0.5D;

        if (incomingStack.isEmpty()) {
            incoming.setDead();
            TOTAL_ENTITIES_SAVED.incrementAndGet();
            return true;
        }

        return false;
    }

    public static boolean handleSpawnMerge(EntityItem item) {
        if (!ModConfig.ITEM_STACKING.enabled || !ModConfig.ITEM_STACKING.mergeOnSpawn || item.world.isRemote || !item.isEntityAlive()) {
            return false;
        }

        TOTAL_ITEMS_SPAWNED.incrementAndGet();

        World world = item.world;
        double rx = ModConfig.ITEM_STACKING.mergeRadiusHorizontal;
        double ry = ModConfig.ITEM_STACKING.mergeRadiusVertical;
        double rz = ModConfig.ITEM_STACKING.mergeRadiusHorizontal;

        AxisAlignedBB box = item.getEntityBoundingBox().grow(rx, ry, rz);
        List<EntityItem> nearby = world.getEntitiesWithinAABB(EntityItem.class, box);

        for (EntityItem other : nearby) {
            if (other != item && other.isEntityAlive()) {
                if (tryMerge(other, item)) {
                    // Fully absorbed!
                    return true;
                }
            }
        }

        return false;
    }

    public static int forceMergeInRadius(World world, AxisAlignedBB searchBox) {
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, searchBox);
        int mergedCount = 0;

        for (int i = 0; i < items.size(); i++) {
            EntityItem a = items.get(i);
            if (!a.isEntityAlive() || a.getItem().isEmpty()) {
                continue;
            }

            for (int j = i + 1; j < items.size(); j++) {
                EntityItem b = items.get(j);
                if (!b.isEntityAlive() || b.getItem().isEmpty()) {
                    continue;
                }

                if (tryMerge(a, b)) {
                    mergedCount++;
                }
            }
        }

        return mergedCount;
    }

    public static void clearStats() {
        TOTAL_ITEMS_SPAWNED.set(0);
        TOTAL_ITEMS_MERGED.set(0);
        TOTAL_ENTITIES_SAVED.set(0);
    }
}
