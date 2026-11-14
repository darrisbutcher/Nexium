package net.storagenexus.essentials.backpack;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Auto Torch's planning, on synthetic grids: where torches go, how far apart, what a wall does to light, and when nothing
 * should be placed. The world around a player is a TorchGrid, so none of this needs the game.
 */
class TorchGridTest {
    private static final int R = TorchGrid.RADIUS;
    /** Block light 13 steps from a torch is 1: the farthest a torch stops Overworld spawns. */
    private static final int REACH = 13;

    /** A flat field: solid ground below y 0, open air above it, no light anywhere. */
    private static TorchGrid flat(int limit) {
        return build(limit, (x, z) -> false, (dx, dz) -> 0);
    }

    private interface Wall {
        boolean at(int dx, int dz);
    }

    private interface Glow {
        int at(int dx, int dz);
    }

    /** A flat field with a full-height wall wherever {@code wall} says and this much block light on the ground. */
    private static TorchGrid build(int limit, Wall wall, Glow glow) {
        TorchGrid grid = new TorchGrid();
        grid.reset(limit);
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                for (int y = -R; y <= R; y++) {
                    int i = TorchGrid.index(x, y, z);
                    if (y < 0) grid.set(i, 0, false, y == -1 && !wall.at(x, z), 15);
                    else if (wall.at(x, z)) grid.set(i, 0, false, false, 15);
                    else grid.set(i, 1, true, false, y == 0 ? glow.at(x, z) : 0);
                }
            }
        }
        grid.resolve();
        return grid;
    }

    private static int player(int dx, int dz) {
        return TorchGrid.index(dx, 0, dz);
    }

    private static List<Integer> plan(TorchGrid grid, int dx, int dz, int max) {
        return grid.plan(player(dx, dz), max, cell -> true, Long.MAX_VALUE, 0, 0);
    }

    private static int manhattan(int a, int b) {
        return Math.abs(TorchGrid.dx(a) - TorchGrid.dx(b)) + Math.abs(TorchGrid.dy(a) - TorchGrid.dy(b)) + Math.abs(TorchGrid.dz(a) - TorchGrid.dz(b));
    }

    @Test
    void aFlatFieldHasOneDarkCellPerBlockOfGround() {
        TorchGrid grid = flat(0);
        assertEquals(13, grid.reach());
        assertEquals((2 * R + 1) * (2 * R + 1), grid.darkCells());
    }

    @Test
    void everyBlockOfAFlatFieldEndsUpLit() {
        TorchGrid grid = flat(0);
        List<Integer> torches = plan(grid, 0, 0, 1000);
        // Checked on its own, not through the plan's bookkeeping: in open air a torch lights what is within 13 steps
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                int cell = TorchGrid.index(x, 0, z);
                assertTrue(torches.stream().anyMatch(t -> manhattan(t, cell) <= REACH), "(" + x + ", " + z + ") is lit");
            }
        }
        assertFalse(torches.isEmpty());
    }

    @Test
    void torchesAreSpacedOutNotPackedTogether() {
        TorchGrid grid = flat(0);
        List<Integer> torches = plan(grid, 0, 0, 1000);
        // In an endless field the best is one torch per 365 blocks (a diamond of radius 13). In a 65 x 65 field torches can't
        // stand outside it, so the edges need a few more than 4225 / 365 = 11.6
        System.out.println("[torch grid] flat 65x65 field: " + torches.size() + " torches (4225 / 365 = 11.6 at the very least)");
        assertTrue(torches.size() <= 24, "at most 24 for the field, used " + torches.size());
        // Well inside the field the torches sit on the staggered lattice: neighbours (13, 14) and (-14, 13) apart, 27 steps
        int closest = Integer.MAX_VALUE;
        for (int a : torches) {
            for (int b : torches) {
                if (a >= b || Math.max(Math.abs(TorchGrid.dx(a)), Math.abs(TorchGrid.dz(a))) > 16
                        || Math.max(Math.abs(TorchGrid.dx(b)), Math.abs(TorchGrid.dz(b))) > 16) continue;
                closest = Math.min(closest, manhattan(a, b));
            }
        }
        System.out.println("[torch grid] closest two torches in the middle are " + closest + " steps apart");
        assertTrue(closest >= 26, "torches in the middle are 27 steps apart on the lattice, closest were " + closest);
    }

    @Test
    void theSameSpacingHoldsWhereverTheFieldSitsInTheWorld() {
        // The lattice is fixed to world coordinates, so a different position (a different phase) gives the same quality
        for (int[] origin : new int[][] {{0, 0}, {7, 3}, {-100, 250}, {13, 14}, {200, -31}}) {
            TorchGrid grid = flat(0);
            List<Integer> torches = grid.plan(player(0, 0), 1000, cell -> true, Long.MAX_VALUE, origin[0], origin[1]);
            assertTrue(torches.size() <= 24, "at most 24 at " + origin[0] + ", " + origin[1] + ", used " + torches.size());
            for (int x = -R; x <= R; x += 4) {
                for (int z = -R; z <= R; z += 4) {
                    int cell = TorchGrid.index(x, 0, z);
                    assertTrue(torches.stream().anyMatch(t -> manhattan(t, cell) <= REACH), "(" + x + ", " + z + ") is lit");
                }
            }
        }
    }

    @Test
    void torchesPlacedInTwoSweepsLineUpLikeOneSweep() {
        // A first sweep places a few torches; the next one sees them as light and carries on, on the same lattice
        TorchGrid first = flat(0);
        List<Integer> few = plan(first, 0, 0, 4);
        TorchGrid second = build(0, (x, z) -> false, (x, z) -> {
            int best = 0;
            for (int t : few) best = Math.max(best, 14 - (Math.abs(x - TorchGrid.dx(t)) + Math.abs(z - TorchGrid.dz(t))));
            return Math.max(0, best);
        });
        List<Integer> rest = plan(second, 0, 0, 1000);
        System.out.println("[torch grid] in two sweeps: " + (few.size() + rest.size()) + " torches");
        assertTrue(few.size() + rest.size() <= 26, "two sweeps use about what one does, used " + (few.size() + rest.size()));
    }

    @Test
    void theFirstTorchGoesNearThePlayer() {
        TorchGrid grid = flat(0);
        List<Integer> torches = plan(grid, 10, -7, 1);
        assertEquals(1, torches.size());
        assertTrue(manhattan(torches.get(0), player(10, -7)) <= REACH, "it lights the block the player stands on");
    }

    @Test
    void theBudgetLimitsHowManyArePlanned() {
        assertEquals(3, plan(flat(0), 0, 0, 3).size());
        assertTrue(plan(flat(0), 0, 0, 0).isEmpty());
    }

    @Test
    void aWallSplitsTheLightSoBothSidesGetTorches() {
        // A full wall along x = 0: no light crosses it, so the two halves need torches of their own
        TorchGrid grid = build(0, (x, z) -> x == 0, (x, z) -> 0);
        List<Integer> torches = plan(grid, -5, 0, 1000);
        assertTrue(torches.stream().anyMatch(t -> TorchGrid.dx(t) < 0), "a torch on the near side");
        assertTrue(torches.stream().anyMatch(t -> TorchGrid.dx(t) > 0), "a torch on the far side, behind the wall");
        for (int x = -R; x <= R; x++) {
            if (x == 0) continue;
            for (int z = -R; z <= R; z++) {
                int cell = TorchGrid.index(x, 0, z);
                int side = Integer.signum(x);
                assertTrue(torches.stream().anyMatch(t -> Integer.signum(TorchGrid.dx(t)) == side && manhattan(t, cell) <= REACH),
                        "(" + x + ", " + z + ") is lit by a torch on its own side");
            }
        }
    }

    @Test
    void groundThatIsAlreadyLitIsLeftAlone() {
        // Every block already has block light 1 or more: nothing can spawn there, so no torches
        TorchGrid lit = build(0, (x, z) -> false, (x, z) -> 1);
        assertEquals(0, lit.darkCells());
        assertTrue(plan(lit, 0, 0, 1000).isEmpty());
    }

    @Test
    void existingLightCountsTowardsWhatIsDark() {
        // A torch already stands in the middle: light 14 - d at distance d, so a diamond of 365 blocks is lit
        TorchGrid grid = build(0, (x, z) -> false, (x, z) -> Math.max(0, 14 - (Math.abs(x) + Math.abs(z))));
        assertEquals((2 * R + 1) * (2 * R + 1) - 365, grid.darkCells());
        int withOne = plan(grid, 0, 0, 1000).size();
        int without = plan(flat(0), 0, 0, 1000).size();
        assertTrue(withOne < without, "fewer torches with one already there (" + withOne + " against " + without + ")");
    }

    @Test
    void whereLightCannotStopSpawnsNothingIsPlanned() {
        // The Nether's limit is 15: even full light doesn't stop spawns
        assertEquals(0, flat(15).reach());
        assertTrue(plan(flat(15), 0, 0, 1000).isEmpty());
        // A limit of 14 is out of reach of a torch as well
        assertTrue(plan(flat(14), 0, 0, 1000).isEmpty());
    }

    @Test
    void aHigherLimitMeansShorterReachAndMoreTorches() {
        // With limit 5 a torch only lights 8 steps (light 14 - 8 = 6 > 5)
        TorchGrid grid = flat(5);
        assertEquals(8, grid.reach());
        assertTrue(plan(grid, 0, 0, 1000).size() > plan(flat(0), 0, 0, 1000).size());
    }

    @Test
    void groundThatCannotHoldATorchIsSkippedWithoutStalling() {
        TorchGrid grid = flat(0);
        List<Integer> none = grid.plan(player(0, 0), 1000, cell -> false, Long.MAX_VALUE, 0, 0);
        assertTrue(none.isEmpty());
    }

    @Test
    void aCaveBelowAndAFieldAboveAreLitSeparately() {
        // A solid slab at y -1 with a sealed cave under it (floor at y -9, air from y -8 to y -2): the cave needs its own torches
        TorchGrid grid = new TorchGrid();
        grid.reset(0);
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                for (int y = -R; y <= R; y++) {
                    int i = TorchGrid.index(x, y, z);
                    if (y >= 0) grid.set(i, 1, true, false, 0);
                    else if (y == -1) grid.set(i, 0, false, true, 15);
                    else if (y >= -8) grid.set(i, 1, true, false, 0);
                    else grid.set(i, 0, false, y == -9, 15);
                }
            }
        }
        grid.resolve();
        List<Integer> torches = grid.plan(player(0, 0), 1000, cell -> true, Long.MAX_VALUE, 0, 0);
        assertTrue(torches.stream().anyMatch(t -> TorchGrid.dy(t) >= 0), "a torch up on the surface");
        assertTrue(torches.stream().anyMatch(t -> TorchGrid.dy(t) <= -2 && TorchGrid.dy(t) >= -8), "a torch down in the cave");
        // Independent check of the cave floor: lit by a torch inside the cave (the slab is solid, so nothing above reaches it)
        List<Integer> inCave = new ArrayList<>();
        for (int t : torches) if (TorchGrid.dy(t) < -1) inCave.add(t);
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                int cell = TorchGrid.index(x, -8, z);
                assertTrue(inCave.stream().anyMatch(t -> manhattan(t, cell) <= REACH), "cave floor (" + x + ", " + z + ") is lit");
            }
        }
    }

    @Test
    void indexAndCoordinatesRoundTrip() {
        IntUnaryOperator check = i -> TorchGrid.index(TorchGrid.dx(i), TorchGrid.dy(i), TorchGrid.dz(i));
        for (int i : new int[] {0, 1, 12345, TorchGrid.SIZE - 1, TorchGrid.index(-R, R, 0), TorchGrid.index(R, -R, R)}) assertEquals(i, check.applyAsInt(i));
        assertEquals(TorchGrid.index(0, 0, 0), TorchGrid.index(TorchGrid.dx(TorchGrid.index(0, 0, 0)), 0, 0));
    }
}
