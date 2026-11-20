package net.nexium.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.nexium.Nexium;

import java.util.ArrayList;
import java.util.List;

/** Server to client: what a Conduit Scanner window shows. {@code open} makes the client open the window, otherwise it updates the open one. */
public record ScanReportPayload(boolean open, ConduitScan.Report report) implements CustomPacketPayload {
    public static final Type<ScanReportPayload> TYPE = new Type<>(Nexium.id("scan_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanReportPayload> STREAM_CODEC = StreamCodec.of(ScanReportPayload::write, ScanReportPayload::read);

    /** A report this size at most is read, whatever a sender claims. */
    private static final int MAX_READ = ConduitScan.MAX_LINKS * 2;

    private static void write(RegistryFriendlyByteBuf buf, ScanReportPayload p) {
        buf.writeBoolean(p.open);
        ConduitScan.Report r = p.report;
        BlockPos.STREAM_CODEC.encode(buf, r.origin());
        buf.writeByte(r.type().ordinal());
        buf.writeByte(r.typesMask());
        buf.writeVarInt(r.conduits());
        buf.writeBoolean(r.truncated());
        buf.writeVarInt(r.links().size());
        for (ConduitScan.Link l : r.links()) {
            BlockPos.STREAM_CODEC.encode(buf, l.conduit());
            buf.writeByte(l.side().ordinal());
            BlockPos.STREAM_CODEC.encode(buf, l.target());
            Identifier.STREAM_CODEC.encode(buf, l.block());
            buf.writeByte(l.mode().ordinal());
            buf.writeByte(l.inChannel());
            buf.writeByte(l.outChannel());
            buf.writeByte(l.priority());
            buf.writeByte(l.redstone().ordinal());
            buf.writeBoolean(l.filtered());
            buf.writeBoolean(l.roundRobin());
            buf.writeByte(l.speedUpgrades());
            buf.writeByte(l.flags());
        }
    }

    private static ScanReportPayload read(RegistryFriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        BlockPos origin = BlockPos.STREAM_CODEC.decode(buf);
        ConduitType type = ConduitType.VALUES[Math.floorMod(buf.readByte(), ConduitType.VALUES.length)];
        int typesMask = buf.readByte();
        int conduits = buf.readVarInt();
        boolean truncated = buf.readBoolean();
        int n = Math.min(buf.readVarInt(), MAX_READ);
        Direction[] sides = Direction.values();
        SideConfig.Mode[] modes = SideConfig.Mode.values();
        SideConfig.RedstoneControl[] redstones = SideConfig.RedstoneControl.values();
        List<ConduitScan.Link> links = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos conduit = BlockPos.STREAM_CODEC.decode(buf);
            Direction side = sides[Math.floorMod(buf.readByte(), sides.length)];
            BlockPos target = BlockPos.STREAM_CODEC.decode(buf);
            Identifier block = Identifier.STREAM_CODEC.decode(buf);
            SideConfig.Mode mode = modes[Math.floorMod(buf.readByte(), modes.length)];
            int in = buf.readByte(), out = buf.readByte(), priority = buf.readByte();
            SideConfig.RedstoneControl redstone = redstones[Math.floorMod(buf.readByte(), redstones.length)];
            boolean filtered = buf.readBoolean(), roundRobin = buf.readBoolean();
            int speed = buf.readByte(), flags = buf.readByte();
            links.add(new ConduitScan.Link(conduit, side, target, block, mode, in, out, priority, redstone, filtered, roundRobin, speed, flags));
        }
        return new ScanReportPayload(open, new ConduitScan.Report(origin, type, typesMask, conduits, truncated, List.copyOf(links)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
