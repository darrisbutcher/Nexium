package net.storagenexus.terminal;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.storagenexus.StorageNexus;

/**
 * Client -> server: fill the Crafting Terminal's grid for a recipe chosen in a recipe viewer
 * (JEI or REI "+" button), from the network first and then the inventory.
 */
public record FillGridPayload(ResourceKey<Recipe<?>> recipe, boolean maxItems) implements CustomPacketPayload {
    public static final Type<FillGridPayload> TYPE = new Type<>(StorageNexus.id("fill_grid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FillGridPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), FillGridPayload::recipe,
            ByteBufCodecs.BOOL, FillGridPayload::maxItems,
            FillGridPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
