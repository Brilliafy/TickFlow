# Tick Flow

![TickFlow](logo.png)

Server side simulation optimization for Minecraft 1.12.2 on Cleanroom or Minecraft Forge.

Tick Flow reduces the cost of entities and tile entities that do not need to run at full speed, and
makes two hot paths cheaper. It runs on both the client and a dedicated server; the useful half is on
the server.

## Features

- Pathfinding failure backoff and swarm path sharing for mobs.
- Spatial merging of dropped items.
- Proximity dormancy for mob spawners and chests.
- Dormancy for fully grown DynamicTrees (only when DynamicTrees is installed).
- An allocation light voxel collision path.

## Pathfinding

Mobs that cannot reach their target retry the search constantly. Tick Flow records failed searches
and backs off for a growing delay before trying again, and resets the delay as soon as the target
moves. Nearby mobs of the same type that are heading to the same place can share a cached path. The
cached path is copied per mob, so two mobs never share the same path point objects.

## Item stacking

Dropped items are merged when they spawn and again while they search for neighbours. The search uses
a wider box than vanilla, so piles collapse faster and the entity count stays low around farms and
mining areas.

## Dormancy

Spawners and chests that are far from any player with no player using them are skipped for a number of
ticks. They wake immediately when a player approaches. Sitting tamed pets and mobs with AI disabled
are treated the same way by the pathfinding side.

When DynamicTrees is installed, fully grown trees with no soil life stop running growth ticks. A tree
wakes when nearby blocks are broken, when soil life is set, or when it is fertilized. The DynamicTrees
mixin is only loaded when that mod is present.

## Fast collision

Entity collision scanning is reimplemented to skip empty chunk sections and to use a fast path for
full block collision boxes. It mirrors the vanilla result, including the Forge collision event and
the world border handling.

## Requirements

- Minecraft 1.12.2
- Minecraft Forge 14.23.5.x or Cleanroom 0.6.x
- MixinBooter 10 or newer
- DynamicTrees is optional. The tree dormancy feature needs it.
- Java 8

## Installation

Put `tickflow-<version>.jar` into your `mods` folder. Install it on the server for the full effect.

## Configuration

File: `config/tickflow.cfg`

### Pathfinding

| Option | Default | Description |
|---|---|---|
| Enable Mod | true | Master toggle for the pathfinding optimizations. |
| Enable Failure Backoff | true | Back off after failed path searches. |
| Base Backoff Ticks | 10 | First delay after a failure. |
| Max Backoff Ticks | 140 | Longest delay after repeated failures. |
| Target Move Reset Distance Sq | 9.0 | Target movement that resets the delay. |
| Enable Swarm Path Sharing | true | Let nearby mobs share paths. |
| Swarm Cluster Radius | 6.0 | Cluster radius for sharing. |
| Swarm Cache Lifetime Ticks | 20 | How long a shared path stays valid. |

### Item Stacking

| Option | Default | Description |
|---|---|---|
| Enable Mod | true | Master toggle. |
| Merge Radius Horizontal | 3.5 | Horizontal search radius. |
| Merge Radius Vertical | 2.0 | Vertical search radius. |
| Merge On Spawn | true | Merge items as soon as they spawn. |
| Enable Density Overflow Collapse | true | Merge harder in dense areas. |
| Density Threshold | 40 | Items per area that triggers the harder merge. |

### Tile Entity Dormancy

| Option | Default | Description |
|---|---|---|
| Enable Mod | true | Master toggle. |
| Enable Spawner Dormancy | true | Skip far spawners. |
| Spawner Max Sleep Ticks | 20 | Longest sleep for a spawner. |
| Spawner Safety Buffer Blocks | 8.0 | Extra distance before a spawner sleeps. |
| Enable Chest Dormancy | true | Skip far, idle chests. |
| Chest Sleep Ticks | 20 | Sleep length for a chest. |

### Dynamic Trees

Only active when DynamicTrees is installed.

| Option | Default | Description |
|---|---|---|
| Enable Mod | true | Master toggle. |
| Enable Mature Tree Dormancy | true | Skip growth ticks for mature trees. |
| Dormancy Refresh Ticks | 12000 | Interval between integrity checks. |
| Awaken Radius | 16.0 | Radius that wakes nearby trees when a block breaks. |

### Collision

| Option | Default | Description |
|---|---|---|
| Enable Mod | true | Master toggle. |
| Full Cube Fast Path | true | Skip box allocation for full blocks. |
| Skip Empty Sections | true | Skip empty chunk sections. |
| Cache Chunk References | true | Cache the current chunk while scanning. |

## Commands

`/tickflow <path|stacker|dormancy|trees|collision> [stats|reset|clear|toggle|merge]`

Each group has its own status and toggle. `/tickflow stacker merge [radius]` collapses items near you.
Requires permission level 2.

## Notes and limitations

- These features change tick timing for distant entities. If a mod is sensitive to exact entity tick
  rates, its group can be turned off in the config.
- DynamicTrees dormancy is skipped entirely when DynamicTrees is not installed.

## Credits

Written by Brilliafy.

## License

GPL-3.0. Copyright (C) 2026 Brilliafy. See the LICENSE file.
