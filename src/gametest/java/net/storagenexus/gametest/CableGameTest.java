package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.PipeBlock;
import net.storagenexus.dimension.DimensionalOutputBlock;
import net.storagenexus.registry.ModBlocks;

/** Inventory Cable connections. */
public class CableGameTest {
    private static boolean arm(GameTestHelper h, BlockPos cable, Direction d) {
        return h.getLevel().getBlockState(h.absolutePos(cable)).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d));
    }

    @GameTest
    public void cableConnectsToIntakeAndFacingOutput(GameTestHelper h) {
        BlockPos cable = new BlockPos(2, 1, 2);
        h.setBlock(cable, ModBlocks.INVENTORY_CABLE);
        h.setBlock(cable.east(), ModBlocks.FLUID_INTAKE);
        h.assertTrue(arm(h, cable, Direction.EAST), "cable arm toward a Fluid Intake");
        // Output facing the cable (west, toward it) connects; one facing away does not.
        h.setBlock(cable.north(), ModBlocks.DIMENSIONAL_OUTPUT.defaultBlockState().setValue(DimensionalOutputBlock.FACING, Direction.SOUTH));
        h.assertTrue(arm(h, cable, Direction.NORTH), "cable arm toward an Output facing it");
        h.setBlock(cable.south(), ModBlocks.DIMENSIONAL_OUTPUT.defaultBlockState().setValue(DimensionalOutputBlock.FACING, Direction.SOUTH));
        h.assertTrue(arm(h, cable, Direction.SOUTH), "cable arm toward an Output facing away too (it pushes on every side)");
        h.succeed();
    }
}
