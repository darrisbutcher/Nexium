package net.storagenexus.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.registry.ModBlockEntities;

/** A single 32-bucket fluid store with the same upgrade slots as drawers. */
public class FluidTankBlockEntity extends FluidDrawerBlockEntity {
    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK, pos, state, 1);
    }
}
