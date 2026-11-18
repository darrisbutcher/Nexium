package net.nexium.essentials.trading;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.nexium.essentials.NexiumEssentials;

/** Client to server: the Cycle Trades key was pressed in a villager's trading screen. */
public record CycleTradesPayload() implements CustomPacketPayload {
    public static final CycleTradesPayload INSTANCE = new CycleTradesPayload();
    public static final Type<CycleTradesPayload> TYPE = new Type<>(NexiumEssentials.id("cycle_trades"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleTradesPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
