package net.storagenexus.aiot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: the tool menu key was pressed; open the tool menu for the tool in the main hand. */
public record ToolMenuPayload() implements CustomPacketPayload {
    public static final ToolMenuPayload INSTANCE = new ToolMenuPayload();
    public static final Type<ToolMenuPayload> TYPE = new Type<>(StorageNexus.id("tool_menu"));
    public static final StreamCodec<ByteBuf, ToolMenuPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
