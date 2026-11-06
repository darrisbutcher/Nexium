package net.storagenexus.elevator;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One elevator in a column: its height, its name ("Floor N" when unnamed) and whether there is room to stand on it. */
public record Floor(int y, String name, boolean blocked) {
    public static final StreamCodec<RegistryFriendlyByteBuf, Floor> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Floor::y,
            ByteBufCodecs.stringUtf8(ElevatorBlockEntity.MAX_NAME + 16), Floor::name,
            ByteBufCodecs.BOOL, Floor::blocked,
            Floor::new);
}
