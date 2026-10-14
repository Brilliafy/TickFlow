package com.cleanroommc.tickflow.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "tickflow", name = "tickflow")
public class ModConfig {

    @Config.Name("Pathfinding")
    @Config.Comment("Pathfinding failure backoff and clustered swarm path sharing.")
    public static Pathfinding PATHFINDING = new Pathfinding();

    @Config.Name("Item Stacking")
    @Config.Comment("Spatial merging of nearby dropped item entities.")
    public static ItemStacking ITEM_STACKING = new ItemStacking();

    @Config.Name("Tile Entity Dormancy")
    @Config.Comment("Proximity-based dormancy for mob spawners and idle chests.")
    public static Dormancy DORMANCY = new Dormancy();

    @Config.Name("Dynamic Trees")
    @Config.Comment("Dormancy for mature DynamicTrees.")
    public static DynamicTrees DYNAMIC_TREES = new DynamicTrees();

    @Config.Name("Collision")
    @Config.Comment("Fast allocation-light voxel collision path.")
    public static Collision COLLISION = new Collision();

    public static class Pathfinding {
        @Config.Comment("Master toggle to enable/disable all pathfinding optimizations.")
        @Config.Name("Enable Mod")
        public boolean enabled = true;

        @Config.Comment("Enable exponential failure backoff for entities that cannot reach their target.")
        @Config.Name("Enable Failure Backoff")
        public boolean enableBackoff = true;

        @Config.Comment("Base ticks to wait before retrying after a pathfinding failure.")
        @Config.RangeInt(min = 5, max = 100)
        @Config.Name("Base Backoff Ticks")
        public int baseBackoffTicks = 10;

        @Config.Comment("Maximum ticks to back off after multiple consecutive pathfinding failures.")
        @Config.RangeInt(min = 20, max = 600)
        @Config.Name("Max Backoff Ticks")
        public int maxBackoffTicks = 140;

        @Config.Comment("Distance squared target must move before resetting backoff early.")
        @Config.RangeDouble(min = 1.0, max = 100.0)
        @Config.Name("Target Move Reset Distance Sq")
        public double targetMoveResetDistSq = 9.0;

        @Config.Comment("Enable clustered swarm path sharing among nearby mobs of the same type.")
        @Config.Name("Enable Swarm Path Sharing")
        public boolean enableSwarmCache = true;

        @Config.Comment("Swarm cluster radius for sharing paths (in blocks).")
        @Config.RangeDouble(min = 2.0, max = 16.0)
        @Config.Name("Swarm Cluster Radius")
        public double swarmClusterRadius = 6.0;

        @Config.Comment("How many ticks a shared swarm path remains valid.")
        @Config.RangeInt(min = 5, max = 100)
        @Config.Name("Swarm Cache Lifetime Ticks")
        public int swarmCacheLifetimeTicks = 20;

        @Config.Comment("Enable debug console logging for path statistics.")
        @Config.Name("Debug Logging")
        public boolean debugLogging = false;
    }

    public static class ItemStacking {
        @Config.Comment("Master toggle to enable/disable Spatial Loot Stacker.")
        @Config.Name("Enable Mod")
        public boolean enabled = true;

        @Config.Comment("Horizontal radius (in blocks) to search and aggressively merge matching dropped items.")
        @Config.RangeDouble(min = 0.5, max = 16.0)
        @Config.Name("Merge Radius Horizontal")
        public double mergeRadiusHorizontal = 3.5;

        @Config.Comment("Vertical radius (in blocks) to search and merge matching dropped items.")
        @Config.RangeDouble(min = 0.5, max = 8.0)
        @Config.Name("Merge Radius Vertical")
        public double mergeRadiusVertical = 2.0;

        @Config.Comment("Merge newly spawned items immediately on entity spawn before physics simulation starts.")
        @Config.Name("Merge On Spawn")
        public boolean mergeOnSpawn = true;

        @Config.Comment("Enable high-density mode: expand merge radius when item count in a chunk exceeds threshold.")
        @Config.Name("Enable Density Overflow Collapse")
        public boolean enableDensityCollapse = true;

