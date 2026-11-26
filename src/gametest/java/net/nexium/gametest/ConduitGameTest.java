package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.nexium.conduit.SideConfig;
import net.nexium.conduit.ConduitBundleBlock;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.registry.ModBlocks;

/** Conduit tiers, channel markers. */
public class ConduitGameTest {
    @GameTest
    public void anEmptyHandOpensTheSideMenu(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        h.getBlockEntity(pos, ConduitBundleBlockEntity.class).addConduit(ConduitType.ITEM, 1);
        var player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        BlockPos abs = h.absolutePos(pos);
        var state = h.getLevel().getBlockState(abs);
        var result = state.useWithoutItem(h.getLevel(), player, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false));
        h.assertTrue(result.consumesAction(), "an empty-handed right-click is used");
        h.assertTrue(player.containerMenu instanceof net.nexium.conduit.ConduitConfigMenu, "and opens the conduit menu, got " + player.containerMenu.getClass().getSimpleName());
        h.succeed();
    }

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

    @GameTest
    public void markersCarryEveryExtractColour(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(pos.east(), Blocks.CHEST);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        be.addConduit(ConduitType.ITEM, 1);
        SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
        cfg.mode = SideConfig.Mode.BOTH;
        cfg.channel = 3;
        cfg.extractChannels = 1 << 0 | 1 << 6 | 1 << 15;
        int marker = ConduitBundleBlockEntity.packMarker(cfg);
        h.assertValueEqual(marker & 15, 3, "insert colour");
        h.assertValueEqual(SideConfig.Mode.values()[marker >> 4 & 3], SideConfig.Mode.BOTH, "mode");
        h.assertValueEqual(marker >>> 6 & 0xFFFF, 1 << 0 | 1 << 6 | 1 << 15, "all three extract colours, the top one too");
        h.succeed();
    }

    @GameTest(maxTicks = 200)
    public void aSideExtractingOnSeveralColoursReachesEachAndNoOther(GameTestHelper h) {
        // Chest A - bundle 1 - bundle 2, with chests B (north), C (south) and D (east) on bundle 2
        BlockPos a = new BlockPos(1, 1, 2), b1 = new BlockPos(2, 1, 2), b2 = new BlockPos(3, 1, 2);
        BlockPos chestB = new BlockPos(3, 1, 1), chestC = new BlockPos(3, 1, 3), chestD = new BlockPos(4, 1, 2);
        h.setBlock(a, Blocks.CHEST);
        h.setBlock(chestB, Blocks.CHEST);
        h.setBlock(chestC, Blocks.CHEST);
        h.setBlock(chestD, Blocks.CHEST);
        h.setBlock(b1, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(b2, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity one = h.getBlockEntity(b1, ConduitBundleBlockEntity.class);
        ConduitBundleBlockEntity two = h.getBlockEntity(b2, ConduitBundleBlockEntity.class);
        one.addConduit(ConduitType.ITEM, 1);
        two.addConduit(ConduitType.ITEM, 1);
        // Chest A only extracts, on orange (1) and magenta (2) at once, taking turns between the receivers
        SideConfig fromA = one.config(ConduitType.ITEM, Direction.WEST);
        fromA.mode = SideConfig.Mode.EXTRACT;
        fromA.extractChannels = 1 << 1 | 1 << 2;
        fromA.roundRobin = true;
        two.config(ConduitType.ITEM, Direction.NORTH).channel = 2; // B inserts on magenta
        two.config(ConduitType.ITEM, Direction.SOUTH).channel = 1; // C inserts on orange
        two.config(ConduitType.ITEM, Direction.EAST).channel = 5;  // D inserts on lime: not one of A's colours
        one.changed();
        two.changed();
        h.getBlockEntity(a, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        h.succeedWhen(() -> {
            int inB = h.getBlockEntity(chestB, ChestBlockEntity.class).countItem(Items.COBBLESTONE);
            int inC = h.getBlockEntity(chestC, ChestBlockEntity.class).countItem(Items.COBBLESTONE);
            h.assertTrue(inB > 0 && inC > 0, "both the magenta and the orange receiver got some: " + inB + " and " + inC);
            h.assertValueEqual(inB + inC, 16, "all of it arrived");
            h.assertValueEqual(h.getBlockEntity(chestD, ChestBlockEntity.class).countItem(Items.COBBLESTONE), 0,
                    "nothing reached the lime receiver, which is not one of its colours");
        });
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
        fromA.setExtractChannel(2);
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
        cfg.setExtractChannel(5);
        var codec = SideConfig.codec(ConduitType.ITEM);
        var json = codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, cfg).getOrThrow().getAsJsonObject();
        json.remove("extract_channel");
        json.remove("extract_channels");
        SideConfig loaded = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        h.assertValueEqual(loaded.extractChannels, 1 << 5, "an old side extracts on the channel it always used");
        h.succeed();
    }

    @GameTest
    public void aSideSavedWithOneExtractChannelKeepsItAndSeveralAreKept(GameTestHelper h) {
        var codec = SideConfig.codec(ConduitType.ITEM);
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        // A side saved by the version before several extract channels (only extract_channel): that one channel
        SideConfig one = new SideConfig(ConduitType.ITEM);
        one.setExtractChannel(9);
        var json = codec.encodeStart(ops, one).getOrThrow().getAsJsonObject();
        json.remove("extract_channels");
        h.assertValueEqual(codec.parse(ops, json).getOrThrow().extractChannels, 1 << 9, "the one extract channel of an older save");
        // Several, saved and loaded again
        SideConfig many = new SideConfig(ConduitType.ITEM);
        many.extractChannels = 1 << 2 | 1 << 7 | 1 << 15;
        SideConfig back = codec.parse(ops, codec.encodeStart(ops, many).getOrThrow()).getOrThrow();
        h.assertValueEqual(back.extractChannels, 1 << 2 | 1 << 7 | 1 << 15, "several extract channels survive a save");
        // The channel older versions would read is the lowest one on
        var oldView = codec.encodeStart(ops, many).getOrThrow().getAsJsonObject();
        h.assertValueEqual(oldView.get("extract_channel").getAsInt(), 2, "and an older version would read the lowest as its one channel");
        // The last one cannot be switched off, any other can
        SideConfig toggled = new SideConfig(ConduitType.ITEM);
        toggled.setExtractChannel(4);
        toggled.toggleExtractChannel(4);
        h.assertValueEqual(toggled.extractChannels, 1 << 4, "the last extract channel stays on");
        toggled.toggleExtractChannel(6);
        h.assertValueEqual(toggled.extractChannels, 1 << 4 | 1 << 6, "another can be switched on");
        toggled.toggleExtractChannel(4);
        h.assertValueEqual(toggled.extractChannels, 1 << 6, "and the first off again");
        h.succeed();
    }
}
