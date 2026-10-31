package net.storagenexus.drawer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/**
 * Client -> server: one left click on a drawer front slot. Sent once per real mouse press, so
 * clicking as fast as you like takes that many stacks, and holding the button takes just one.
 */
public record DrawerClickPayload(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<DrawerClickPayload> TYPE = new Type<>(StorageNexus.id("drawer_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrawerClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DrawerClickPayload::pos,
            ByteBufCodecs.VAR_INT, DrawerClickPayload::slot,
            DrawerClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
