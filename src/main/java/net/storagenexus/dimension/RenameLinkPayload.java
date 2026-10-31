package net.storagenexus.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client -> server: rename the link of the Dimensional Input/Output at pos. */
public record RenameLinkPayload(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<RenameLinkPayload> TYPE = new Type<>(StorageNexus.id("rename_link"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameLinkPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RenameLinkPayload::pos,
            ByteBufCodecs.stringUtf8(32), RenameLinkPayload::name,
            RenameLinkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
