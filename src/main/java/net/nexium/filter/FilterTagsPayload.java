package net.nexium.filter;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.nexium.Nexium;

import java.util.List;

/** Client to server: the tag picker's choice of item tags for the Advanced Filter whose menu is open (checked and saved by {@link FilterMenu#setTags}). */
public record FilterTagsPayload(List<Identifier> tags) implements CustomPacketPayload {
    public static final Type<FilterTagsPayload> TYPE = new Type<>(Nexium.id("filter_tags"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterTagsPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(FilterData.MAX_TAGS)), FilterTagsPayload::tags, FilterTagsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
