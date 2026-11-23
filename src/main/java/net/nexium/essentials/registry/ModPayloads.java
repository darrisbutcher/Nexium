package net.nexium.essentials.registry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.NexiumEssentials;
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
        public static final Type<ToggleMagnet> TYPE = new Type<>(NexiumEssentials.id("toggle_magnet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMagnet> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the Open Backpack key was pressed. */
    public record OpenBackpack() implements CustomPacketPayload {
        public static final OpenBackpack INSTANCE = new OpenBackpack();
        public static final Type<OpenBackpack> TYPE = new Type<>(NexiumEssentials.id("open_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpack> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the Open Jetpack key was pressed. */
    public record OpenJetpack() implements CustomPacketPayload {
        public static final OpenJetpack INSTANCE = new OpenJetpack();
        public static final Type<OpenJetpack> TYPE = new Type<>(NexiumEssentials.id("open_jetpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenJetpack> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the Jetpack engine key (0) or hover key (1) was pressed. */
    public record JetpackSwitch(int which) implements CustomPacketPayload {
        public static final Type<JetpackSwitch> TYPE = new Type<>(NexiumEssentials.id("jetpack_switch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, JetpackSwitch> STREAM_CODEC =
                net.minecraft.network.codec.ByteBufCodecs.VAR_INT.map(JetpackSwitch::new, JetpackSwitch::which).mapStream(b -> b);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: what another player wears in their wings slot, so it can be drawn on them. */
    public record WingsSync(int entityId, ItemStack wings) implements CustomPacketPayload {
        public static final Type<WingsSync> TYPE = new Type<>(NexiumEssentials.id("wings_sync"));
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
        PayloadTypeRegistry.serverboundPlay().register(OpenJetpack.TYPE, OpenJetpack.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(JetpackSwitch.TYPE, JetpackSwitch.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DiagonalPlacePayload.TYPE, DiagonalPlacePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CycleTradesPayload.TYPE, CycleTradesPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WingsSync.TYPE, WingsSync.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DiagonalPlacePayload.TYPE, (payload, context) ->
                DiagonalPlacement.place(context.player(), payload.hand(), payload.pos(), payload.face(), payload.hit()));
        ServerPlayNetworking.registerGlobalReceiver(CycleTradesPayload.TYPE, (payload, context) -> TradeCycling.cycle(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(OpenBackpack.TYPE, (payload, context) -> Backpacks.openEquipped(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(JetpackSwitch.TYPE, (payload, context) -> net.nexium.jetpack.JetpackFlight.toggle(context.player(), payload.which()));
        ServerPlayNetworking.registerGlobalReceiver(OpenJetpack.TYPE, (payload, context) -> net.nexium.jetpack.JetpackFlight.openWorn(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ToggleMagnet.TYPE, (payload, context) -> MagnetLogic.toggle(context.player()));
    }

    private ModPayloads() {}
}
