package com.cleanroommc.tickflow;

import com.cleanroommc.tickflow.command.CommandTickFlow;
import com.cleanroommc.tickflow.config.ModConfig;
import com.cleanroommc.tickflow.dormancy.ChestDormancyManager;
import com.cleanroommc.tickflow.dormancy.SpawnerDormancyManager;
import com.cleanroommc.tickflow.path.PathEventHandler;
import com.cleanroommc.tickflow.stacker.ItemSpawnEventHandler;
import com.cleanroommc.tickflow.trees.TreeEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tick Flow - server-side simulation optimizations for Cleanroom 1.12.2:
 *
 * <ul>
 *   <li><b>Pathfinding</b> - exponential failure backoff plus clustered swarm path sharing.</li>
 *   <li><b>Item Stacking</b> - spatial merging of nearby dropped item entities.</li>
 *   <li><b>Tile Entity Dormancy</b> - proximity-based dormancy for mob spawners and idle chests.</li>
 *   <li><b>Dynamic Trees</b> - dormancy for mature DynamicTrees (only active when DynamicTrees is loaded).</li>
 *   <li><b>Collision</b> - allocation-light voxel collision path for World#getCollisionBoxes.</li>
 * </ul>
 */
@Mod(
        modid = TickFlow.MOD_ID,
        name = TickFlow.MOD_NAME,
        version = TickFlow.VERSION,
        dependencies = "after:dynamictrees",
        acceptableRemoteVersions = "*"
)
public class TickFlow {

    public static final String MOD_ID = "tickflow";
    public static final String MOD_NAME = "Tick Flow";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.Instance(MOD_ID)
    public static TickFlow instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);

        MinecraftForge.EVENT_BUS.register(new PathEventHandler());
        MinecraftForge.EVENT_BUS.register(new ItemSpawnEventHandler());
        MinecraftForge.EVENT_BUS.register(this);

        if (Loader.isModLoaded("dynamictrees")) {
            MinecraftForge.EVENT_BUS.register(new TreeEventHandler());
            LOGGER.info("[TickFlow] DynamicTrees detected, dormancy optimization engine enabled.");
        } else {
            LOGGER.info("[TickFlow] DynamicTrees not detected, dormancy engine idle.");
        }

        LOGGER.info("[TickFlow] Initialized path backoff, swarm caching, spatial item stacking, " +
                "tile entity dormancy and fast voxel collision systems.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTickFlow());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            long now = event.world.getTotalWorldTime();
            SpawnerDormancyManager.cleanup(now);
            ChestDormancyManager.cleanup(now);
        }
    }
}
