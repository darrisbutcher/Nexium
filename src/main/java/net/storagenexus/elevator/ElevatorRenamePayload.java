package net.storagenexus.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client -> server: name the floor of the elevator at pos (an empty name goes back to "Floor N"). */
public record ElevatorRenamePayload(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<ElevatorRenamePayload> TYPE = new Type<>(StorageNexus.id("elevator_rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElevatorRenamePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ElevatorRenamePayload::pos,
            ByteBufCodecs.stringUtf8(ElevatorBlockEntity.MAX_NAME), ElevatorRenamePayload::name,
            ElevatorRenamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
