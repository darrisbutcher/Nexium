package net.storagenexus.essentials.backpack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The cube of world around a player that Auto Torch plans in: {@value #SIDE} cells a side, for each cell how much block
 * light is lost entering it, whether a hostile mob could spawn there ({@link #SPAWNABLE}) and whether it is dark enough
 * that one could ({@link #DARK}). Plain arrays and arithmetic, so the planning can be tested without a world.
 *
 * <p>Light works as in the game: a torch gives light 14 and every step through a cell that lets light in costs at least 1
 * (more for cells that dampen it), so a torch lights a cell if the cheapest path to it costs {@code 13 - limit} or less,
 * where {@code limit} is the dimension's monster spawn block light limit (0 in the Overworld: any light at all stops
 * spawns). {@link #plan} places torches greedily: starting from the dark spawnable cell nearest the player, it puts the next
 * torch where it lights the most dark cells that are still unlit (counting torches already planned), so torches end up
 * about as far apart as they can be while leaving no dark cell, even in separate caves behind walls.
 *
 * <p>On open ground the best arrangement is a staggered lattice: the diamonds a torch lights (radius {@code r}) tile the
 * plane exactly when torches are spaced {@code (r, r + 1)} and {@code (-(r + 1), r)} apart, one torch per
 * {@code 2r² + 2r + 1} blocks (365 for the Overworld). A greedy choice alone leaves overlaps and gaps (more than twice that
 * many torches), so cells on that lattice, fixed to world coordinates so every sweep, player and cave uses the same one, get a
 * bonus when choosing; where terrain makes a lattice cell a poor choice the greedy gain wins.
 */
public final class TorchGrid {
    public static final int RADIUS = 32;
    public static final int SIDE = 2 * RADIUS + 1;
    public static final int SIZE = SIDE * SIDE * SIDE;
    public static final int TORCH_LIGHT = 14;
    /** Where a torch could go, tried per dark cell when choosing one (spread evenly through everything that lights it). */
    private static final int MAX_CANDIDATES = 24;

    /**
     * How much a lattice cell's gain counts for over another cell's, in blocks lit. It stops a lattice cell losing to a
     * neighbour that lights a few more, but not to one that lights far more (a lattice cell in a nook).
     */
    private static final int LATTICE_BONUS = 90;

    /** A mob could be spawned here: room for it to stand, on ground it can spawn on. */
    public static final byte SPAWNABLE = 1;
    /** Spawnable, with block light too low to stop spawns. */
    public static final byte DARK = 2;
    /** Lit by a torch this plan has already placed. */
    public static final byte COVERED = 4;
    /** Raw scan results, turned into {@link #SPAWNABLE} and {@link #DARK} by {@link #resolve}. */
    static final byte EMPTY = 8, FLOOR = 16;

    /** Light lost entering a cell: 0 if light can't enter it, else at least 1. */
    private final byte[] cost = new byte[SIZE];
    private final byte[] flags = new byte[SIZE];
    private final byte[] light = new byte[SIZE];
    private final int[] stamp = new int[SIZE];
    private final int[] dist = new int[SIZE];
    private int generation;
    private int limit;
    private int reach;
    /** World x and z of the grid's centre, so the lattice lines up from one sweep to the next. */
    private int originX, originZ;
    private int[][] buckets = new int[0][];
    private int[] bucketSize = new int[0];

    /** Index of the cell this far from the grid's centre in each direction (each from -{@value #RADIUS} to {@value #RADIUS}). */
    public static int index(int dx, int dy, int dz) {
        return ((dx + RADIUS) * SIDE + (dy + RADIUS)) * SIDE + (dz + RADIUS);
    }

    public static int dx(int index) {
        return index / (SIDE * SIDE) - RADIUS;
    }

    public static int dy(int index) {
        return (index / SIDE) % SIDE - RADIUS;
    }

    public static int dz(int index) {
        return index % SIDE - RADIUS;
    }

    /** Empties the grid for a new scan in a dimension with this monster spawn block light limit. */
    public void reset(int monsterLightLimit) {
        Arrays.fill(cost, (byte) 0);
        Arrays.fill(flags, (byte) 0);
        Arrays.fill(light, (byte) 15);
        limit = monsterLightLimit;
        reach = Math.max(0, TORCH_LIGHT - 1 - monsterLightLimit);
        buckets = new int[reach + 1][64];
        bucketSize = new int[reach + 1];
    }

    /** How many steps away a torch still stops spawns: 13 in the Overworld. Zero where torches can't (the Nether). */
    public int reach() {
        return reach;
    }

    /**
     * Records a scanned cell. {@code lightLost} is how much light entering it costs (0 if it blocks light), {@code empty}
     * and {@code floor} say whether a mob could stand in it and whether it is ground a mob can spawn on, and
     * {@code blockLight} is the block light there now (only needed for an empty cell above a floor).
     */
    public void set(int index, int lightLost, boolean empty, boolean floor, int blockLight) {
        cost[index] = (byte) lightLost;
        byte f = 0;
        if (empty) f |= EMPTY;
        if (floor) f |= FLOOR;
        flags[index] = f;
        light[index] = (byte) blockLight;
    }

    /** Works out which cells are spawnable (an empty cell with an empty one above and floor below) and which of those are dark. */
    public void resolve() {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                int base = (x * SIDE) * SIDE + z;
                for (int y = 1; y < SIDE - 1; y++) {
                    int i = base + y * SIDE;
                    if ((flags[i] & EMPTY) == 0 || (flags[i - SIDE] & FLOOR) == 0 || (flags[i + SIDE] & EMPTY) == 0) continue;
                    flags[i] |= SPAWNABLE;
                    if (light[i] <= limit) flags[i] |= DARK;
                }
            }
        }
    }

    public boolean isSpawnable(int index) {
        return (flags[index] & SPAWNABLE) != 0;
    }

    /** Dark, and not lit by a torch planned so far. */
    public boolean isDark(int index) {
        return (flags[index] & (DARK | COVERED)) == DARK;
    }

    /** How many dark cells the grid has (before any planning). */
    public int darkCells() {
        int n = 0;
        for (byte f : flags) if ((f & DARK) != 0) n++;
        return n;
    }

    @FunctionalInterface
    private interface Visitor {
        void visit(int index, int distance);
    }

    private int visitedCount;

    /** Visits every cell light from a torch at {@code start} reaches (within {@link #reach} steps), nearest first. */
    private void flood(int start, Visitor visitor) {
        generation++;
        stamp[start] = generation;
        dist[start] = 0;
        push(0, start);
        for (int d = 0; d <= reach; d++) {
            // New cells always go to a later bucket (every step costs at least 1), so this one doesn't grow while it is read
            for (int k = 0; k < bucketSize[d]; k++) {
                int i = buckets[d][k];
                if (dist[i] != d) continue;
                visitor.visit(i, d);
                int x = i / (SIDE * SIDE), y = (i / SIDE) % SIDE, z = i % SIDE;
                if (x > 0) spread(i - SIDE * SIDE, d);
                if (x < SIDE - 1) spread(i + SIDE * SIDE, d);
                if (y > 0) spread(i - SIDE, d);
                if (y < SIDE - 1) spread(i + SIDE, d);
                if (z > 0) spread(i - 1, d);
                if (z < SIDE - 1) spread(i + 1, d);
            }
            bucketSize[d] = 0;
        }
    }

    private void spread(int to, int from) {
        int c = cost[to];
        if (c == 0) return;
        int nd = from + c;
        if (nd > reach) return;
        if (stamp[to] != generation || nd < dist[to]) {
            stamp[to] = generation;
            dist[to] = nd;
            push(nd, to);
        }
    }

    private void push(int d, int index) {
        if (bucketSize[d] == buckets[d].length) buckets[d] = Arrays.copyOf(buckets[d], buckets[d].length * 2);
        buckets[d][bucketSize[d]++] = index;
    }

    /** Whether this cell is on the lattice the diamonds a torch lights tile the plane with (any height). */
    private boolean onLattice(int cell) {
        int x = originX + dx(cell), z = originZ + dz(cell);
        int area = 2 * reach * reach + 2 * reach + 1;
        return Math.floorMod(reach * x + (reach + 1) * z, area) == 0 && Math.floorMod(-(reach + 1) * x + reach * z, area) == 0;
    }

    /** How many dark, still-unlit cells a torch here would light. */
    private int gain(int torch) {
        visitedCount = 0;
        flood(torch, (i, d) -> {
            if ((flags[i] & (DARK | COVERED)) == DARK) visitedCount++;
        });
        return visitedCount;
    }

    /** Marks everything a torch here lights as covered, and the torch's own cell as taken. */
    private void cover(int torch) {
        flood(torch, (i, d) -> flags[i] |= COVERED);
        flags[torch] &= ~(SPAWNABLE | DARK);
    }

    /** The cell to put a torch in to light this dark cell and as many other dark cells as possible, or -1 if none can hold one. */
    private int bestTorchFor(int dark, IntPredicate canHold) {
        List<Integer> candidates = new ArrayList<>();
        flood(dark, (i, d) -> {
            if ((flags[i] & SPAWNABLE) != 0) candidates.add(i);
        });
        if (candidates.isEmpty()) return -1;
        // Every lattice cell that lights it, and an even spread of the rest
        int step = Math.max(1, candidates.size() / MAX_CANDIDATES);
        int best = -1, bestScore = Integer.MIN_VALUE;
        for (int k = 0; k < candidates.size(); k++) {
            int candidate = candidates.get(k);
            boolean lattice = onLattice(candidate);
            if (!lattice && k % step != 0) continue;
            if (!canHold.test(candidate)) continue;
            int score = gain(candidate) + (lattice ? LATTICE_BONUS : 0);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        if (best < 0) {
            // None of those could hold a torch: take the nearest one that can
            for (int candidate : candidates) {
                if (canHold.test(candidate)) return candidate;
            }
        }
        return best;
    }

    /**
     * Plans up to {@code maxTorches} torches, in the order to place them: each lights the dark cell nearest the player that
     * no earlier one lights (and as many others as it can), until no dark cell is left, the budget is used or the deadline
     * ({@link System#nanoTime} value) passes. {@code canHold} says whether a torch can really go in a cell, and
     * {@code worldX}/{@code worldZ} are the world coordinates of the grid's centre. The cells it returns are treated as
     * lit from then on.
     */
    public List<Integer> plan(int player, int maxTorches, IntPredicate canHold, long deadline, int worldX, int worldZ) {
        originX = worldX;
        originZ = worldZ;
        List<Integer> placed = new ArrayList<>();
        if (reach <= 0 || maxTorches <= 0) return placed;
        int px = dx(player), py = dy(player), pz = dz(player);
        int n = 0;
        long[] seeds = new long[Math.min(SIZE, 4096)];
        for (int i = 0; i < SIZE; i++) {
            if ((flags[i] & DARK) == 0) continue;
            if (n == seeds.length) seeds = Arrays.copyOf(seeds, n * 2);
            long ex = dx(i) - px, ey = dy(i) - py, ez = dz(i) - pz;
            seeds[n++] = ((ex * ex + ey * ey + ez * ez) << 32) | i;
        }
        Arrays.sort(seeds, 0, n);
        for (int k = 0; k < n && placed.size() < maxTorches; k++) {
            int seed = (int) (seeds[k] & 0xFFFFFFFFL);
            if (!isDark(seed)) continue;
            if (System.nanoTime() > deadline) break;
            int torch = bestTorchFor(seed, canHold);
            if (torch < 0) {
                // Nothing can light this one: leave it out of the rest of the plan
                flags[seed] |= COVERED;
                continue;
            }
            placed.add(torch);
            cover(torch);
        }
        return placed;
    }
}