        @Config.Comment("Item count threshold per chunk to trigger high-density collapse mode.")
        @Config.RangeInt(min = 10, max = 200)
        @Config.Name("Density Threshold")
        public int densityThreshold = 40;

        @Config.Comment("Enable debug console logging.")
        @Config.Name("Debug Logging")
        public boolean debugLogging = false;
    }

    public static class Dormancy {
        @Config.Comment("Master toggle for Tile Entity Dormancy.")
        @Config.Name("Enable Mod")
        public boolean enabled = true;

        @Config.Comment("Enable proximity-based dormancy for mob spawners.")
        @Config.Name("Enable Spawner Dormancy")
        public boolean enableSpawnerDormancy = true;

        @Config.Comment("Maximum ticks a mob spawner can sleep when far from players.")
        @Config.RangeInt(min = 1, max = 100)
        @Config.Name("Spawner Max Sleep Ticks")
        public int spawnerMaxSleepTicks = 20;

        @Config.Comment("Extra safety buffer distance (in blocks) beyond activating range before a spawner can sleep.")
        @Config.RangeDouble(min = 2.0, max = 32.0)
        @Config.Name("Spawner Safety Buffer Blocks")
        public double spawnerSafetyBufferBlocks = 8.0;

        @Config.Comment("Enable dormancy for idle chests with closed lids when no players are nearby.")
        @Config.Name("Enable Chest Dormancy")
        public boolean enableChestDormancy = true;

        @Config.Comment("Ticks an idle chest sleeps between lid/player checks.")
        @Config.RangeInt(min = 1, max = 80)
        @Config.Name("Chest Sleep Ticks")
        public int chestSleepTicks = 20;

        @Config.Comment("Print detailed dormancy statistics to logger.")
        @Config.Name("Debug Logging")
        public boolean debugLogging = false;
    }

    public static class DynamicTrees {
        @Config.Comment("Master toggle to enable/disable DynamicTrees dormancy optimization.")
        @Config.Name("Enable Mod")
        public boolean enabled = true;

        @Config.Comment("Mark mature trees with zero soil life as dormant to skip recursive growth analysis.")
        @Config.Name("Enable Mature Tree Dormancy")
        public boolean enableTreeDormancy = true;

        @Config.Comment("Ticks before a dormant tree performs an integrity check (default: 12000 = 10 minutes).")
        @Config.RangeInt(min = 1200, max = 72000)
        @Config.Name("Dormancy Refresh Ticks")
        public int dormancyRefreshTicks = 12000;

        @Config.Comment("Radius around broken blocks to awaken nearby dormant trees (in blocks).")
        @Config.RangeDouble(min = 1.0, max = 32.0)
        @Config.Name("Awaken Radius")
        public double awakenRadius = 16.0;

        @Config.Comment("Enable debug console logging.")
        @Config.Name("Debug Logging")
        public boolean debugLogging = false;
    }

    public static class Collision {
        @Config.Comment("Master toggle to enable/disable fast voxel collision engine.")
        @Config.Name("Enable Mod")
        public boolean enabled = true;

        @Config.Comment("Fast path for full cube blocks without dynamic collision box allocation.")
        @Config.Name("Full Cube Fast Path")
        public boolean enableFullCubeFastPath = true;

        @Config.Comment("Skip collision scanning in completely empty chunk sections.")
        @Config.Name("Skip Empty Sections")
        public boolean skipEmptySections = true;

        @Config.Comment("Direct chunk cache during multi-block bounding box iteration.")
        @Config.Name("Cache Chunk References")
        public boolean cacheChunkReferences = true;

        @Config.Comment("Enable debug console logging.")
        @Config.Name("Debug Logging")
        public boolean debugLogging = false;
    }

    @Mod.EventBusSubscriber(modid = "tickflow")
    public static class ConfigSyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("tickflow")) {
                ConfigManager.sync("tickflow", Config.Type.INSTANCE);
            }
        }
    }
}
