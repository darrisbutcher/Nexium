package net.storagenexus.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client -> server: from the elevator at pos, go to the elevator at height y in the same column. */
public record ElevatorGoPayload(BlockPos pos, int y) implements CustomPacketPayload {
    public static final Type<ElevatorGoPayload> TYPE = new Type<>(StorageNexus.id("elevator_go"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElevatorGoPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ElevatorGoPayload::pos,
            ByteBufCodecs.VAR_INT, ElevatorGoPayload::y,
            ElevatorGoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
