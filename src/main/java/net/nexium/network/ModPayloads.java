package net.nexium.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.nexium.aiot.ToolMenu;
import net.nexium.aiot.ToolMenuPayload;
import net.nexium.dimension.DimensionalMenu;
import net.nexium.elevator.ElevatorGoPayload;
import net.nexium.elevator.ElevatorOpenPayload;
import net.nexium.elevator.ElevatorRenamePayload;
import net.nexium.elevator.Elevators;
import net.nexium.drawer.DrawerClickPayload;
import net.nexium.drawer.DrawerInteraction;
import net.nexium.nexium.ArmorBonusPayload;
import net.nexium.nexium.NexiumArmor;
import net.nexium.dimension.RenameLinkPayload;
import net.nexium.terminal.FillGridPayload;
import net.nexium.terminal.TerminalActionPayload;
import net.nexium.terminal.TerminalMenu;
import net.nexium.terminal.TerminalSyncPayload;

public final class ModPayloads {
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(net.nexium.energy.PowerReportPayload.TYPE, net.nexium.energy.PowerReportPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.nexium.energy.PowerRequestPayload.TYPE, net.nexium.energy.PowerRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.nexium.energy.PowerControlPayload.TYPE, net.nexium.energy.PowerControlPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(net.nexium.energy.PowerRequestPayload.TYPE,
                (payload, context) -> net.nexium.energy.PowerMeter.refresh(context.player(), payload.meter()));
        ServerPlayNetworking.registerGlobalReceiver(net.nexium.energy.PowerControlPayload.TYPE,
                (payload, context) -> net.nexium.energy.PowerMeter.control(context.player(), payload.meter(), payload.machine(), payload.priority(), payload.paused()));
        PayloadTypeRegistry.serverboundPlay().register(TerminalActionPayload.TYPE, TerminalActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RenameLinkPayload.TYPE, RenameLinkPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FillGridPayload.TYPE, FillGridPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DrawerClickPayload.TYPE, DrawerClickPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToolMenuPayload.TYPE, ToolMenuPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.nexium.sort.SortPayload.TYPE, net.nexium.sort.SortPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(net.nexium.sort.SortPayload.TYPE, (payload, context) ->
                net.nexium.sort.InventorySorter.sort(context.player(), payload.containerId(), payload.player()));
        PayloadTypeRegistry.serverboundPlay().register(net.nexium.aiot.module.VeinKeyPayload.TYPE, net.nexium.aiot.module.VeinKeyPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.nexium.aiot.module.AreaTogglePayload.TYPE, net.nexium.aiot.module.AreaTogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.nexium.aiot.module.TunnelStepPayload.TYPE, net.nexium.aiot.module.TunnelStepPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ElevatorOpenPayload.TYPE, ElevatorOpenPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ElevatorGoPayload.TYPE, ElevatorGoPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ElevatorRenamePayload.TYPE, ElevatorRenamePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ElevatorGoPayload.TYPE, (payload, context) -> Elevators.go(context.player(), payload.pos(), payload.y()));
        ServerPlayNetworking.registerGlobalReceiver(ElevatorRenamePayload.TYPE, (payload, context) -> Elevators.rename(context.player(), payload.pos(), payload.name()));
        PayloadTypeRegistry.serverboundPlay().register(ArmorBonusPayload.TYPE, ArmorBonusPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ArmorBonusPayload.TYPE, (payload, context) -> NexiumArmor.toggle(context.player(), payload.bonus()));
        ServerPlayNetworking.registerGlobalReceiver(net.nexium.aiot.module.AreaTogglePayload.TYPE, (payload, context) -> ToolMenu.toggleArea(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(net.nexium.aiot.module.TunnelStepPayload.TYPE, (payload, context) -> ToolMenu.stepTunnel(context.player(), Integer.signum(payload.direction())));
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
