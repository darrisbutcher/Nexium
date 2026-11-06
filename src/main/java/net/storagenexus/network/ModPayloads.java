package net.storagenexus.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.aiot.ToolMenuPayload;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.elevator.ElevatorGoPayload;
import net.storagenexus.elevator.ElevatorOpenPayload;
import net.storagenexus.elevator.ElevatorRenamePayload;
import net.storagenexus.elevator.Elevators;
import net.storagenexus.drawer.DrawerClickPayload;
import net.storagenexus.drawer.DrawerInteraction;
import net.storagenexus.nexium.ArmorBonusPayload;
import net.storagenexus.nexium.NexiumArmor;
import net.storagenexus.dimension.RenameLinkPayload;
import net.storagenexus.terminal.FillGridPayload;
import net.storagenexus.terminal.TerminalActionPayload;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.terminal.TerminalSyncPayload;

public final class ModPayloads {
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TerminalActionPayload.TYPE, TerminalActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RenameLinkPayload.TYPE, RenameLinkPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FillGridPayload.TYPE, FillGridPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DrawerClickPayload.TYPE, DrawerClickPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToolMenuPayload.TYPE, ToolMenuPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.storagenexus.aiot.module.VeinKeyPayload.TYPE, net.storagenexus.aiot.module.VeinKeyPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.storagenexus.aiot.module.AreaTogglePayload.TYPE, net.storagenexus.aiot.module.AreaTogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.storagenexus.aiot.module.TunnelStepPayload.TYPE, net.storagenexus.aiot.module.TunnelStepPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ElevatorOpenPayload.TYPE, ElevatorOpenPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ElevatorGoPayload.TYPE, ElevatorGoPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ElevatorRenamePayload.TYPE, ElevatorRenamePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ElevatorGoPayload.TYPE, (payload, context) -> Elevators.go(context.player(), payload.pos(), payload.y()));
        ServerPlayNetworking.registerGlobalReceiver(ElevatorRenamePayload.TYPE, (payload, context) -> Elevators.rename(context.player(), payload.pos(), payload.name()));
        PayloadTypeRegistry.serverboundPlay().register(ArmorBonusPayload.TYPE, ArmorBonusPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ArmorBonusPayload.TYPE, (payload, context) -> NexiumArmor.toggle(context.player(), payload.bonus()));
        ServerPlayNetworking.registerGlobalReceiver(net.storagenexus.aiot.module.AreaTogglePayload.TYPE, (payload, context) -> ToolMenu.toggleArea(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(net.storagenexus.aiot.module.TunnelStepPayload.TYPE, (payload, context) -> ToolMenu.stepTunnel(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ToolMenuPayload.TYPE, (payload, context) -> ToolMenu.open(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(DrawerClickPayload.TYPE,
                (payload, context) -> DrawerInteraction.handleClick(context.player(), payload));

        ServerPlayNetworking.registerGlobalReceiver(TerminalActionPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof TerminalMenu menu) {
                menu.handleAction(context.player(), payload);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(FillGridPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof TerminalMenu menu) menu.fillGrid(context.player(), payload);
        });
        ServerPlayNetworking.registerGlobalReceiver(RenameLinkPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof DimensionalMenu menu) {
                menu.rename(context.player(), payload.name());
            }
        });
    }

    private ModPayloads() {}
}
