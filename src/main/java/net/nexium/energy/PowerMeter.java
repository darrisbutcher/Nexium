package net.nexium.energy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.EnergyNetwork;
import net.nexium.registry.ModBlocks;

/** The server's side of the Power Meter dashboard: finds the network a meter sits against and answers the screen. */
public final class PowerMeter {
    private static final double REACH_SQR = 12 * 12;

    /** A Power Meter block, or a conduit wearing one as its facade (which works just like a meter against the conduit). */
    public static boolean isMeter(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.POWER_METER)) return true;
        return level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity bundle
                && bundle.facade() != null && bundle.facade().is(ModBlocks.POWER_METER);
    }

    /**
     * The energy network the meter at {@code pos} is against, or null (not a meter, or no Energy Conduit beside it). A conduit
     * wearing the meter as its facade reports on its own network when it carries Energy, else on an Energy Conduit beside it.
     */
    public static EnergyNetwork networkOf(Level level, BlockPos pos) {
        if (!isMeter(level, pos)) return null;
        if (level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity own && own.has(ConduitType.ENERGY)) return EnergyNetwork.of(own);
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(pos.relative(d)) instanceof ConduitBundleBlockEntity bundle && bundle.has(ConduitType.ENERGY)) {
                return EnergyNetwork.of(bundle);
            }
        }
        return null;
    }

    /** Opens the dashboard on this player's screen. */
    public static void open(ServerPlayer player, BlockPos pos) {
        send(player, pos, true);
    }

    /** Sends a fresh report to a screen that is already open. */
    public static void refresh(ServerPlayer player, BlockPos pos) {
        if (reachable(player, pos)) send(player, pos, false);
    }

    /** Sets the priority and off switch of one machine on the meter's network, then sends the new report. */
    public static void control(ServerPlayer player, BlockPos meter, BlockPos machine, int priority, boolean paused) {
        if (!reachable(player, meter)) return;
        EnergyNetwork network = networkOf(player.level(), meter);
        if (network == null || !network.control(player.level(), machine, priority, paused)) return;
        send(player, meter, false);
    }

    private static boolean reachable(ServerPlayer player, BlockPos pos) {
        return isMeter(player.level(), pos)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= REACH_SQR;
    }

    private static void send(ServerPlayer player, BlockPos pos, boolean open) {
        EnergyNetwork network = networkOf(player.level(), pos);
        ServerPlayNetworking.send(player, new PowerReportPayload(pos, open, network == null ? null : network.report(player.level())));
    }

    private PowerMeter() {}
}
