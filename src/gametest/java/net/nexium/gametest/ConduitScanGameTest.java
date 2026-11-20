package net.nexium.gametest;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitScan;
import net.nexium.conduit.ConduitScan.Link;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.ScanReportPayload;
import net.nexium.conduit.SideConfig;
import net.nexium.registry.ModBlocks;

import java.util.List;

/** The Conduit Scanner's report: what each side of a conduit network connects to, its channels and mode, and the warnings. */
public class ConduitScanGameTest {
    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    private static ConduitBundleBlockEntity bundle(GameTestHelper h, BlockPos pos, ConduitType... types) {
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        for (ConduitType t : types) be.addConduit(t, 1);
        return be;
    }

    private static Link linkAt(ConduitScan.Report report, GameTestHelper h, BlockPos rel) {
        return report.links().stream().filter(l -> l.target().equals(h.absolutePos(rel))).findFirst().orElse(null);
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aScanListsEachSideThatEndsAtABlock(GameTestHelper h) {
        ConduitBundleBlockEntity be = bundle(h, CENTER, ConduitType.ITEM);
        BlockPos west = CENTER.west(), east = CENTER.east(), south = CENTER.south(), north = CENTER.north();
        for (BlockPos chest : List.of(west, east, south, north)) h.setBlock(chest, Blocks.CHEST);
        // West takes from its chest onto channel 3; east delivers channel 3 (the pair works); south delivers channel 7 (nothing sends it);
        // north takes onto channel 9 (nothing delivers it)
        SideConfig w = be.config(ConduitType.ITEM, Direction.WEST);
        w.mode = SideConfig.Mode.EXTRACT;
        w.extractChannel = 3;
        SideConfig e = be.config(ConduitType.ITEM, Direction.EAST);
        e.mode = SideConfig.Mode.INSERT;
        e.channel = 3;
        e.priority = 4;
        SideConfig s = be.config(ConduitType.ITEM, Direction.SOUTH);
        s.mode = SideConfig.Mode.INSERT;
        s.channel = 7;
        SideConfig n = be.config(ConduitType.ITEM, Direction.NORTH);
        n.mode = SideConfig.Mode.EXTRACT;
        n.extractChannel = 9;
        be.changed();

        ConduitScan.Report report = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.ITEM);
        h.assertTrue(report != null, "an item conduit has an item report");
        h.assertValueEqual(report.links().size(), 4, "four sides end at a chest");
        h.assertValueEqual(report.conduits(), 1, "one conduit on the network");
        h.assertTrue(!report.truncated(), "nothing was cut short");
        h.assertTrue(report.carries(ConduitType.ITEM) && !report.carries(ConduitType.FLUID) && !report.carries(ConduitType.ENERGY), "it carries items only");

        Link lw = linkAt(report, h, west), le = linkAt(report, h, east), ls = linkAt(report, h, south), ln = linkAt(report, h, north);
        h.assertTrue(lw.mode() == SideConfig.Mode.EXTRACT && lw.outChannel() == 3 && lw.flags() == 0, "west extracts on 3 and has a receiver: " + lw);
        h.assertTrue(le.mode() == SideConfig.Mode.INSERT && le.inChannel() == 3 && le.priority() == 4 && le.flags() == 0, "east inserts on 3 and has a sender: " + le);
        h.assertTrue(ls.has(ConduitScan.NO_SENDER) && !ls.has(ConduitScan.NO_RECEIVER), "south inserts on 7 with nothing extracting there: " + ls);
        h.assertTrue(ln.has(ConduitScan.NO_RECEIVER) && !ln.has(ConduitScan.NO_SENDER), "north extracts on 9 with nothing inserting there: " + ln);
        h.assertTrue(lw.side() == Direction.WEST && lw.conduit().equals(h.absolutePos(CENTER)) && lw.block().getPath().equals("chest"), "it records the side, the conduit and the block");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aBothSideCountsForItsOwnChannelsAndOtherBlocksOnly(GameTestHelper h) {
        ConduitBundleBlockEntity be = bundle(h, CENTER, ConduitType.ITEM);
        h.setBlock(CENTER.west(), Blocks.CHEST);
        h.setBlock(CENTER.east(), Blocks.BARREL);
        SideConfig w = be.config(ConduitType.ITEM, Direction.WEST);
        w.mode = SideConfig.Mode.BOTH;
        w.channel = 2;
        w.extractChannel = 2;
        be.changed();
        // A Both side alone on its channel: it would only be moving things into and out of itself, so both warnings show
        ConduitScan.Report alone = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.ITEM);
        Link lw = linkAt(alone, h, CENTER.west());
        h.assertTrue(lw.has(ConduitScan.NO_RECEIVER) && lw.has(ConduitScan.NO_SENDER), "a Both side with no other block on its channels warns both ways: " + lw);
        // A second block inserting and extracting on channel 2 answers it
        SideConfig e = be.config(ConduitType.ITEM, Direction.EAST);
        e.mode = SideConfig.Mode.BOTH;
        e.channel = 2;
        e.extractChannel = 2;
        be.changed();
        ConduitScan.Report pair = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.ITEM);
        h.assertTrue(linkAt(pair, h, CENTER.west()).flags() == 0 && linkAt(pair, h, CENTER.east()).flags() == 0, "two Both sides on one channel answer each other");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aScanFollowsConduitsAndFlagsRedstone(GameTestHelper h) {
        ConduitBundleBlockEntity a = bundle(h, CENTER, ConduitType.ITEM);
        ConduitBundleBlockEntity b = bundle(h, CENTER.east(), ConduitType.ITEM);
        h.setBlock(CENTER.west(), Blocks.CHEST);
        h.setBlock(CENTER.east().east(), Blocks.CHEST);
        SideConfig held = a.config(ConduitType.ITEM, Direction.WEST);
        held.mode = SideConfig.Mode.EXTRACT;
        held.redstone = SideConfig.RedstoneControl.WITH_SIGNAL;   // only works with a signal, and there is none
        a.changed();
        b.changed();
        ConduitScan.Report report = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.ITEM);
        h.assertValueEqual(report.conduits(), 2, "the scan follows the conduits that connect");
        h.assertValueEqual(report.links().size(), 2, "and finds the chest at each end");
        h.assertTrue(linkAt(report, h, CENTER.west()).has(ConduitScan.REDSTONE_OFF), "a side that wants a signal and has none is held off by redstone");
        h.assertTrue(!linkAt(report, h, CENTER.east().east()).has(ConduitScan.REDSTONE_OFF), "a side that always works is not");
        // Starting from the other conduit finds the same network
        ConduitScan.Report fromOther = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER.east()), ConduitType.ITEM);
        h.assertTrue(fromOther.links().equals(report.links()) && fromOther.conduits() == 2, "from either conduit, the same report");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aTypeTheConduitDoesNotCarryHasNoReport(GameTestHelper h) {
        bundle(h, CENTER, ConduitType.ITEM);
        h.assertTrue(ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.FLUID) == null, "no fluid report for an item conduit");
        h.assertTrue(ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER.north()), ConduitType.ITEM) == null, "and none where there is no conduit");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void energyHasNoChannelsAndShowsPausedSides(GameTestHelper h) {
        ConduitBundleBlockEntity be = bundle(h, CENTER, ConduitType.ITEM, ConduitType.ENERGY);
        h.setBlock(CENTER.west(), ModBlocks.IRON_BATTERY);
        be.config(ConduitType.ENERGY, Direction.WEST).paused = true;
        be.changed();
        ConduitScan.Report report = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.ENERGY);
        h.assertTrue(report != null && report.carries(ConduitType.ITEM) && report.carries(ConduitType.ENERGY), "the bundle's tabs are item and energy");
        Link l = linkAt(report, h, CENTER.west());
        h.assertTrue(l != null && l.inChannel() == -1 && l.outChannel() == -1, "energy has no channels");
        h.assertTrue(l.has(ConduitScan.PAUSED) && !l.has(ConduitScan.NO_RECEIVER) && !l.has(ConduitScan.NO_SENDER), "a paused side is flagged, and no channel warnings are made for energy: " + l);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aReportSurvivesTheTripToTheClient(GameTestHelper h) {
        ConduitBundleBlockEntity be = bundle(h, CENTER, ConduitType.ITEM);
        h.setBlock(CENTER.west(), Blocks.CHEST);
        h.setBlock(CENTER.east(), Blocks.HOPPER);
        be.config(ConduitType.ITEM, Direction.WEST).mode = SideConfig.Mode.EXTRACT;
        be.config(ConduitType.ITEM, Direction.EAST).priority = 7;
        be.changed();
        ConduitScan.Report report = ConduitScan.scan(h.getLevel(), h.absolutePos(CENTER), ConduitType.ITEM);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        ScanReportPayload.STREAM_CODEC.encode(buf, new ScanReportPayload(true, report));
        ScanReportPayload back = ScanReportPayload.STREAM_CODEC.decode(buf);
        h.assertTrue(back.open() && back.report().equals(report), "the report comes out of the packet as it went in: " + back.report());
        h.succeed();
    }
}
