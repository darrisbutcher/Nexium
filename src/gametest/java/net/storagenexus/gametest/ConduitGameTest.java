package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.storagenexus.conduit.SideConfig;
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
        h.assertValueEqual(m[ConduitType.ITEM.ordinal()][Direction.EAST.get3DDataValue()],
                ConduitBundleBlockEntity.packMarker(be.config(ConduitType.ITEM, Direction.EAST)), "item channels on the chest side");
        h.assertValueEqual(m[ConduitType.ITEM.ordinal()][Direction.EAST.get3DDataValue()] & 15, 3, "insert channel 3");
        h.assertValueEqual(m[ConduitType.ITEM.ordinal()][Direction.WEST.get3DDataValue()], -1, "nothing on an empty side");
        h.assertValueEqual(m[ConduitType.FLUID.ordinal()][Direction.EAST.get3DDataValue()], -1, "no fluid conduit, no fluid ring");
        h.succeed();
    }

    @GameTest(maxTicks = 100)
    public void extractAndInsertChannelsStaySeparate(GameTestHelper h) {
        // Chest A - bundle 1 - bundle 2, with chest B north of bundle 2 and chest C south of it
        BlockPos a = new BlockPos(1, 1, 2), b1 = new BlockPos(2, 1, 2), b2 = new BlockPos(3, 1, 2);
        BlockPos chestB = new BlockPos(3, 1, 1), chestC = new BlockPos(3, 1, 3);
        h.setBlock(a, Blocks.CHEST);
        h.setBlock(chestB, Blocks.CHEST);
        h.setBlock(chestC, Blocks.CHEST);
        h.setBlock(b1, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(b2, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity one = h.getBlockEntity(b1, ConduitBundleBlockEntity.class);
        ConduitBundleBlockEntity two = h.getBlockEntity(b2, ConduitBundleBlockEntity.class);
        one.addConduit(ConduitType.ITEM, 1);
        two.addConduit(ConduitType.ITEM, 1);
        // Chest A: Both, inserting on orange (1) but extracting on magenta (2)
        SideConfig fromA = one.config(ConduitType.ITEM, Direction.WEST);
        fromA.mode = SideConfig.Mode.BOTH;
        fromA.channel = 1;
        fromA.extractChannel = 2;
        two.config(ConduitType.ITEM, Direction.NORTH).channel = 2; // B inserts on magenta
        two.config(ConduitType.ITEM, Direction.SOUTH).channel = 1; // C inserts on orange
        one.changed();
        two.changed();
        h.getBlockEntity(a, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        h.succeedWhen(() -> {
            h.assertValueEqual(h.getBlockEntity(chestB, ChestBlockEntity.class).countItem(Items.COBBLESTONE), 8,
                    "everything reached the magenta insert side");
            h.assertValueEqual(h.getBlockEntity(chestC, ChestBlockEntity.class).countItem(Items.COBBLESTONE), 0,
                    "nothing reached the orange insert side");
        });
    }

    @GameTest
    public void oldSavesKeepOneChannelForBoth(GameTestHelper h) {
        SideConfig cfg = new SideConfig(ConduitType.ITEM);
        cfg.channel = 5;
        cfg.extractChannel = 5;
        var codec = SideConfig.codec(ConduitType.ITEM);
        var json = codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, cfg).getOrThrow().getAsJsonObject();
        json.remove("extract_channel");
        SideConfig loaded = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        h.assertValueEqual(loaded.extractChannel, 5, "an old side extracts on the channel it always used");
        h.succeed();
    }
}
