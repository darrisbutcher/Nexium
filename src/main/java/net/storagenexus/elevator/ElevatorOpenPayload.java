package net.storagenexus.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

import java.util.List;

/** Server -> client: open the elevator screen for the elevator at pos, with every floor in its column (lowest first). */
public record ElevatorOpenPayload(BlockPos pos, String ownName, List<Floor> floors) implements CustomPacketPayload {
    public static final Type<ElevatorOpenPayload> TYPE = new Type<>(StorageNexus.id("elevator_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElevatorOpenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ElevatorOpenPayload::pos,
            ByteBufCodecs.stringUtf8(ElevatorBlockEntity.MAX_NAME), ElevatorOpenPayload::ownName,
            Floor.STREAM_CODEC.apply(ByteBufCodecs.list()), ElevatorOpenPayload::floors,
            ElevatorOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
