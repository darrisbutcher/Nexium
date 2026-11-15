package net.storagenexus.essentials.placement;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.NexusEssentials;

/**
 * Client to server: a right-click with the Diagonal Placement key held, on this face of this block at this point, with the item
 * in this hand. The server places the block diagonally, see {@link DiagonalPlacement}.
 */
public record DiagonalPlacePayload(BlockPos pos, Direction face, Vec3 hit, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<DiagonalPlacePayload> TYPE = new Type<>(NexusEssentials.id("diagonal_place"));
    public static final StreamCodec<ByteBuf, DiagonalPlacePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DiagonalPlacePayload::pos,
            Direction.STREAM_CODEC, DiagonalPlacePayload::face,
            Vec3.STREAM_CODEC, DiagonalPlacePayload::hit,
            InteractionHand.STREAM_CODEC, DiagonalPlacePayload::hand,
            DiagonalPlacePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
