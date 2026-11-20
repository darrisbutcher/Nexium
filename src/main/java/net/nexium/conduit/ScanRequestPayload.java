package net.nexium.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.nexium.Nexium;

/** Client to server: the open Conduit Scanner window wants a fresh report for this conduit and type (an index into {@link ConduitType#VALUES}). */
public record ScanRequestPayload(BlockPos conduit, int conduitType) implements CustomPacketPayload {
    public static final Type<ScanRequestPayload> TYPE = new Type<>(Nexium.id("scan_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScanRequestPayload::conduit, ByteBufCodecs.VAR_INT, ScanRequestPayload::conduitType, ScanRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
