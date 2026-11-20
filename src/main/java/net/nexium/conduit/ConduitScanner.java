package net.nexium.conduit;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The server's side of the Conduit Scanner window: opens it from the item, and answers the open window's requests for a fresh report. */
public final class ConduitScanner {
    private static final double REACH_SQR = 16 * 16;
    /** Ticks a player waits between reports: a scan walks a whole network, so a modified client cannot ask for one every tick. */
    private static final int MIN_INTERVAL = 10;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST.remove(handler.getPlayer().getUUID()));
    }

    /** Opens the window for the conduit at {@code pos}, on the first type it carries (item, fluid, then energy). */
    public static void open(ServerPlayer player, BlockPos pos) {
        for (ConduitType type : ConduitType.VALUES) {
            ConduitScan.Report report = ConduitScan.scan(player.level(), pos, type);
            if (report != null) {
                ServerPlayNetworking.send(player, new ScanReportPayload(true, report));
                return;
            }
        }
    }

    /** Sends a fresh report to a window that is already open, if the player is near enough and has not just had one. */
    public static void refresh(ServerPlayer player, BlockPos pos, int typeIndex) {
        if (typeIndex < 0 || typeIndex >= ConduitType.VALUES.length) return;
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH_SQR) return;
        long now = player.level().getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < MIN_INTERVAL && now >= last) return;
        ConduitScan.Report report = ConduitScan.scan(player.level(), pos, ConduitType.VALUES[typeIndex]);
        if (report == null) return;
        LAST.put(player.getUUID(), now);
        ServerPlayNetworking.send(player, new ScanReportPayload(false, report));
    }

    private ConduitScanner() {}
}
