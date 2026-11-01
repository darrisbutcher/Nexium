package net.storagenexus.essentials.registry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.magnet.MagnetLogic;

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
        PayloadTypeRegistry.clientboundPlay().register(WingsSync.TYPE, WingsSync.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleMagnet.TYPE, (payload, context) -> MagnetLogic.toggle(context.player()));
    }

    private ModPayloads() {}
}
