package net.nexium.essentials.registry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.NexusEssentials;
import net.nexium.essentials.backpack.Backpacks;
import net.nexium.essentials.magnet.MagnetLogic;
import net.nexium.essentials.placement.DiagonalPlacePayload;
import net.nexium.essentials.placement.DiagonalPlacement;
import net.nexium.essentials.trading.CycleTradesPayload;
import net.nexium.essentials.trading.TradeCycling;

public final class ModPayloads {
    /** Client to server: the magnet hotkey was pressed. */
    public record ToggleMagnet() implements CustomPacketPayload {
        public static final ToggleMagnet INSTANCE = new ToggleMagnet();
        public static final Type<ToggleMagnet> TYPE = new Type<>(NexusEssentials.id("toggle_magnet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMagnet> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the Open Backpack key was pressed. */
    public record OpenBackpack() implements CustomPacketPayload {
        public static final OpenBackpack INSTANCE = new OpenBackpack();
        public static final Type<OpenBackpack> TYPE = new Type<>(NexusEssentials.id("open_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpack> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: what another player wears in their wings slot, so it can be drawn on them. */
    public record WingsSync(int entityId, ItemStack wings) implements CustomPacketPayload {
        public static final Type<WingsSync> TYPE = new Type<>(NexusEssentials.id("wings_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WingsSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, WingsSync::entityId,
                ItemStack.OPTIONAL_STREAM_CODEC, WingsSync::wings,
                WingsSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(ToggleMagnet.TYPE, ToggleMagnet.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenBackpack.TYPE, OpenBackpack.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DiagonalPlacePayload.TYPE, DiagonalPlacePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CycleTradesPayload.TYPE, CycleTradesPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WingsSync.TYPE, WingsSync.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DiagonalPlacePayload.TYPE, (payload, context) ->
                DiagonalPlacement.place(context.player(), payload.hand(), payload.pos(), payload.face(), payload.hit()));
        ServerPlayNetworking.registerGlobalReceiver(CycleTradesPayload.TYPE, (payload, context) -> TradeCycling.cycle(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(OpenBackpack.TYPE, (payload, context) -> Backpacks.openEquipped(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ToggleMagnet.TYPE, (payload, context) -> MagnetLogic.toggle(context.player()));
    }

    private ModPayloads() {}
}
