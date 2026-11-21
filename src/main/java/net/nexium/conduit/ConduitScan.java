package net.nexium.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a Conduit Scanner shows: every connection of one conduit network, one type at a time. Starting from a conduit it walks the
 * conduits that connect to each other (as items, fluids or energy are moved) and lists each side that ends at a block, with the
 * side's mode, its channels, priority, redstone setting and filter, and a few warnings: an extractor with nothing inserting on its
 * channel, an inserter with nothing extracting on its channel, a side that redstone is holding off right now, or one that is paused.
 */
public final class ConduitScan {
    /** The most conduits a scan walks, as many as a network moves things across. */
    public static final int MAX_CONDUITS = 1024;
    /** The most connections one report carries, so a huge network cannot make a huge packet. */
    public static final int MAX_LINKS = 400;

    /** Flags on a {@link Link}. */
    public static final int NO_RECEIVER = 1, NO_SENDER = 2, REDSTONE_OFF = 4, PAUSED = 8;

    /**
     * One side of a conduit that ends at a block. {@code inChannel} is the insert colour and {@code outChannels} the set of extract
     * colours (one bit each, bit n is channel n); for energy, which has no channels (every energy conduit shares one pool), they are
     * -1 and 0.
     */
    public record Link(BlockPos conduit, Direction side, BlockPos target, Identifier block, SideConfig.Mode mode, int inChannel,
                       int outChannels, int priority, SideConfig.RedstoneControl redstone, boolean filtered, boolean roundRobin,
                       int speedUpgrades, int flags) {
        public boolean has(int flag) {
            return (flags & flag) != 0;
        }

        /** Whether this side extracts on {@code channel}. */
        public boolean outOn(int channel) {
            return channel >= 0 && (outChannels >> channel & 1) != 0;
        }

        /** The lowest extract colour on, or -1 when there is none (energy). */
        public int firstOut() {
            return outChannels == 0 ? -1 : Integer.numberOfTrailingZeros(outChannels);
        }
    }

    /** {@code typesMask} has a bit for each conduit type the starting conduit carries; {@code truncated} says the network was cut short. */
    public record Report(BlockPos origin, ConduitType type, int typesMask, int conduits, boolean truncated, List<Link> links) {
        public boolean carries(ConduitType t) {
            return (typesMask & (1 << t.ordinal())) != 0;
        }
    }

    /** The bit for {@code type} in a types mask. */
    public static int bit(ConduitType type) {
        return 1 << type.ordinal();
    }

    /** The report for the conduit at {@code start} and the network of {@code type} it belongs to, or null if it is not a conduit that carries that type. */
    public static @Nullable Report scan(Level level, BlockPos start, ConduitType type) {
        if (!(level.getBlockEntity(start) instanceof ConduitBundleBlockEntity first) || !first.has(type)) return null;
        int typesMask = 0;
        for (ConduitType t : ConduitType.VALUES) if (first.has(t)) typesMask |= bit(t);

        Set<BlockPos> seen = new HashSet<>();
        Deque<ConduitBundleBlockEntity> queue = new ArrayDeque<>();
        List<Link> links = new ArrayList<>();
        seen.add(start.immutable());
        queue.add(first);
        boolean truncated = false;
        while (!queue.isEmpty()) {
            ConduitBundleBlockEntity bundle = queue.poll();
            BlockPos pos = bundle.getBlockPos();
            boolean powered = level.hasNeighborSignal(pos);
            for (Direction d : Direction.values()) {
                if (!bundle.connectsTo(type, d)) continue;
                BlockPos next = pos.relative(d);
                if (level.getBlockEntity(next) instanceof ConduitBundleBlockEntity other) {
                    if (seen.size() >= MAX_CONDUITS) truncated = true;
                    else if (seen.add(next.immutable())) queue.add(other);
                    continue;
                }
                if (links.size() >= MAX_LINKS) {
                    truncated = true;
                    continue;
                }
                SideConfig cfg = bundle.config(type, d);
                boolean energy = type == ConduitType.ENERGY;
                int flags = 0;
                if (!cfg.redstone.allows(powered)) flags |= REDSTONE_OFF;
                if (cfg.paused) flags |= PAUSED;
                links.add(new Link(pos.immutable(), d, next.immutable(), BuiltInRegistries.BLOCK.getKey(level.getBlockState(next).getBlock()),
                        cfg.mode, energy ? -1 : cfg.channel, energy ? 0 : cfg.extractChannels, cfg.priority, cfg.redstone,
                        !cfg.filter.isEmpty(), cfg.roundRobin, cfg.speedUpgrades(), flags));
            }
        }
        List<Link> sorted = new ArrayList<>(links);
        sorted.sort(Comparator.comparingLong((Link l) -> l.target().asLong()).thenComparing(l -> l.side().ordinal()));
        if (type != ConduitType.ENERGY) sorted = withDeadEnds(sorted);
        return new Report(start.immutable(), type, typesMask, seen.size(), truncated, List.copyOf(sorted));
    }

    /**
     * Adds the dead-end warnings: an extracting side needs another block that inserts on one of its channels, and an inserting side needs
     * another block that extracts on its channel (the same block does not count: a conduit never moves things from a block back into itself).
     */
    private static List<Link> withDeadEnds(List<Link> links) {
        List<Link> out = new ArrayList<>(links.size());
        for (Link l : links) {
            int flags = l.flags();
            if (l.mode().extracts() && links.stream().noneMatch(o -> !o.target().equals(l.target()) && o.mode().inserts() && l.outOn(o.inChannel()))) {
                flags |= NO_RECEIVER;
            }
            if (l.mode().inserts() && links.stream().noneMatch(o -> !o.target().equals(l.target()) && o.mode().extracts() && o.outOn(l.inChannel()))) {
                flags |= NO_SENDER;
            }
            out.add(flags == l.flags() ? l : new Link(l.conduit(), l.side(), l.target(), l.block(), l.mode(), l.inChannel(), l.outChannels(),
                    l.priority(), l.redstone(), l.filtered(), l.roundRobin(), l.speedUpgrades(), flags));
        }
        return out;
    }

    private ConduitScan() {}
}
