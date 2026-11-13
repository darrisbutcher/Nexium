package net.storagenexus.sort;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: sort the open container, or the player's main inventory, A to Z. */
public record SortPayload(int containerId, boolean player) implements CustomPacketPayload {
    public static final Type<SortPayload> TYPE = new Type<>(StorageNexus.id("sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SortPayload::containerId,
            ByteBufCodecs.BOOL, SortPayload::player,
            SortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
