package com.cleanroommc.tickflow.command;

import com.cleanroommc.tickflow.collision.FastCollisionEngine;
import com.cleanroommc.tickflow.config.ModConfig;
import com.cleanroommc.tickflow.dormancy.ChestDormancyManager;
import com.cleanroommc.tickflow.dormancy.SpawnerDormancyManager;
import com.cleanroommc.tickflow.path.PathBackoffTracker;
import com.cleanroommc.tickflow.path.SwarmPathCache;
import com.cleanroommc.tickflow.stacker.StackerManager;
import com.cleanroommc.tickflow.trees.TreeDormancyManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandTickFlow extends CommandBase {

    @Override
    public String getName() {
        return "tickflow";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tickflow <path|stacker|dormancy|trees|collision> [stats|reset|clear|toggle|merge]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: " + getUsage(sender)));
            return;
        }

        String sub = args[0].toLowerCase();
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);

        if (sub.equals("path")) {
            executePath(sender, rest);
        } else if (sub.equals("stacker")) {
            executeStacker(sender, rest);
        } else if (sub.equals("dormancy")) {
            executeDormancy(sender, rest);
        } else if (sub.equals("trees")) {
            executeTrees(sender, rest);
        } else if (sub.equals("collision")) {
            executeCollision(sender, rest);
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: " + getUsage(sender)));
        }
    }

    private void executePath(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            long total = PathBackoffTracker.TOTAL_REQUESTS.get();
            long suppressed = PathBackoffTracker.TOTAL_SUPPRESSED.get();
            long swarmHits = PathBackoffTracker.TOTAL_SWARM_HITS.get();
            int active = PathBackoffTracker.getActiveBackoffCount();

            double suppressedPct = total > 0 ? (suppressed * 100.0 / total) : 0.0;
            double swarmPct = total > 0 ? (swarmHits * 100.0 / total) : 0.0;

            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== AsyncPathfinder Statistics ==="));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Status: " +
                    (ModConfig.PATHFINDING.enabled ? TextFormatting.GREEN + "ACTIVE" : TextFormatting.RED + "DISABLED")));
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Total Path Requests: " + TextFormatting.WHITE + total));
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Suppressed via Backoff: " + TextFormatting.WHITE +
                    suppressed + String.format(" (%.1f%%)", suppressedPct)));
            sender.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "Swarm Cache Hits: " + TextFormatting.WHITE +
                    swarmHits + String.format(" (%.1f%%)", swarmPct)));
            sender.sendMessage(new TextComponentString(TextFormatting.DARK_AQUA + "Active Mobs Backed Off: " + TextFormatting.WHITE + active));
        } else if (args[0].equalsIgnoreCase("reset")) {
            PathBackoffTracker.clearAll();
            SwarmPathCache.clearAll();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "AsyncPathfinder counters and caches cleared."));
        } else if (args[0].equalsIgnoreCase("toggle")) {
            ModConfig.PATHFINDING.enabled = !ModConfig.PATHFINDING.enabled;
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "AsyncPathfinder optimizations now: " +
                    (ModConfig.PATHFINDING.enabled ? TextFormatting.GREEN + "ENABLED" : TextFormatting.RED + "DISABLED")));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /asyncpath [stats|reset|toggle]"));
        }
    }

    private void executeStacker(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            long spawned = StackerManager.TOTAL_ITEMS_SPAWNED.get();
            long merged = StackerManager.TOTAL_ITEMS_MERGED.get();
            long saved = StackerManager.TOTAL_ENTITIES_SAVED.get();

            double ratio = spawned > 0 ? (saved * 100.0 / spawned) : 0.0;

            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Spatial Loot Stacker Statistics ==="));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Status: " +
                    (ModConfig.ITEM_STACKING.enabled ? TextFormatting.GREEN + "ACTIVE" : TextFormatting.RED + "DISABLED")));
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Total Items Processed: " + TextFormatting.WHITE + spawned));
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Items Merged: " + TextFormatting.WHITE + merged));
            sender.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "Entity Instances Saved: " + TextFormatting.WHITE +
                    saved + String.format(" (%.1f%% reduction)", ratio)));
        } else if (args[0].equalsIgnoreCase("merge")) {
            if (!(sender instanceof EntityPlayer)) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "This command can only be executed by a player in a world."));
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            double r = 16.0;
            if (args.length > 1) {
                try {
                    r = Double.parseDouble(args[1]);
                } catch (NumberFormatException ignored) {}
            }

            AxisAlignedBB box = player.getEntityBoundingBox().grow(r, r, r);
            int count = StackerManager.forceMergeInRadius(player.world, box);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Collapsed " + count + " items within " + r + " blocks."));
        } else if (args[0].equalsIgnoreCase("clear")) {
            StackerManager.clearStats();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Spatial Loot Stacker stats reset."));
        } else if (args[0].equalsIgnoreCase("toggle")) {
            ModConfig.ITEM_STACKING.enabled = !ModConfig.ITEM_STACKING.enabled;
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Spatial Loot Stacker now: " +
                    (ModConfig.ITEM_STACKING.enabled ? TextFormatting.GREEN + "ENABLED" : TextFormatting.RED + "DISABLED")));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /lootstacker [stats|merge|clear|toggle]"));
        }
    }

    private void executeDormancy(ICommandSender sender, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            long spawnerSaved = SpawnerDormancyManager.SPAWNER_TICKS_SAVED.get();
            long spawnerExec = SpawnerDormancyManager.SPAWNER_TICKS_EXECUTED.get();
            long chestSaved = ChestDormancyManager.CHEST_TICKS_SAVED.get();
            long totalSpawner = spawnerSaved + spawnerExec;
            double spawnerPct = totalSpawner > 0 ? ((double) spawnerSaved / totalSpawner) * 100.0D : 0.0D;

            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Tile Entity Dormancy Statistics ==="));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Enabled: " + TextFormatting.WHITE + ModConfig.DORMANCY.enabled));
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Spawner Ticks Saved: " + TextFormatting.WHITE + spawnerSaved + String.format(" (%.1f%% CPU reduction)", spawnerPct)));
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Spawner Ticks Executed: " + TextFormatting.WHITE + spawnerExec));
            sender.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "Chest Ticks Saved: " + TextFormatting.WHITE + chestSaved));
            return;
        }

        if ("clear".equalsIgnoreCase(args[0])) {
            SpawnerDormancyManager.clearAll();
            ChestDormancyManager.clearAll();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[TileEntityDormancy] Statistics and wake maps cleared."));
            return;
        }

        if ("toggle".equalsIgnoreCase(args[0])) {
            ModConfig.DORMANCY.enabled = !ModConfig.DORMANCY.enabled;
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[TileEntityDormancy] Enabled set to: " + ModConfig.DORMANCY.enabled));
            return;
        }

        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /tedormancy <status|clear|toggle>"));
    }

    private void executeTrees(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            int dormant = TreeDormancyManager.getDormantTreeCount();
            long saved = TreeDormancyManager.TICKS_SAVED.get();
            long awakened = TreeDormancyManager.AWAKEN_COUNT.get();

            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "=== DynamicTrees Dormancy Statistics ==="));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Status: " +
                    (ModConfig.DYNAMIC_TREES.enabled ? TextFormatting.GREEN + "ACTIVE" : TextFormatting.RED + "DISABLED")));
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Currently Dormant Trees: " + TextFormatting.WHITE + dormant));
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Random Growth Ticks Saved: " + TextFormatting.WHITE + saved));
            sender.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "Tree Awakening Events: " + TextFormatting.WHITE + awakened));
        } else if (args[0].equalsIgnoreCase("clear")) {
            TreeDormancyManager.clearAll();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "DynamicTrees dormancy cache cleared. All trees will re-evaluate."));
        } else if (args[0].equalsIgnoreCase("toggle")) {
            ModConfig.DYNAMIC_TREES.enabled = !ModConfig.DYNAMIC_TREES.enabled;
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "DynamicTrees dormancy optimization now: " +
                    (ModConfig.DYNAMIC_TREES.enabled ? TextFormatting.GREEN + "ENABLED" : TextFormatting.RED + "DISABLED")));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /dtdormancy [stats|clear|toggle]"));
        }
    }

    private void executeCollision(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            long total = FastCollisionEngine.TOTAL_COLLISION_CALLS.get();
            long skipped = FastCollisionEngine.EMPTY_SECTIONS_SKIPPED.get();
            long fastHits = FastCollisionEngine.FULL_CUBE_FAST_HITS.get();

            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Fast Voxel Collision Statistics ==="));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Status: " +
                    (ModConfig.COLLISION.enabled ? TextFormatting.GREEN + "ACTIVE" : TextFormatting.RED + "DISABLED")));
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Total Collision Queries: " + TextFormatting.WHITE + total));
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Empty Sections Skipped (16-block col): " + TextFormatting.WHITE + skipped));
            sender.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "Full Cube Fast Path Hits: " + TextFormatting.WHITE + fastHits));
        } else if (args[0].equalsIgnoreCase("clear")) {
            FastCollisionEngine.clearStats();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Fast Voxel Collision stats reset."));
        } else if (args[0].equalsIgnoreCase("toggle")) {
            ModConfig.COLLISION.enabled = !ModConfig.COLLISION.enabled;
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Fast Voxel Collision now: " +
                    (ModConfig.COLLISION.enabled ? TextFormatting.GREEN + "ENABLED" : TextFormatting.RED + "DISABLED")));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /voxelcollision [stats|clear|toggle]"));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "path", "stacker", "dormancy", "trees", "collision");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("path")) {
                return getListOfStringsMatchingLastWord(args, "stats", "reset", "toggle");
            }
            if (sub.equals("stacker")) {
                return getListOfStringsMatchingLastWord(args, "stats", "merge", "clear", "toggle");
            }
            if (sub.equals("dormancy")) {
                return getListOfStringsMatchingLastWord(args, "status", "clear", "toggle");
            }
            if (sub.equals("trees")) {
                return getListOfStringsMatchingLastWord(args, "stats", "clear", "toggle");
            }
            if (sub.equals("collision")) {
                return getListOfStringsMatchingLastWord(args, "stats", "clear", "toggle");
            }
        }
        return Collections.emptyList();
    }
}
