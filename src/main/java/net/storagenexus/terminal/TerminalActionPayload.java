package net.storagenexus.terminal;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.storagenexus.StorageNexus;

/** Client -> server: a click in the terminal's item or fluid grid. */
public record TerminalActionPayload(int action, ItemStack item, Fluid fluid) implements CustomPacketPayload {
    public static final int TAKE_STACK = 0;
    public static final int TAKE_HALF = 1;
    public static final int TAKE_TO_INVENTORY = 2;
    public static final int INSERT_CARRIED = 3;
    public static final int INSERT_ONE = 4;
    public static final int FILL_BUCKET = 5;
    public static final int EMPTY_BUCKET = 6;

    public static final Type<TerminalActionPayload> TYPE = new Type<>(StorageNexus.id("terminal_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalActionPayload::action,
            ItemStack.OPTIONAL_STREAM_CODEC, TerminalActionPayload::item,
            ByteBufCodecs.registry(Registries.FLUID), TerminalActionPayload::fluid,
            TerminalActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
