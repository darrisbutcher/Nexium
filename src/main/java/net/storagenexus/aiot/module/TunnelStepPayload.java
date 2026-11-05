package net.storagenexus.aiot.module;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: the tunnel key was pressed; step the tunnel length of the AIOT in the main hand. */
public record TunnelStepPayload() implements CustomPacketPayload {
    public static final TunnelStepPayload INSTANCE = new TunnelStepPayload();
    public static final Type<TunnelStepPayload> TYPE = new Type<>(StorageNexus.id("tunnel_step"));
    public static final StreamCodec<ByteBuf, TunnelStepPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
