package net.storagenexus.energy;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.storagenexus.StorageNexus;
import org.jspecify.annotations.Nullable;

/**
 * Something that holds Nexus Energy (NE): generators give it, Energy Conduits and Batteries move and keep it, and
 * farms and the Freezer use it. Amounts are whole NE; rates are NE per tick.
 */
public interface NexusEnergy {
    /** Looks up the energy holder of a block from one of its sides (null for "any side"). */
    BlockApiLookup<NexusEnergy, @Nullable Direction> SIDED =
            BlockApiLookup.get(StorageNexus.id("energy"), NexusEnergy.class, Direction.class);

    /** Puts in up to {@code max} NE and returns how much went in. With {@code simulate}, nothing changes. */
    long insert(long max, boolean simulate);

    /** Takes out up to {@code max} NE and returns how much came out. With {@code simulate}, nothing changes. */
    long extract(long max, boolean simulate);

    long stored();

    long capacity();
}
