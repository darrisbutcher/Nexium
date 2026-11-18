package net.nexium.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.nexium.StorageNexus;

/** Client to server: set one machine's priority (-10 to 10, higher is served first) and off switch from the Power Meter. */
public record PowerControlPayload(BlockPos meter, BlockPos machine, int priority, boolean paused) implements CustomPacketPayload {
    public static final Type<PowerControlPayload> TYPE = new Type<>(StorageNexus.id("power_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PowerControlPayload::meter,
            BlockPos.STREAM_CODEC, PowerControlPayload::machine,
            ByteBufCodecs.VAR_INT, PowerControlPayload::priority,
            ByteBufCodecs.BOOL, PowerControlPayload::paused,
            PowerControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
