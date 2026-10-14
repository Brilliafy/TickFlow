package com.cleanroommc.tickflow.trees;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TreeEventHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (!world.isRemote && ModConfig.DYNAMIC_TREES.enabled && ModConfig.DYNAMIC_TREES.enableTreeDormancy) {
            TreeDormancyManager.awakenArea(world, event.getPos(), ModConfig.DYNAMIC_TREES.awakenRadius);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        if (!world.isRemote && ModConfig.DYNAMIC_TREES.enabled && ModConfig.DYNAMIC_TREES.enableTreeDormancy) {
            BlockPos center = new BlockPos(event.getExplosion().getPosition());
            TreeDormancyManager.awakenArea(world, center, ModConfig.DYNAMIC_TREES.awakenRadius * 1.5);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            TreeDormancyManager.cleanup(event.world.getTotalWorldTime());
        }
    }
}
