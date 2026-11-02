package net.storagenexus.aiot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: a button in the tool menu was clicked, for the tool in the main hand. */
public record ToolMenuPayload(Action action) implements CustomPacketPayload {
    public enum Action {
        /** Switch an AIOT between Silk Touch and Fortune. */
        SWAP_SILK_TOUCH_FORTUNE,
        /** Turn a Nexium tool's Haste II on or off. */
        TOGGLE_HASTE
    }

    public static final Type<ToolMenuPayload> TYPE = new Type<>(StorageNexus.id("tool_menu"));
    public static final StreamCodec<ByteBuf, ToolMenuPayload> STREAM_CODEC = ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal)
            .map(ToolMenuPayload::new, ToolMenuPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
