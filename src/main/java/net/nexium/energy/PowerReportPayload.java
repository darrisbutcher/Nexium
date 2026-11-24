package net.nexium.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.nexium.Nexium;
import net.nexium.conduit.EnergyNetwork;

import java.util.ArrayList;
import java.util.List;

/** Server to client: what a Power Meter shows. {@code open} makes the client open the screen, otherwise it updates the open one. A null report means no Energy Conduit beside the meter. */
public record PowerReportPayload(BlockPos meter, boolean open, EnergyNetwork.Report report) implements CustomPacketPayload {
    public static final Type<PowerReportPayload> TYPE = new Type<>(Nexium.id("power_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerReportPayload> STREAM_CODEC = StreamCodec.of(PowerReportPayload::write, PowerReportPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, PowerReportPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.meter);
        buf.writeBoolean(p.open);
        EnergyNetwork.Report r = p.report;
        buf.writeBoolean(r != null);
        if (r == null) return;
        buf.writeLong(r.stored());
        buf.writeLong(r.capacity());
        buf.writeLong(r.batteryStored());
        buf.writeLong(r.batteryCapacity());
        buf.writeFloat(r.produced());
        buf.writeFloat(r.used());
        buf.writeFloat(r.batteryNet());
        buf.writeBoolean(r.deficit());
        buf.writeInt(r.secondsLeft());
        buf.writeVarInt(r.endpoints().size());
        for (EnergyNetwork.Endpoint e : r.endpoints()) {
            BlockPos.STREAM_CODEC.encode(buf, e.pos());
            buf.writeByte(e.role().ordinal());
            buf.writeFloat(e.perTick());
            buf.writeByte(e.priority());
            buf.writeBoolean(e.paused());
            buf.writeBoolean(e.direct());
        }
        buf.writeVarInt(r.producedHistory().length);
        for (float f : r.producedHistory()) buf.writeFloat(f);
        for (float f : r.usedHistory()) buf.writeFloat(f);
    }

    private static PowerReportPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos meter = BlockPos.STREAM_CODEC.decode(buf);
        boolean open = buf.readBoolean();
        if (!buf.readBoolean()) return new PowerReportPayload(meter, open, null);
        long stored = buf.readLong(), capacity = buf.readLong(), batteryStored = buf.readLong(), batteryCapacity = buf.readLong();
        float produced = buf.readFloat(), used = buf.readFloat(), batteryNet = buf.readFloat();
        boolean deficit = buf.readBoolean();
        int secondsLeft = buf.readInt();
        int n = buf.readVarInt();
        List<EnergyNetwork.Endpoint> endpoints = new ArrayList<>(n);
        EnergyNetwork.Role[] roles = EnergyNetwork.Role.values();
        for (int i = 0; i < n; i++) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            EnergyNetwork.Role role = roles[Math.floorMod(buf.readByte(), roles.length)];
            endpoints.add(new EnergyNetwork.Endpoint(pos, role, buf.readFloat(), buf.readByte(), buf.readBoolean(), buf.readBoolean()));
        }
        int h = Math.min(buf.readVarInt(), 4096);
        float[] pHist = new float[h], uHist = new float[h];
        for (int i = 0; i < h; i++) pHist[i] = buf.readFloat();
        for (int i = 0; i < h; i++) uHist[i] = buf.readFloat();
        return new PowerReportPayload(meter, open, new EnergyNetwork.Report(stored, capacity, batteryStored, batteryCapacity,
                produced, used, batteryNet, deficit, secondsLeft, endpoints, pHist, uHist));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
