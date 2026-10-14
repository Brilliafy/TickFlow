package com.cleanroommc.tickflow.path;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.EntityLiving;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PathBackoffTracker {

    public static final AtomicLong TOTAL_REQUESTS = new AtomicLong(0);
    public static final AtomicLong TOTAL_SUPPRESSED = new AtomicLong(0);
    public static final AtomicLong TOTAL_SWARM_HITS = new AtomicLong(0);

    public static class BackoffRecord {
        public int failureCount;
        public long lastFailureTime;
        public long backoffUntilTime;
        public double lastTargetX;
        public double lastTargetY;
        public double lastTargetZ;

        public BackoffRecord(long now, double tx, double ty, double tz) {
            this.failureCount = 1;
            this.lastFailureTime = now;
            this.backoffUntilTime = now + ModConfig.PATHFINDING.baseBackoffTicks;
            this.lastTargetX = tx;
            this.lastTargetY = ty;
            this.lastTargetZ = tz;
        }

        public void recordFailure(long now, double tx, double ty, double tz) {
            this.failureCount++;
            int shift = Math.min(this.failureCount - 1, 5);
            long delay = Math.min((long) ModConfig.PATHFINDING.maxBackoffTicks, (long) ModConfig.PATHFINDING.baseBackoffTicks * (1L << shift));
            this.lastFailureTime = now;
            this.backoffUntilTime = now + delay;
            this.lastTargetX = tx;
            this.lastTargetY = ty;
            this.lastTargetZ = tz;
        }

        public boolean targetMoved(double tx, double ty, double tz) {
            double dx = tx - this.lastTargetX;
            double dy = ty - this.lastTargetY;
            double dz = tz - this.lastTargetZ;
            return (dx * dx + dy * dy + dz * dz) >= ModConfig.PATHFINDING.targetMoveResetDistSq;
        }
    }

    private static final Map<Integer, BackoffRecord> BACKOFF_MAP = new ConcurrentHashMap<>();
    private static long lastCleanupTime = 0;

    public static boolean shouldSuppress(EntityLiving entity, double targetX, double targetY, double targetZ) {
        if (!ModConfig.PATHFINDING.enabled || !ModConfig.PATHFINDING.enableBackoff) {
            return false;
        }

        TOTAL_REQUESTS.incrementAndGet();

        if (BossChecker.isBoss(entity)) {
            return false;
        }

        int id = entity.getEntityId();
        BackoffRecord record = BACKOFF_MAP.get(id);
        if (record == null) {
            return false;
        }

        long now = entity.world.getTotalWorldTime();
        if (now >= record.backoffUntilTime) {
            return false;
        }

        if (record.targetMoved(targetX, targetY, targetZ)) {
            BACKOFF_MAP.remove(id);
            return false;
        }

        TOTAL_SUPPRESSED.incrementAndGet();
        return true;
    }

    public static void recordFailure(EntityLiving entity, double targetX, double targetY, double targetZ) {
        if (!ModConfig.PATHFINDING.enabled || !ModConfig.PATHFINDING.enableBackoff || BossChecker.isBoss(entity)) {
            return;
        }

        int id = entity.getEntityId();
        long now = entity.world.getTotalWorldTime();
        BACKOFF_MAP.compute(id, (k, old) -> {
            if (old == null) {
                return new BackoffRecord(now, targetX, targetY, targetZ);
            } else {
                old.recordFailure(now, targetX, targetY, targetZ);
                return old;
            }
        });
    }

    public static void recordSuccess(EntityLiving entity) {
        if (entity != null) {
            BACKOFF_MAP.remove(entity.getEntityId());
        }
    }

    public static void resetEntity(int entityId) {
        BACKOFF_MAP.remove(entityId);
    }

    public static void cleanup(long currentTime) {
        if (currentTime - lastCleanupTime < 200) {
            return;
        }
        lastCleanupTime = currentTime;
        Iterator<Map.Entry<Integer, BackoffRecord>> it = BACKOFF_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BackoffRecord> entry = it.next();
            if (currentTime - entry.getValue().lastFailureTime > 600) {
                it.remove();
            }
        }
    }

    public static void clearAll() {
        BACKOFF_MAP.clear();
        TOTAL_REQUESTS.set(0);
        TOTAL_SUPPRESSED.set(0);
        TOTAL_SWARM_HITS.set(0);
    }

    public static int getActiveBackoffCount() {
        return BACKOFF_MAP.size();
    }
}
