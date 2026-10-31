package net.storagenexus.terminal;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.storagenexus.StorageNexus;

import java.util.List;

/** Server -> client: the full contents of the network a terminal is viewing. */
public record TerminalSyncPayload(boolean online, List<ItemEntry> items, List<FluidEntry> fluids) implements CustomPacketPayload {
    public static final Type<TerminalSyncPayload> TYPE = new Type<>(StorageNexus.id("terminal_sync"));

    public record ItemEntry(ItemStack stack, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ItemEntry::stack,
                ByteBufCodecs.VAR_LONG, ItemEntry::count,
                ItemEntry::new);
    }

    public record FluidEntry(Fluid fluid, long amount) {
        public static final StreamCodec<RegistryFriendlyByteBuf, FluidEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.FLUID), FluidEntry::fluid,
                ByteBufCodecs.VAR_LONG, FluidEntry::amount,
                FluidEntry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TerminalSyncPayload::online,
            ItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::items,
            FluidEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::fluids,
            TerminalSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
