package net.nexium.autocraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.nexium.Nexium;

/** Client -> server: fill the open Pattern Encoder from a recipe chosen in JEI (its "+" button). */
public record EncoderFillPayload(ResourceKey<Recipe<?>> recipe) implements CustomPacketPayload {
    public static final Type<EncoderFillPayload> TYPE = new Type<>(Nexium.id("encoder_fill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EncoderFillPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), EncoderFillPayload::recipe, EncoderFillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
