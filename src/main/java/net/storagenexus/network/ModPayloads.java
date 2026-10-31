package net.storagenexus.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.dimension.RenameLinkPayload;
import net.storagenexus.terminal.TerminalActionPayload;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.terminal.TerminalSyncPayload;

public final class ModPayloads {
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TerminalActionPayload.TYPE, TerminalActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RenameLinkPayload.TYPE, RenameLinkPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TerminalActionPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof TerminalMenu menu) {
                menu.handleAction(context.player(), payload);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(RenameLinkPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof DimensionalMenu menu) {
                menu.rename(context.player(), payload.name());
            }
        });
    }

    private ModPayloads() {}
}
