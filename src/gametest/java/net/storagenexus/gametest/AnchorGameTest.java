package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.storagenexus.dimension.AnchorRegistry;

/**
 * Dimensional Anchors keep their 3x3 chunks loaded with a ticket of their own, and never touch vanilla's forced-chunk
 * flag, which is one switch per chunk shared with /forceload and other mods.
 * <p>
 * Each test works around a chunk far from every test area (checked first), so it can't affect the chunks the test
 * framework keeps forced.
 */
public class AnchorGameTest {
    /** A chunk this far east of the test is clear of every test area; {@code lane} keeps the tests apart. */
    private static ChunkPos farChunk(GameTestHelper h, int lane) {
        ChunkPos here = ChunkPos.containing(h.absolutePos(BlockPos.ZERO));
        ChunkPos center = new ChunkPos(here.x() + 128, here.z() + lane * 8);
        assertNoneForced(h, center);
        return center;
    }

    private static GlobalPos anchorIn(ServerLevel level, ChunkPos chunk, int y) {
        return GlobalPos.of(level.dimension(), chunk.getMiddleBlockPosition(y));
    }

    private static boolean hasTicket(ServerLevel level, ChunkPos chunk) {
        TicketStorage tickets = level.getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        return tickets.getTickets(chunk.pack()).stream().anyMatch(t -> t.getType() == AnchorRegistry.TICKET);
    }

    @GameTest
    public void anchorLeavesOtherForcedChunksAlone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        AnchorRegistry anchors = AnchorRegistry.get(level.getServer());
        ChunkPos center = farChunk(h, 0);
        ChunkPos other = new ChunkPos(center.x() + 1, center.z()); // inside the anchor's 3x3
        level.setChunkForced(other.x(), other.z(), true); // like /forceload add
        GlobalPos anchor = anchorIn(level, center, 64);

        anchors.put(anchor, false);
        anchors.refreshArea(level, anchor.pos());
        boolean ticketWhilePlaced = hasTicket(level, center);
        boolean centerForced = level.getForceLoadedChunks().contains(center.pack());
        anchors.remove(anchor);
        anchors.refreshArea(level, anchor.pos());
        boolean ticketAfterRemoval = hasTicket(level, center);
        boolean otherStillForced = level.getForceLoadedChunks().contains(other.pack());
        level.setChunkForced(other.x(), other.z(), false); // clean up before asserting

        h.assertTrue(ticketWhilePlaced, "an always-on anchor holds its chunks with its own ticket");
        h.assertFalse(centerForced, "it doesn't use the forced flag");
        h.assertFalse(ticketAfterRemoval, "removing the anchor drops its ticket");
        h.assertTrue(otherStillForced, "removing the anchor left a chunk someone else forced still forced");
        h.succeed();
    }

    @GameTest
    public void anchorsSharingAChunkAndTabletsKeepItLoadedWhileNeeded(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        AnchorRegistry anchors = AnchorRegistry.get(level.getServer());
        ChunkPos center = farChunk(h, 1);
        GlobalPos first = anchorIn(level, center, 64);
        GlobalPos second = anchorIn(level, center, 70);

        anchors.put(first, false);
        anchors.refreshArea(level, first.pos());
        anchors.put(second, false);
        anchors.refreshArea(level, second.pos());
        anchors.remove(first);
        anchors.refreshArea(level, first.pos());
        h.assertTrue(hasTicket(level, center), "the second anchor in the chunk still holds it after the first is removed");
        anchors.remove(second);
        anchors.refreshArea(level, second.pos());
        h.assertFalse(hasTicket(level, center), "with both anchors gone the chunk is released");

        // An on-demand anchor sleeps until a tablet uses something in its area
        GlobalPos onDemand = anchorIn(level, center, 64);
        anchors.put(onDemand, true);
        anchors.refreshArea(level, onDemand.pos());
        h.assertFalse(hasTicket(level, center), "a sleeping on-demand anchor holds nothing");
        Runnable release = anchors.loadOnDemand(level, new ChunkPos(center.x() - 1, center.z() + 1).getMiddleBlockPosition(64));
        h.assertTrue(release != null, "a tablet target in its 3x3 wakes it");
        h.assertTrue(hasTicket(level, center), "while the tablet uses it, it holds its chunks");
        release.run();
        h.assertFalse(hasTicket(level, center), "when the tablet is done, it lets them go");
        anchors.remove(onDemand);
        anchors.refreshArea(level, onDemand.pos());
        h.succeed();
    }

    /** A world from before 1.1.4: its anchors saved as a bare list, and their chunks forced with the vanilla flag. */
    @GameTest
    public void oldWorldsSwapForcedChunksForTicketsOnce(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ChunkPos center = farChunk(h, 2);
        GlobalPos anchor = anchorIn(level, center, 64);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setChunkForced(center.x() + dx, center.z() + dz, true);

        CompoundTag entry = new CompoundTag();
        entry.put("pos", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, anchor).getOrThrow());
        entry.putBoolean("on_demand", false);
        ListTag oldSave = new ListTag();
        oldSave.add(entry);
        AnchorRegistry loaded = AnchorRegistry.TYPE.codec().parse(NbtOps.INSTANCE, oldSave).getOrThrow();
        h.assertTrue(loaded.all().contains(anchor), "the old save loads");

        loaded.reconcile(level.getServer()); // what server start does
        int stillForced = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getForceLoadedChunks().contains(ChunkPos.pack(center.x() + dx, center.z() + dz))) stillForced++;
            }
        }
        boolean ticket = hasTicket(level, center);
        Tag newSave = AnchorRegistry.TYPE.codec().encodeStart(NbtOps.INSTANCE, loaded).getOrThrow();
        AnchorRegistry reloaded = AnchorRegistry.TYPE.codec().parse(NbtOps.INSTANCE, newSave).getOrThrow();
        loaded.remove(anchor);
        loaded.refreshArea(level, anchor.pos()); // clean up the ticket
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setChunkForced(center.x() + dx, center.z() + dz, false);

        h.assertValueEqual(stillForced, 0, "forced chunks left around the old anchor");
        h.assertTrue(ticket, "the anchor holds its chunks with its ticket instead");
        h.assertTrue(newSave instanceof CompoundTag c && c.getBooleanOr("uses_tickets", false), "the new save records that the swap is done");
        h.assertTrue(reloaded.all().contains(anchor), "the new save loads back with its anchor");
        h.succeed();
    }

    /** The far chunks must start out unforced, or a test could release a chunk something else needs. */
    private static void assertNoneForced(GameTestHelper h, ChunkPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long chunk = ChunkPos.pack(center.x() + dx, center.z() + dz);
                h.assertFalse(h.getLevel().getForceLoadedChunks().contains(chunk), "precondition: chunk " + ChunkPos.unpack(chunk) + " isn't forced already");
            }
        }
    }
}
