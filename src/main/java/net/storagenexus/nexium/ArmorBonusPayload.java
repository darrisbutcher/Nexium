package net.storagenexus.nexium;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: a switch in the armor menu was clicked; turn that bonus on or off. */
public record ArmorBonusPayload(ArmorBonus bonus) implements CustomPacketPayload {
    public static final Type<ArmorBonusPayload> TYPE = new Type<>(StorageNexus.id("armor_bonus"));
    public static final StreamCodec<ByteBuf, ArmorBonusPayload> STREAM_CODEC = ArmorBonus.STREAM_CODEC.map(ArmorBonusPayload::new, ArmorBonusPayload::bonus);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
