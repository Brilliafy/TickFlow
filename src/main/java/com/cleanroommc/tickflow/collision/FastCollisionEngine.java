package com.cleanroommc.tickflow.collision;

import com.cleanroommc.tickflow.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FastCollisionEngine {

    public static final AtomicLong TOTAL_COLLISION_CALLS = new AtomicLong(0);
    public static final AtomicLong EMPTY_SECTIONS_SKIPPED = new AtomicLong(0);
    public static final AtomicLong FULL_CUBE_FAST_HITS = new AtomicLong(0);

    private static final ThreadLocal<BlockPos.MutableBlockPos> MUTABLE_POS =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public static boolean getCollisionBoxesFast(World world, @Nullable Entity entityIn, AxisAlignedBB aabb, boolean checkOnly, @Nullable List<AxisAlignedBB> outList) {
        if (!ModConfig.COLLISION.enabled) {
            return false;
        }

        TOTAL_COLLISION_CALLS.incrementAndGet();

        int minX = MathHelper.floor(aabb.minX) - 1;
        int maxX = MathHelper.ceil(aabb.maxX) + 1;
        int minY = MathHelper.floor(aabb.minY) - 1;
        int maxY = MathHelper.ceil(aabb.maxY) + 1;
        int minZ = MathHelper.floor(aabb.minZ) - 1;
        int maxZ = MathHelper.ceil(aabb.maxZ) + 1;

        WorldBorder worldborder = world.getWorldBorder();
        boolean flag = entityIn != null && entityIn.isOutsideBorder();
        boolean flag1 = entityIn != null && world.isInsideWorldBorder(entityIn);
        IBlockState borderState = Blocks.STONE.getDefaultState();

        BlockPos.MutableBlockPos mutablePos = MUTABLE_POS.get();

        // Batch the very hot counters into plain locals and flush them once, avoiding a CAS per block/section.
        int emptySkipped = 0;
        int fullHits = 0;

        try {
            if (checkOnly && !ForgeEventFactory.gatherCollisionBoxes(world, entityIn, aabb, outList)) {
                return true;
            }

            int lastChunkX = Integer.MIN_VALUE;
            int lastChunkZ = Integer.MIN_VALUE;
            Chunk cachedChunk = null;

            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    boolean edgeX = x == minX || x == maxX - 1;
                    boolean edgeZ = z == minZ || z == maxZ - 1;

                    if (edgeX && edgeZ) {
                        continue;
                    }

                    int chunkX = x >> 4;
                    int chunkZ = z >> 4;

                    Chunk chunk;
                    if (ModConfig.COLLISION.cacheChunkReferences && cachedChunk != null && chunkX == lastChunkX && chunkZ == lastChunkZ) {
                        chunk = cachedChunk;
                    } else {
                        lastChunkX = chunkX;
                        lastChunkZ = chunkZ;
                        chunk = cachedChunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                    }

                    if (chunk == null || !chunk.isLoaded()) {
                        continue;
                    }

                    ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();

                    for (int y = minY; y < maxY; ++y) {
                        if ((edgeX || edgeZ) && y == maxY - 1) {
                            continue;
                        }

                        if (checkOnly) {
                            if (x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) {
                                return true;
                            }
                        } else if (entityIn != null && flag == flag1) {
                            entityIn.setOutsideBorder(!flag1);
                        }

                        if (y < 0 || y >= 256) {
                            continue;
                        }

                        int sectionIdx = y >> 4;
                        ExtendedBlockStorage storage = (sectionIdx >= 0 && sectionIdx < storages.length) ? storages[sectionIdx] : null;

                        if (storage == null || storage.isEmpty()) {
                            if (ModConfig.COLLISION.skipEmptySections) {
                                emptySkipped++;
                                // Advance y to the top of this 16-block section (loop ++y then resumes at the next section).
                                y = ((sectionIdx + 1) << 4) - 1;
                                continue;
                            }
                        }

                        mutablePos.setPos(x, y, z);
                        IBlockState state;

                        if (!checkOnly && !worldborder.contains(mutablePos) && flag1) {
                            state = borderState;
                        } else if (storage != null) {
                            state = storage.get(x & 15, y & 15, z & 15);
                        } else {
                            state = chunk.getBlockState(mutablePos);
                        }

                        if (state.getBlock() == Blocks.AIR) {
                            if (checkOnly && !ForgeEventFactory.gatherCollisionBoxes(world, entityIn, aabb, outList)) {
                                return true;
                            }
                            continue;
                        }

                        if (ModConfig.COLLISION.enableFullCubeFastPath
                                && state.isFullCube()
                                && state.getCollisionBoundingBox(world, mutablePos) == Block.FULL_BLOCK_AABB) {
                            if (aabb.minX < x + 1 && aabb.maxX > x &&
                                aabb.minY < y + 1 && aabb.maxY > y &&
                                aabb.minZ < z + 1 && aabb.maxZ > z) {

                                fullHits++;
                                if (checkOnly) {
                                    return true;
                                }
                                if (outList != null) {
                                    outList.add(new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1));
                                }
                            }
                        } else {
                            state.addCollisionBoxToList(world, mutablePos, aabb, outList, entityIn, false);
                        }

                        if (checkOnly && !ForgeEventFactory.gatherCollisionBoxes(world, entityIn, aabb, outList)) {
                            return true;
                        }
                    }
                }
            }

            // Vanilla World#getCollisionBoxes returns whether any collision box was gathered, for both
            // check-only and full queries. The previous implementation returned false for check-only
            // queries, which let entities pass through solid blocks in suffocation/inside checks.
            return outList != null && !outList.isEmpty();
        } finally {
            if (emptySkipped != 0) {
                EMPTY_SECTIONS_SKIPPED.addAndGet(emptySkipped);
            }
            if (fullHits != 0) {
                FULL_CUBE_FAST_HITS.addAndGet(fullHits);
            }
        }
    }

    public static void clearStats() {
        TOTAL_COLLISION_CALLS.set(0);
        EMPTY_SECTIONS_SKIPPED.set(0);
        FULL_CUBE_FAST_HITS.set(0);
    }
}
