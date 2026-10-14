package com.cleanroommc.tickflow.path;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PathEventHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!event.getEntityLiving().world.isRemote) {
            PathBackoffTracker.resetEntity(event.getEntityLiving().getEntityId());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntityLiving().world.isRemote) {
            PathBackoffTracker.resetEntity(event.getEntityLiving().getEntityId());
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            long time = event.world.getTotalWorldTime();
            PathBackoffTracker.cleanup(time);
            SwarmPathCache.cleanup(time);
        }
    }
}
