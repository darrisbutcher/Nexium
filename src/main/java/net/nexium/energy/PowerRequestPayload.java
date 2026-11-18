package net.nexium.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.nexium.StorageNexus;

/** Client to server: the open Power Meter screen wants a fresh report. */
public record PowerRequestPayload(BlockPos meter) implements CustomPacketPayload {
    public static final Type<PowerRequestPayload> TYPE = new Type<>(StorageNexus.id("power_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerRequestPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, PowerRequestPayload::meter, PowerRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
