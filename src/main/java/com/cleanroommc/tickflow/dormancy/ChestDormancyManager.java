package com.cleanroommc.tickflow.dormancy;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ChestDormancyManager {

    public static final AtomicLong CHEST_TICKS_SAVED = new AtomicLong(0);

    private static final Map<Long, Long> CHEST_WAKE_MAP = new ConcurrentHashMap<>();
    private static long lastCleanupTime = 0;

    private static long getKey(int dim, BlockPos pos) {
        return pos.toLong() ^ (((long) dim) << 48);
    }

    public static boolean isDormant(TileEntityChest chest) {
        if (!ModConfig.DORMANCY.enabled || !ModConfig.DORMANCY.enableChestDormancy) {
            return false;
        }

        World world = chest.getWorld();
        if (world == null || world.isRemote) {
            return false;
        }

        // Active animation or players using chest: never dormant!
        if (chest.numPlayersUsing > 0 || chest.lidAngle > 0.0F) {
            return false;
        }

        BlockPos pos = chest.getPos();
        int dim = world.provider.getDimension();
        long key = getKey(dim, pos);
        long now = world.getTotalWorldTime();

        Long wakeTick = CHEST_WAKE_MAP.get(key);
        if (wakeTick != null && now < wakeTick) {
            CHEST_TICKS_SAVED.incrementAndGet();
            return true;
        }

        List<EntityPlayer> players = world.playerEntities;
        if (players == null || players.isEmpty()) {
            CHEST_WAKE_MAP.put(key, now + ModConfig.DORMANCY.chestSleepTicks);
            CHEST_TICKS_SAVED.incrementAndGet();
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

        // Within 16 blocks: tick to allow opening animation
        if (minDistanceSq <= 256.0D) {
            CHEST_WAKE_MAP.remove(key);
            return false;
        }

        CHEST_WAKE_MAP.put(key, now + ModConfig.DORMANCY.chestSleepTicks);
        CHEST_TICKS_SAVED.incrementAndGet();
        return true;
    }

    public static void awaken(World world, BlockPos pos) {
        if (world != null && pos != null) {
            CHEST_WAKE_MAP.remove(getKey(world.provider.getDimension(), pos));
        }
    }

    public static void cleanup(long now) {
        if (now - lastCleanupTime < 1200) {
            return;
        }
        lastCleanupTime = now;
        Iterator<Map.Entry<Long, Long>> it = CHEST_WAKE_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> entry = it.next();
            if (now - entry.getValue() > 1200) {
                it.remove();
            }
        }
    }

    public static void clearAll() {
        CHEST_WAKE_MAP.clear();
        CHEST_TICKS_SAVED.set(0);
    }
}
