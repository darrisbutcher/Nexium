package net.storagenexus.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Gives a block's energy to the machines touching it. Energy Conduits take theirs by themselves, so they are skipped. */
public final class EnergyPush {
    /** Gives what {@code from} holds to the neighbours that take energy, in turn; batteries only when {@code toBatteries}. */
    public static void push(Level level, BlockPos pos, EnergyBuffer from, boolean toBatteries) {
        for (Direction side : Direction.values()) {
            if (from.stored() <= 0) return;
            BlockPos next = pos.relative(side);
            if (level.getBlockEntity(next) instanceof net.storagenexus.conduit.ConduitBundleBlockEntity) continue;
            if (!toBatteries && level.getBlockEntity(next) instanceof BatteryBlockEntity) continue;
            NexusEnergy target = NexusEnergy.SIDED.find(level, next, side.getOpposite());
            if (target == null) continue;
            long room = target.insert(from.stored(), true);
            if (room > 0) target.insert(from.remove(room, false), false);
        }
    }

    private EnergyPush() {}
}
