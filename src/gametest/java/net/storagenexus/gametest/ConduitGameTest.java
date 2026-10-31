package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.conduit.ConduitBundleBlock;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.registry.ModBlocks;

/** Conduit tiers, channel markers. */
public class ConduitGameTest {
    @GameTest
    public void tiersShowInBlockState(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        be.addConduit(ConduitType.ITEM, 2);
        be.addConduit(ConduitType.FLUID, 2);
        var state = h.getLevel().getBlockState(h.absolutePos(pos));
        h.assertValueEqual(state.getValue(ConduitBundleBlock.ITEM_TIER), 2, "item tier in block state");
        h.assertValueEqual(state.getValue(ConduitBundleBlock.FLUID_TIER), 2, "fluid tier in block state");
        h.succeed();
    }

    @GameTest
    public void armMarkersReportChannels(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(pos.east(), Blocks.CHEST);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        be.addConduit(ConduitType.ITEM, 1);
        be.config(ConduitType.ITEM, Direction.EAST).channel = 3;
        int[][] m = be.channelMarkers(h.getLevel().getGameTime());
        h.assertValueEqual(m[ConduitType.ITEM.ordinal()][Direction.EAST.get3DDataValue()], 3, "item channel on the chest side");
        h.assertValueEqual(m[ConduitType.ITEM.ordinal()][Direction.WEST.get3DDataValue()], -1, "nothing on an empty side");
        h.assertValueEqual(m[ConduitType.FLUID.ordinal()][Direction.EAST.get3DDataValue()], -1, "no fluid conduit, no fluid ring");
        h.succeed();
    }
}
