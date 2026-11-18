package net.storagenexus.quest;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.storagenexus.StorageNexus;

/** Client to server: send me the quest book's state; {@code open} also opens the book on my screen. */
public record QuestRequestPayload(boolean open) implements CustomPacketPayload {
    public static final Type<QuestRequestPayload> TYPE = new Type<>(StorageNexus.id("quest_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestRequestPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, QuestRequestPayload::open, QuestRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
