package com.cleanroommc.tickflow.stacker;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ItemSpawnEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !ModConfig.ITEM_STACKING.enabled || !ModConfig.ITEM_STACKING.mergeOnSpawn) {
            return;
        }

        if (event.getEntity() instanceof EntityItem) {
            EntityItem item = (EntityItem) event.getEntity();
            if (StackerManager.handleSpawnMerge(item)) {
                event.setCanceled(true);
            }
        }
    }
}
