package net.storagenexus.aiot.module;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/**
 * Client to server: the tunnel key was held while the scroll wheel turned; move the tunnel length of the AIOT in the main
 * hand one step up ({@code direction} above 0) or down (below 0).
 */
public record TunnelStepPayload(int direction) implements CustomPacketPayload {
    public static final Type<TunnelStepPayload> TYPE = new Type<>(StorageNexus.id("tunnel_step"));
    public static final StreamCodec<ByteBuf, TunnelStepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TunnelStepPayload::direction, TunnelStepPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
