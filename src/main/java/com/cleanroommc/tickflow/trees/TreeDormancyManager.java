package com.cleanroommc.tickflow.trees;

import com.cleanroommc.tickflow.config.ModConfig;
import com.ferreusveritas.dynamictrees.blocks.BlockRooty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TreeDormancyManager {

    public static final AtomicLong TICKS_SAVED = new AtomicLong(0);
    public static final AtomicLong AWAKEN_COUNT = new AtomicLong(0);

    public static class DormancyRecord {
        public long dormantSinceTick;
        public final int dimensionId;

        public DormancyRecord(long tick, int dim) {
            this.dormantSinceTick = tick;
            this.dimensionId = dim;
        }
    }

    private static final Map<Long, DormancyRecord> DORMANT_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, Set<Long>> CHUNK_TO_ROOTS = new ConcurrentHashMap<>();
    private static long lastCleanupTime = 0;

    private static long getChunkKey(int dim, int chunkX, int chunkZ) {
        return (((long) dim) << 48) | (ChunkPos.asLong(chunkX, chunkZ) & 0x0000FFFFFFFFFFFFL);
    }

    private static long getRootKey(int dim, BlockPos pos) {
        return pos.toLong() ^ (((long) dim) << 48);
    }

    public static boolean isDormant(World world, BlockPos rootPos, IBlockState rootyState) {
        if (!ModConfig.DYNAMIC_TREES.enabled || !ModConfig.DYNAMIC_TREES.enableTreeDormancy || world.isRemote) {
            return false;
        }

        if (!(rootyState.getBlock() instanceof BlockRooty)) {
            return false;
        }

        int dim = world.provider.getDimension();
        BlockRooty blockRooty = (BlockRooty) rootyState.getBlock();
        int life = blockRooty.getSoilLife(rootyState, world, rootPos);
        if (life > 0) {
            // Tree is fertile and actively growing
            removeTree(dim, rootPos);
            return false;
        }

        long key = getRootKey(dim, rootPos);
        long now = world.getTotalWorldTime();
        DormancyRecord record = DORMANT_MAP.get(key);

        if (record != null) {
            if (now - record.dormantSinceTick < ModConfig.DYNAMIC_TREES.dormancyRefreshTicks) {
                TICKS_SAVED.incrementAndGet();
                return true;
            } else {
                // Periodic refresh
                record.dormantSinceTick = now;
                return false;
            }
        }

        // New mature tree detected
        DORMANT_MAP.put(key, new DormancyRecord(now, dim));
        long chunkKey = getChunkKey(dim, rootPos.getX() >> 4, rootPos.getZ() >> 4);
        CHUNK_TO_ROOTS.computeIfAbsent(chunkKey, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(key);

        TICKS_SAVED.incrementAndGet();
        return true;
    }

    private static void removeTree(int dim, BlockPos rootPos) {
        long key = getRootKey(dim, rootPos);
        if (DORMANT_MAP.remove(key) != null) {
            long chunkKey = getChunkKey(dim, rootPos.getX() >> 4, rootPos.getZ() >> 4);
            Set<Long> set = CHUNK_TO_ROOTS.get(chunkKey);
            if (set != null) {
                set.remove(key);
                if (set.isEmpty()) {
                    CHUNK_TO_ROOTS.remove(chunkKey);
                }
            }
            AWAKEN_COUNT.incrementAndGet();
        }
    }

    public static void awaken(World world, BlockPos rootPos) {
        if (world != null && !world.isRemote && rootPos != null) {
            removeTree(world.provider.getDimension(), rootPos);
        }
    }

    public static void awakenArea(World world, BlockPos centerPos, double radius) {
        if (world == null || world.isRemote || centerPos == null) {
            return;
        }

        int dim = world.provider.getDimension();
        double radiusSq = radius * radius;
        int cx = centerPos.getX();
        int cy = centerPos.getY();
        int cz = centerPos.getZ();

        int minChunkX = (cx - (int) radius) >> 4;
        int maxChunkX = (cx + (int) radius) >> 4;
        int minChunkZ = (cz - (int) radius) >> 4;
        int maxChunkZ = (cz + (int) radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(dim, chunkX, chunkZ);
                Set<Long> set = CHUNK_TO_ROOTS.get(chunkKey);
                if (set != null && !set.isEmpty()) {
                    List<Long> toRemove = new ArrayList<>();
                    for (Long rootKey : set) {
                        BlockPos pos = BlockPos.fromLong(rootKey ^ (((long) dim) << 48));
                        double dx = pos.getX() - cx;
                        double dy = pos.getY() - cy;
                        double dz = pos.getZ() - cz;
                        if ((dx * dx + dy * dy + dz * dz) <= radiusSq) {
                            toRemove.add(rootKey);
                        }
                    }
                    for (Long rootKey : toRemove) {
                        set.remove(rootKey);
                        DORMANT_MAP.remove(rootKey);
                        AWAKEN_COUNT.incrementAndGet();
                    }
                    if (set.isEmpty()) {
                        CHUNK_TO_ROOTS.remove(chunkKey);
                    }
                }
            }
        }
    }

    public static void cleanup(long now) {
        if (now - lastCleanupTime < 2400) {
            return;
        }
        lastCleanupTime = now;
        Iterator<Map.Entry<Long, DormancyRecord>> it = DORMANT_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, DormancyRecord> entry = it.next();
            if (now - entry.getValue().dormantSinceTick > 72000L) { // Stale after 1 hour
                long rootKey = entry.getKey();
                it.remove();
                BlockPos pos = BlockPos.fromLong(rootKey ^ (((long) entry.getValue().dimensionId) << 48));
                long chunkKey = getChunkKey(entry.getValue().dimensionId, pos.getX() >> 4, pos.getZ() >> 4);
                Set<Long> set = CHUNK_TO_ROOTS.get(chunkKey);
                if (set != null) {
                    set.remove(rootKey);
                    if (set.isEmpty()) {
                        CHUNK_TO_ROOTS.remove(chunkKey);
                    }
                }
            }
        }
    }

    public static int getDormantTreeCount() {
        return DORMANT_MAP.size();
    }

    public static void clearAll() {
        DORMANT_MAP.clear();
        CHUNK_TO_ROOTS.clear();
        TICKS_SAVED.set(0);
        AWAKEN_COUNT.set(0);
    }
}
