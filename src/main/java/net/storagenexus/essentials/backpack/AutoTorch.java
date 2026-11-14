package net.storagenexus.essentials.backpack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.storagenexus.essentials.slots.ExtraSlots;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Auto Torch module, on the server. While a Backpack with the module in and on is worn, and has torches in it, this
 * sweeps the {@value TorchGrid#SIDE}-block cube around the player (a few slices a tick, so it never stalls the server)
 * and places torches, taken from the Backpack, wherever a hostile mob could still spawn: any ground in the cube
 * with block light too low to stop spawns, in the open, in caves behind walls and in caves nobody has seen. {@link TorchGrid}
 * decides where, so they end up as far apart as they can be and still leave no dark spawnable block.
 *
 * <p>It does nothing in dimensions where light can't stop spawns (the Nether), in spectator and adventure mode, in spawn
 * protection, in chunks that aren't loaded or lit yet, or while the Backpack's screen is open.
 */
public final class AutoTorch {
    /** Slices of the cube read each tick. */
    private static final int PLANES_PER_TICK = 8;
    /** Most torches one sweep places; the next sweep sees the new light and carries on. */
    public static final int MAX_PER_SWEEP = 8;
    /** Ticks before sweeping again after one that placed torches (the light engine needs a moment) and after one that found nothing. */
    private static final int SETTLE_TICKS = 10, IDLE_TICKS = 40;
    /** Longest one sweep may spend choosing places for its torches. */
    private static final long PLAN_BUDGET_NANOS = 10_000_000L;
    /** A sweep starts over if the player has moved this far from where it started. */
    private static final int MOVED_TOO_FAR = 8;

    private static final Map<UUID, Sweep> SWEEPS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SWEEPS.remove(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SWEEPS.clear());
    }

    /** Whether the player's Auto Torch has anything to do: the module is on in the worn Backpack, torches are in it, and torches can work here. */
    public static boolean active(ServerPlayer player) {
        ItemStack backpack = ExtraSlots.of(player).backpack();
        return backpack.getItem() instanceof BackpackItem
                && BackpackModules.active(backpack, BackpackModuleType.AUTO_TORCH)
                && player.isAlive() && !player.isSpectator() && player.mayBuild()
                && !(player.containerMenu instanceof BackpackMenu)
                && player.level().dimensionType().monsterSpawnBlockLightLimit() < TorchGrid.TORCH_LIGHT - 1
                && BackpackContents.count(backpack, Items.TORCH) > 0;
    }

    private static void tick(ServerPlayer player) {
        if (!active(player)) {
            SWEEPS.remove(player.getUUID());
            return;
        }
        SWEEPS.computeIfAbsent(player.getUUID(), id -> new Sweep()).step(player);
    }

    /** Scans and plans in one go and places what it plans: what a sweep does, without waiting for ticks. Returns how many torches it placed. */
    public static int sweepNow(ServerPlayer player) {
        if (!active(player)) return 0;
        Sweep sweep = new Sweep();
        sweep.start(player);
        sweep.scan(player.level(), 0, TorchGrid.SIDE);
        return sweep.finish(player, Long.MAX_VALUE);
    }

    private static boolean canHold(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return level.getBlockState(pos).isAir() && Blocks.TORCH.defaultBlockState().canSurvive(level, pos) && level.mayInteract(player, pos);
    }

    /** One player's sweeps: a scan of the cube read in slices, then a plan, then the torches. */
    private static final class Sweep {
        private final TorchGrid grid = new TorchGrid();
        private ServerLevel level;
        private BlockPos center = BlockPos.ZERO;
        private int nextPlane;
        private boolean scanning;
        private int cooldown;

        void start(ServerPlayer player) {
            level = player.level();
            center = player.blockPosition();
            grid.reset(level.dimensionType().monsterSpawnBlockLightLimit());
            nextPlane = 0;
            scanning = true;
        }

        void step(ServerPlayer player) {
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (!scanning || level != player.level() || tooFar(player)) {
                start(player);
                return;
            }
            int to = Math.min(TorchGrid.SIDE, nextPlane + PLANES_PER_TICK);
            scan(level, nextPlane, to);
            nextPlane = to;
            if (nextPlane < TorchGrid.SIDE) return;
            scanning = false;
            cooldown = finish(player, System.nanoTime() + PLAN_BUDGET_NANOS) > 0 ? SETTLE_TICKS : IDLE_TICKS;
        }

        private boolean tooFar(ServerPlayer player) {
            BlockPos p = player.blockPosition();
            return Math.max(Math.abs(p.getX() - center.getX()), Math.max(Math.abs(p.getY() - center.getY()), Math.abs(p.getZ() - center.getZ()))) > MOVED_TOO_FAR;
        }

        /** Reads the slices {@code from} up to {@code to} (west to east) of the cube into the grid. Chunks that aren't loaded and lit are left as solid, unspawnable cells. */
        void scan(ServerLevel level, int from, int to) {
            int minY = Math.max(level.getMinY(), center.getY() - TorchGrid.RADIUS);
            int maxY = Math.min(level.getMaxY(), center.getY() + TorchGrid.RADIUS);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int px = from; px < to; px++) {
                int x = center.getX() + px - TorchGrid.RADIUS;
                for (int pz = 0; pz < TorchGrid.SIDE; pz++) {
                    int z = center.getZ() + pz - TorchGrid.RADIUS;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                    if (chunk == null || !chunk.isLightCorrect()) continue;
                    boolean floorBelow = false;
                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);
                        BlockState state = chunk.getBlockState(pos);
                        int damping = state.getLightDampening();
                        FluidState fluid = state.getFluidState();
                        boolean empty = NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, fluid, EntityTypes.ZOMBIE);
                        boolean floor = !empty && state.isValidSpawn(level, pos, EntityTypes.ZOMBIE);
                        int light = empty && floorBelow ? level.getBrightness(LightLayer.BLOCK, pos) : 15;
                        grid.set(TorchGrid.index(px - TorchGrid.RADIUS, y - center.getY(), pz - TorchGrid.RADIUS),
                                damping >= 15 ? 0 : Math.max(1, damping), empty, floor, light);
                        floorBelow = floor;
                    }
                }
            }
        }

        /** Plans torches for what was scanned (until the {@link System#nanoTime} deadline) and places them, taking them from the Backpack. Returns how many went down. */
        int finish(ServerPlayer player, long deadline) {
            ItemStack backpack = ExtraSlots.of(player).backpack();
            if (!(backpack.getItem() instanceof BackpackItem)) return 0;
            grid.resolve();
            int budget = Math.min(MAX_PER_SWEEP, BackpackContents.count(backpack, Items.TORCH));
            BlockPos at = player.blockPosition();
            int near = TorchGrid.index(clamp(at.getX() - center.getX()), clamp(at.getY() - center.getY()), clamp(at.getZ() - center.getZ()));
            List<Integer> cells = grid.plan(near, budget, cell -> canHold(level, cellPos(cell), player), deadline,
                    center.getX(), center.getZ());
            int placed = 0;
            for (int cell : cells) {
                BlockPos pos = cellPos(cell);
                // Only within the radius of where the player is now, and only where a torch still fits
                if (Math.abs(pos.getX() - at.getX()) > TorchGrid.RADIUS || Math.abs(pos.getY() - at.getY()) > TorchGrid.RADIUS
                        || Math.abs(pos.getZ() - at.getZ()) > TorchGrid.RADIUS || !canHold(level, pos, player)) continue;
                BlockState torch = Blocks.TORCH.defaultBlockState();
                level.setBlock(pos, torch, Block.UPDATE_ALL);
                level.playSound(null, pos, torch.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 1.0F);
                placed++;
            }
            if (placed > 0) BackpackContents.take(backpack, Items.TORCH, placed);
            return placed;
        }

        private BlockPos cellPos(int cell) {
            return center.offset(TorchGrid.dx(cell), TorchGrid.dy(cell), TorchGrid.dz(cell));
        }

        private static int clamp(int d) {
            return Math.max(-TorchGrid.RADIUS, Math.min(TorchGrid.RADIUS, d));
        }
    }

    private AutoTorch() {}
}
