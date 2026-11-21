package net.nexium.energy;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.nexium.Nexium;
import org.jspecify.annotations.Nullable;

/**
 * Something that holds Nexus Energy (NE): generators give it, Energy Conduits and Batteries move and keep it, and
 * farms and the Freezer use it. Amounts are whole NE; rates are NE per tick.
 */
public interface NexusEnergy {
    /** Looks up the energy holder of a block from one of its sides (null for "any side"). */
    BlockApiLookup<NexusEnergy, @Nullable Direction> SIDED =
            BlockApiLookup.get(Nexium.id("energy"), NexusEnergy.class, Direction.class);

    /** Puts in up to {@code max} NE and returns how much went in. With {@code simulate}, nothing changes. */
    long insert(long max, boolean simulate);

    /** Takes out up to {@code max} NE and returns how much came out. With {@code simulate}, nothing changes. */
    long extract(long max, boolean simulate);

    long stored();

    /** Whether energy can be put in from outside (a generator's buffer says no). */
    default boolean takesIn() { return true; }

    /** Whether energy can be taken out from outside (a machine's buffer says no). */
    default boolean givesOut() { return true; }

    long capacity();

    /**
     * Everything that has ever been put in from outside, by anything (a conduit network, a generator touching it), or -1 when this holder
     * does not count. The Power Meter reads how much changed over a second, so what a machine took in shows however it arrived.
     */
    default long totalIn() { return -1; }

    /** The same for what has been taken out from outside (a generator's output, whoever took it), or -1. */
    default long totalOut() { return -1; }
}
