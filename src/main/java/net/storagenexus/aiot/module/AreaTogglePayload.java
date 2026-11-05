package net.storagenexus.aiot.module;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: the 3x3 key was pressed; switch the 3x3 Mining Module of the AIOT in the main hand on or off. */
public record AreaTogglePayload() implements CustomPacketPayload {
    public static final AreaTogglePayload INSTANCE = new AreaTogglePayload();
    public static final Type<AreaTogglePayload> TYPE = new Type<>(StorageNexus.id("area_toggle"));
    public static final StreamCodec<ByteBuf, AreaTogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
