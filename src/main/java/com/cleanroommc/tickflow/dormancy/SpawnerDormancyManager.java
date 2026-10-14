package com.cleanroommc.tickflow.dormancy;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SpawnerDormancyManager {

    public static final AtomicLong SPAWNER_TICKS_SAVED = new AtomicLong(0);
    public static final AtomicLong SPAWNER_TICKS_EXECUTED = new AtomicLong(0);

    // Map: (dimension ^ pos.toLong()) -> wakeTick
    private static final Map<Long, Long> SPAWNER_WAKE_MAP = new ConcurrentHashMap<>();
    private static long lastCleanupTime = 0;

    private static long getKey(int dim, BlockPos pos) {
        return pos.toLong() ^ (((long) dim) << 48);
    }

    public static boolean isDormant(World world, BlockPos pos, int activatingRange) {
        if (!ModConfig.DORMANCY.enabled || !ModConfig.DORMANCY.enableSpawnerDormancy || world == null || world.isRemote || pos == null) {
            SPAWNER_TICKS_EXECUTED.incrementAndGet();
            return false;
        }

        int dim = world.provider.getDimension();
        long key = getKey(dim, pos);
        long now = world.getTotalWorldTime();

        Long wakeTick = SPAWNER_WAKE_MAP.get(key);
        if (wakeTick != null && now < wakeTick) {
            SPAWNER_TICKS_SAVED.incrementAndGet();
            return true;
        }

        List<EntityPlayer> players = world.playerEntities;
        if (players == null || players.isEmpty()) {
            SPAWNER_WAKE_MAP.put(key, now + ModConfig.DORMANCY.spawnerMaxSleepTicks);
            SPAWNER_TICKS_SAVED.incrementAndGet();
            return true;
        }

        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;

        double minDistanceSq = Double.MAX_VALUE;
        int pCount = players.size();
        for (int i = 0; i < pCount; i++) {
            EntityPlayer player = players.get(i);
            if (player != null && !player.isDead) {
                double dx = player.posX - px;
                double dy = player.posY - py;
                double dz = player.posZ - pz;
                double dSq = dx * dx + dy * dy + dz * dz;
                if (dSq < minDistanceSq) {
                    minDistanceSq = dSq;
                }
            }
        }

        double threshold = (double) activatingRange + ModConfig.DORMANCY.spawnerSafetyBufferBlocks;
        double thresholdSq = threshold * threshold;

        if (minDistanceSq <= thresholdSq) {
            // Player is close: tick normally every tick!
            SPAWNER_WAKE_MAP.remove(key);
            SPAWNER_TICKS_EXECUTED.incrementAndGet();
            return false;
        }

        // Player is far: sleep based on distance
        double dist = Math.sqrt(minDistanceSq);
        double excess = dist - threshold;
        int sleepTicks = Math.min(ModConfig.DORMANCY.spawnerMaxSleepTicks, Math.max(2, (int) (excess / 1.5D)));

        SPAWNER_WAKE_MAP.put(key, now + sleepTicks);
        SPAWNER_TICKS_SAVED.incrementAndGet();
        return true;
    }

    public static void awaken(World world, BlockPos pos) {
        if (world != null && pos != null) {
            SPAWNER_WAKE_MAP.remove(getKey(world.provider.getDimension(), pos));
        }
    }

    public static void cleanup(long now) {
        if (now - lastCleanupTime < 1200) {
            return;
        }
        lastCleanupTime = now;
        Iterator<Map.Entry<Long, Long>> it = SPAWNER_WAKE_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> entry = it.next();
            if (now - entry.getValue() > 1200) {
                it.remove();
            }
        }
    }

    public static void clearAll() {
        SPAWNER_WAKE_MAP.clear();
        SPAWNER_TICKS_SAVED.set(0);
        SPAWNER_TICKS_EXECUTED.set(0);
    }
}
