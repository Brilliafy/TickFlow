package com.cleanroommc.tickflow.path;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SwarmPathCache {

    public static class CachedSwarmPath {
        public final PathPoint[] points;
        public final double targetX;
        public final double targetY;
        public final double targetZ;
        public final long createdTime;
        public final Class<?> mobClass;

        public CachedSwarmPath(Path path, double tx, double ty, double tz, long time, Class<?> mobClass) {
            this.targetX = tx;
            this.targetY = ty;
            this.targetZ = tz;
            this.createdTime = time;
            this.mobClass = mobClass;

            int len = path.getCurrentPathLength();
            this.points = new PathPoint[len];
            for (int i = 0; i < len; i++) {
                PathPoint pt = path.getPathPointFromIndex(i);
                this.points[i] = pt == null ? null : new PathPoint(pt.x, pt.y, pt.z);
            }
        }

        public boolean isFresh(long now) {
            return (now - this.createdTime) <= ModConfig.PATHFINDING.swarmCacheLifetimeTicks;
        }

        public Path sliceFromNearest(EntityLiving entity) {
            if (this.points.length == 0) {
                return null;
            }

            double ex = entity.posX;
            double ey = entity.posY;
            double ez = entity.posZ;

            int closestIndex = -1;
            double minDistanceSq = Double.MAX_VALUE;

            for (int i = 0; i < this.points.length; i++) {
                PathPoint pt = this.points[i];
                if (pt == null) {
                    continue;
                }
                double dx = pt.x + 0.5 - ex;
                double dy = pt.y - ey;
                double dz = pt.z + 0.5 - ez;
                double dSq = dx * dx + dy * dy + dz * dz;

                if (dSq < minDistanceSq) {
                    minDistanceSq = dSq;
                    closestIndex = i;
                }
            }

            // If the closest point is within 4.0 blocks (16.0 distSq), reuse the remaining path
            if (closestIndex >= 0 && minDistanceSq <= 16.0) {
                int remaining = this.points.length - closestIndex;
                if (remaining <= 0) {
                    return null;
                }

                // Deep-copy the path points. PathPoint holds per-path mutable state (visited flags,
                // path indices, distance caches), so sharing instances between entities corrupts
                // the navigation of every mob that reuses the cached path.
                PathPoint[] subPoints = new PathPoint[remaining];
                for (int i = 0; i < remaining; i++) {
                    PathPoint pt = this.points[closestIndex + i];
                    subPoints[i] = pt == null ? new PathPoint(0, 0, 0) : new PathPoint(pt.x, pt.y, pt.z);
                }
                return new Path(subPoints);
            }

            return null;
        }
    }

    private static final Map<Long, CachedSwarmPath> SWARM_MAP = new ConcurrentHashMap<>();
    private static long lastCleanupTime = 0;

    private static long getClusterKey(EntityLiving entity, int targetId) {
        long cx = (long) Math.floor(entity.posX / ModConfig.PATHFINDING.swarmClusterRadius);
        long cz = (long) Math.floor(entity.posZ / ModConfig.PATHFINDING.swarmClusterRadius);
        long dim = entity.world != null ? (long) entity.world.provider.getDimension() : 0L;
        long h = (cx & 0xFFFFFL) | ((cz & 0xFFFFFL) << 20) | (((long) targetId & 0xFFFFFL) << 40);
        return h ^ (dim * 31337L) ^ entity.getClass().hashCode();
    }

    private static long getClusterKeyPos(EntityLiving entity, BlockPos pos) {
        long cx = (long) Math.floor(entity.posX / ModConfig.PATHFINDING.swarmClusterRadius);
        long cz = (long) Math.floor(entity.posZ / ModConfig.PATHFINDING.swarmClusterRadius);
        long dim = entity.world != null ? (long) entity.world.provider.getDimension() : 0L;
        long h = (cx & 0xFFFFFL) | ((cz & 0xFFFFFL) << 20) | (((long) pos.hashCode() & 0xFFFFFL) << 40);
        return h ^ (dim * 31337L) ^ entity.getClass().hashCode();
    }

    public static Path tryGetCachedPath(EntityLiving entity, Entity target) {
        if (!ModConfig.PATHFINDING.enabled || !ModConfig.PATHFINDING.enableSwarmCache || BossChecker.isBoss(entity) || target == null) {
            return null;
        }

        long key = getClusterKey(entity, target.getEntityId());
        CachedSwarmPath cached = SWARM_MAP.get(key);
        if (cached != null) {
            long now = entity.world.getTotalWorldTime();
            if (cached.isFresh(now)) {
                Path sliced = cached.sliceFromNearest(entity);
                if (sliced != null) {
                    PathBackoffTracker.TOTAL_SWARM_HITS.incrementAndGet();
                    return sliced;
                }
            } else {
                SWARM_MAP.remove(key);
            }
        }
        return null;
    }

    public static Path tryGetCachedPathPos(EntityLiving entity, BlockPos pos) {
        if (!ModConfig.PATHFINDING.enabled || !ModConfig.PATHFINDING.enableSwarmCache || BossChecker.isBoss(entity) || pos == null) {
            return null;
        }

        long key = getClusterKeyPos(entity, pos);
        CachedSwarmPath cached = SWARM_MAP.get(key);
        if (cached != null) {
            long now = entity.world.getTotalWorldTime();
            if (cached.isFresh(now)) {
                Path sliced = cached.sliceFromNearest(entity);
                if (sliced != null) {
                    PathBackoffTracker.TOTAL_SWARM_HITS.incrementAndGet();
                    return sliced;
                }
            } else {
                SWARM_MAP.remove(key);
            }
        }
        return null;
    }

    public static void putCachedPath(EntityLiving entity, Entity target, Path path) {
        if (!ModConfig.PATHFINDING.enabled || !ModConfig.PATHFINDING.enableSwarmCache || BossChecker.isBoss(entity) || target == null || path == null) {
            return;
        }
        if (path.getCurrentPathLength() <= 1) {
            return;
        }

        long key = getClusterKey(entity, target.getEntityId());
        long now = entity.world.getTotalWorldTime();
        SWARM_MAP.put(key, new CachedSwarmPath(path, target.posX, target.posY, target.posZ, now, entity.getClass()));
    }

    public static void putCachedPathPos(EntityLiving entity, BlockPos pos, Path path) {
        if (!ModConfig.PATHFINDING.enabled || !ModConfig.PATHFINDING.enableSwarmCache || BossChecker.isBoss(entity) || pos == null || path == null) {
            return;
        }
        if (path.getCurrentPathLength() <= 1) {
            return;
        }

        long key = getClusterKeyPos(entity, pos);
        long now = entity.world.getTotalWorldTime();
        SWARM_MAP.put(key, new CachedSwarmPath(path, pos.getX(), pos.getY(), pos.getZ(), now, entity.getClass()));
    }

    public static void cleanup(long now) {
        if (now - lastCleanupTime < 100) {
            return;
        }
        lastCleanupTime = now;
        Iterator<Map.Entry<Long, CachedSwarmPath>> it = SWARM_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, CachedSwarmPath> entry = it.next();
            if (!entry.getValue().isFresh(now)) {
                it.remove();
            }
        }
    }

    public static void clearAll() {
        SWARM_MAP.clear();
    }
}
