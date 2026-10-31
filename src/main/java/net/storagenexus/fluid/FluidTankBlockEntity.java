package net.storagenexus.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.registry.ModBlockEntities;

/** A single fluid store holding four times a fluid drawer (128 buckets), with the same upgrade slots. */
public class FluidTankBlockEntity extends FluidDrawerBlockEntity {
    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK, pos, state, 1);
    }

    @Override
    protected long baseCapacity() {
        return 4 * BASE_CAPACITY;
    }
}
