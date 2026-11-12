package net.storagenexus.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.storagenexus.energy.BatteryBlockEntity;
import net.storagenexus.energy.NexusEnergy;
import net.storagenexus.util.StorageEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The energy of a run of touching Energy Conduits, shared like Mekanism's cable networks: every conduit adds to one pool
 * (a Tier 1 segment holds {@value #CAPACITY_TIER_1} NE, a Tier 2 segment {@value #CAPACITY_TIER_2}) and the pool, not the
 * individual conduit, is what generators fill and machines draw from. The energy is kept in the conduits themselves, so it
 * survives the run being cut or joined; breaking a conduit loses its share.
 *
 * <p>Each tick the network (once, whichever member ticks first) pulls from the generators and batteries on its extracting
 * sides, then gives to the machines on its inserting sides: higher priority first, equal priorities sharing evenly.
 * Batteries come last and only take what is above half of the pool, so a machine is never starved by a battery filling.
 */
public final class EnergyNetwork {
    public static final long CAPACITY_TIER_1 = 2_000, CAPACITY_TIER_2 = 16_000;

    private static final int MAX_MEMBERS = 4096;
    private static final Map<ConduitBundleBlockEntity, EnergyNetwork> NETWORKS = new WeakHashMap<>();

    private final List<ConduitBundleBlockEntity> members;
    private final long version;
    private long lastTick = Long.MIN_VALUE;

    private EnergyNetwork(List<ConduitBundleBlockEntity> members, long version) {
        this.members = members;
        this.version = version;
    }

    /** The network this conduit belongs to, built again when any conduit has changed. */
    public static EnergyNetwork of(ConduitBundleBlockEntity start) {
        long version = StorageEvents.conduitVersion();
        EnergyNetwork known = NETWORKS.get(start);
        if (known != null && known.version == version) return known;
        List<ConduitBundleBlockEntity> found = new ArrayList<>();
        Set<ConduitBundleBlockEntity> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<ConduitBundleBlockEntity> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && found.size() < MAX_MEMBERS) {
            ConduitBundleBlockEntity b = queue.poll();
            found.add(b);
            for (Direction d : Direction.values()) {
                if (!b.connectsTo(ConduitType.ENERGY, d)) continue;
                if (b.getLevel() != null && b.getLevel().getBlockEntity(b.getBlockPos().relative(d)) instanceof ConduitBundleBlockEntity next
                        && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        EnergyNetwork network = new EnergyNetwork(found, version);
        for (ConduitBundleBlockEntity b : found) NETWORKS.put(b, network);
        return network;
    }

    public int size() { return members.size(); }

    /** NE in the pool. */
    public long stored() {
        long sum = 0;
        for (ConduitBundleBlockEntity m : members) sum += m.energyStored();
        return sum;
    }

    /** NE the pool holds when full. */
    public long capacity() {
        long sum = 0;
        for (ConduitBundleBlockEntity m : members) sum += m.energyCapacity();
        return sum;
    }

    /** Puts up to {@code amount} into the pool and returns how much fitted. */
    public long insert(long amount) {
        long left = amount;
        for (ConduitBundleBlockEntity m : members) {
            if (left <= 0) break;
            left -= m.addEnergy(left);
        }
        return amount - left;
    }

    /** Takes up to {@code amount} out of the pool and returns how much came out. */
    public long extract(long amount) {
        long left = amount;
        for (ConduitBundleBlockEntity m : members) {
            if (left <= 0) break;
            left -= m.removeEnergy(left);
        }
        return amount - left;
    }

    private record Side(ConduitBundleBlockEntity member, Direction dir, BlockPos pos, int priority, long rate, boolean battery) {}

    /** Runs one tick of the network, once however many of its conduits call it. */
    public void tick(Level level) {
        long now = level.getGameTime();
        if (now == lastTick) return;
        lastTick = now;
        List<Side> sources = new ArrayList<>();
        List<Side> sinks = new ArrayList<>();
        for (ConduitBundleBlockEntity m : members) {
            if (m.isRemoved()) continue;
            boolean powered = level.hasNeighborSignal(m.getBlockPos());
            long rate = m.tier(ConduitType.ENERGY) >= 2 ? ConduitBundleBlockEntity.ENERGY_TIER_2 : ConduitBundleBlockEntity.ENERGY_TIER_1;
            for (Direction d : Direction.values()) {
                BlockPos next = m.getBlockPos().relative(d);
                if (level.getBlockEntity(next) instanceof ConduitBundleBlockEntity || !m.connectsTo(ConduitType.ENERGY, d)) continue;
                SideConfig cfg = m.config(ConduitType.ENERGY, d);
                boolean battery = level.getBlockEntity(next) instanceof BatteryBlockEntity;
                if (cfg.mode.extracts() && cfg.redstone.allows(powered)) sources.add(new Side(m, d, next, cfg.priority, rate, battery));
                if (cfg.mode.inserts()) sinks.add(new Side(m, d, next, cfg.priority, rate, battery));
            }
        }
        if (sources.isEmpty() && sinks.isEmpty()) return;

        // Fill the pool from the generators and batteries
        Set<BlockPos> feeding = new HashSet<>();
        for (Side s : sources) {
            NexusEnergy source = NexusEnergy.SIDED.find(level, s.pos, s.dir.getOpposite());
            if (source == null) continue;
            long take = Math.min(s.rate, source.extract(s.rate, true));
            if (take <= 0) continue;
            long fits = insert(take);
            if (fits <= 0) continue;
            long got = source.extract(fits, false);
            if (got < fits) extract(fits - got); // the source gave less than it promised: hand the rest back
            feeding.add(s.pos);
        }

        // Give it to the machines, then to batteries
        long capacity = capacity();
        List<Side> machines = new ArrayList<>(), batteries = new ArrayList<>();
        for (Side s : sinks) {
            if (feeding.contains(s.pos)) continue; // never feed a block the pool just took from
            (s.battery ? batteries : machines).add(s);
        }
        machines.sort(Comparator.comparingInt((Side s) -> -s.priority));
        int i = 0;
        while (i < machines.size()) {
            int j = i;
            while (j < machines.size() && machines.get(j).priority == machines.get(i).priority) j++;
            give(level, machines.subList(i, j), 0);
            i = j;
        }
        // Batteries take only what is above half of the pool
        give(level, batteries, capacity / 2);
    }

    /** Shares the pool, above {@code keep}, evenly between these sides, up to each one's rate. */
    private void give(Level level, List<Side> group, long keep) {
        if (group.isEmpty()) return;
        long[] left = new long[group.size()];
        for (int k = 0; k < left.length; k++) left[k] = group.get(k).rate;
        for (int pass = 0; pass < 4; pass++) {
            long pool = stored() - keep;
            if (pool <= 0) return;
            int active = 0;
            for (long l : left) if (l > 0) active++;
            if (active == 0) return;
            long share = Math.max(1, pool / active);
            boolean any = false;
            for (int k = 0; k < left.length; k++) {
                if (left[k] <= 0) continue;
                Side s = group.get(k);
                NexusEnergy dest = NexusEnergy.SIDED.find(level, s.pos, s.dir.getOpposite());
                if (dest == null) { left[k] = 0; continue; }
                long want = Math.min(Math.min(share, left[k]), Math.max(0, stored() - keep));
                long room = dest.insert(want, true);
                if (room <= 0) { left[k] = 0; continue; }
                long taken = extract(room);
                long moved = dest.insert(taken, false);
                if (moved < taken) insert(taken - moved);
                left[k] -= moved;
                if (moved > 0) any = true;
            }
            if (!any) return;
        }
    }
}
