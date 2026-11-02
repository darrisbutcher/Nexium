package net.storagenexus.aiot.module;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: the Vein Mine key was pressed ({@code held}) or let go. */
public record VeinKeyPayload(boolean held) implements CustomPacketPayload {
    public static final Type<VeinKeyPayload> TYPE = new Type<>(StorageNexus.id("vein_key"));
    public static final StreamCodec<ByteBuf, VeinKeyPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(VeinKeyPayload::new, VeinKeyPayload::held);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
