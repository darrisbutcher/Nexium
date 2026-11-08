package net.storagenexus.network;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.conduit.ConduitBundleBlock;
import net.storagenexus.drawer.DrawerControllerBlockEntity;
import net.storagenexus.filter.FilterData;
import net.storagenexus.util.StorageEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A snapshot of every inventory reachable from a set of connected network blocks.
 * Storage Connectors read all six neighbours; Filtered Connectors read only the block they face.
 * No power, no channels — Tom's Simple Storage style.
 */
public final class StorageNetwork {
    private static final int MAX_NODES = 2048;
    private static final int CACHE_TICKS = 20;

    /** One storage on the network; {@code pos} is the block it was read from (null if none). */
    public record Source<T>(Storage<T> storage, int priority, @Nullable FilterData filter, @Nullable BlockEntity owner,
                            @Nullable BlockPos pos) {
        public boolean valid() {
            return owner == null || !owner.isRemoved();
        }
    }

    private record CacheKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private record CacheEntry(StorageNetwork network, long version, long gameTime) {}

    private static final Map<CacheKey, CacheEntry> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
            return size() > 256;
        }
    };

    private final List<Source<ItemVariant>> items;
    private final List<Source<FluidVariant>> fluids;
    private final int nodeCount;

    private StorageNetwork(List<Source<ItemVariant>> items, List<Source<FluidVariant>> fluids, int nodeCount) {
        this.items = items;
        this.fluids = fluids;
        this.nodeCount = nodeCount;
    }

    public int nodeCount() { return nodeCount; }
    public int inventoryCount() { return items.size() + fluids.size(); }

    /** The network containing the network block at {@code start}, or null if there is none. */
    public static @Nullable StorageNetwork find(Level level, BlockPos start) {
        if (!(level.getBlockState(start).getBlock() instanceof NetworkBlock)) return null;
        CacheKey key = new CacheKey(level.dimension(), start.immutable());
        CacheEntry e = CACHE.get(key);
        long v = StorageEvents.storageVersion();
        long t = level.getGameTime();
        if (e != null && e.version == v && t >= e.gameTime && t - e.gameTime < CACHE_TICKS) return e.network;
        StorageNetwork net = scan(level, start);
        CACHE.put(key, new CacheEntry(net, v, t));
        return net;
    }

    /** Drops every cached network; called when the server stops so nothing outlives its world. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static StorageNetwork scan(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> connectors = new ArrayList<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof StorageConnectorBlock || state.getBlock() instanceof FilteredConnectorBlock) {
                connectors.add(pos);
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (visited.contains(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof NetworkBlock) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        // Inventory faces to read: (inventory pos, side we read from, priority, filter)
        record Face(BlockPos pos, Direction side, int priority, FilterData filter) {}
        List<Face> faces = new ArrayList<>();
        Set<BlockPos> claimedByController = new HashSet<>();
        for (BlockPos c : connectors) {
            BlockState state = level.getBlockState(c);
            ConnectorBlockEntity cbe = level.getBlockEntity(c) instanceof ConnectorBlockEntity x ? x : null;
            int priority = cbe != null ? cbe.priority() : 0;
            FilterData filter = cbe != null ? cbe.filterData() : null;
            List<Direction> dirs = state.getBlock() instanceof FilteredConnectorBlock
                    ? List.of(state.getValue(FilteredConnectorBlock.FACING))
                    : List.of(Direction.values());
            for (Direction d : dirs) {
                BlockPos inv = c.relative(d);
                BlockState invState = level.getBlockState(inv);
                if (invState.getBlock() instanceof NetworkBlock || invState.getBlock() instanceof ConduitBundleBlock) continue;
                if (level.getBlockEntity(inv) instanceof DrawerControllerBlockEntity ctrl) {
                    claimedByController.addAll(ctrl.clusterPositions());
                }
                faces.add(new Face(inv, d.getOpposite(), priority, filter));
            }
        }

        List<Source<ItemVariant>> items = new ArrayList<>();
        List<Source<FluidVariant>> fluids = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        // An inventory touched by two connectors counts once, at the higher of their priorities
        faces.sort(Comparator.comparingInt((Face f) -> f.priority).reversed());
        for (Face f : faces) {
            if (claimedByController.contains(f.pos)) continue;
            if (!seen.add(f.pos)) continue; // one inventory, one entry, even if two connectors touch it
            BlockEntity owner = level.getBlockEntity(f.pos);
            Storage<ItemVariant> is = ItemStorage.SIDED.find(level, f.pos, f.side);
            if (is != null) items.add(new Source<>(is, f.priority, f.filter, owner, f.pos));
            Storage<FluidVariant> fs = FluidStorage.SIDED.find(level, f.pos, f.side);
            if (fs != null) fluids.add(new Source<>(fs, f.priority, f.filter, owner, f.pos));
        }
        // Disk Drives are network blocks themselves: their disks are storage of their own
        for (BlockPos node : visited) {
            if (level.getBlockEntity(node) instanceof net.storagenexus.disk.DiskDriveBlockEntity drive) {
                items.add(new Source<>(drive.itemStorage(), drive.priority(), drive.filterData(), drive, node));
                fluids.add(new Source<>(drive.fluidStorage(), drive.priority(), drive.filterData(), drive, node));
            }
        }
        items.sort(Comparator.comparingInt((Source<ItemVariant> s) -> s.priority).reversed());
        fluids.sort(Comparator.comparingInt((Source<FluidVariant> s) -> s.priority).reversed());
        return new StorageNetwork(items, fluids, visited.size());
    }

    // ---- Items ------------------------------------------------------------------------------

    private static boolean allowsItem(Source<ItemVariant> s, ItemVariant v) {
        return s.filter == null || s.filter.matches(v);
    }

    private static boolean allowsFluid(Source<FluidVariant> s, FluidVariant v) {
        return s.filter == null || s.filter.matches(v);
    }

    private static <T> boolean contains(Storage<T> storage, T resource) {
        for (StorageView<T> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0 && view.getResource().equals(resource)) return true;
        }
        return false;
    }

    /** Inserts by priority, highest first; within a priority, into inventories that already hold the item first. */
    public long insertItem(ItemVariant resource, long maxAmount, TransactionContext tx) {
        return insert(items, resource, maxAmount, tx, s -> allowsItem(s, resource));
    }

    public long extractItem(ItemVariant resource, long maxAmount, TransactionContext tx) {
        return extract(items, resource, maxAmount, tx, s -> allowsItem(s, resource));
    }

    public long insertFluid(FluidVariant resource, long maxAmount, TransactionContext tx) {
        return insert(fluids, resource, maxAmount, tx, s -> allowsFluid(s, resource));
    }

    public long extractFluid(FluidVariant resource, long maxAmount, TransactionContext tx) {
        return extract(fluids, resource, maxAmount, tx, s -> allowsFluid(s, resource));
    }

    /**
     * Fills the highest priority first. Within one priority, inventories that already hold the resource come first, so
     * like items stay together; a lower priority inventory only gets what the higher ones can't take, even if it already
     * holds some. {@code sources} is sorted by priority, highest first.
     */
    private static <T> long insert(List<Source<T>> sources, T resource, long maxAmount, TransactionContext tx,
                                   Predicate<Source<T>> allowed) {
        long inserted = 0;
        for (int start = 0; start < sources.size() && inserted < maxAmount; ) {
            int priority = sources.get(start).priority;
            int end = start;
            while (end < sources.size() && sources.get(end).priority == priority) end++;
            for (int pass = 0; pass < 2 && inserted < maxAmount; pass++) {
                for (int i = start; i < end && inserted < maxAmount; i++) {
                    Source<T> s = sources.get(i);
                    if (!s.valid() || !allowed.test(s) || !s.storage.supportsInsertion()) continue;
                    if (pass == 0 && !contains(s.storage, resource)) continue;
                    inserted += s.storage.insert(resource, maxAmount - inserted, tx);
                }
            }
            start = end;
        }
        return inserted;
    }

    private static <T> long extract(List<Source<T>> sources, T resource, long maxAmount, TransactionContext tx,
                                    Predicate<Source<T>> allowed) {
        long extracted = 0;
        for (int i = sources.size() - 1; i >= 0 && extracted < maxAmount; i--) { // lowest priority first
            Source<T> s = sources.get(i);
            if (!s.valid() || !allowed.test(s) || !s.storage.supportsExtraction()) continue;
            extracted += s.storage.extract(resource, maxAmount - extracted, tx);
        }
        return extracted;
    }

    /** Used and total space, for the terminal's capacity display. Fluids in droplets. */
    public record Usage(long items, long itemCapacity, long fluid, long fluidCapacity) {
        public static final Usage NONE = new Usage(0, 0, 0, 0);
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Usage> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(
                        net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, Usage::items,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, Usage::itemCapacity,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, Usage::fluid,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, Usage::fluidCapacity,
                        Usage::new);
    }

    public Usage usage() {
        long[] item = total(items), fluid = total(fluids);
        return new Usage(item[0], item[1], fluid[0], fluid[1]);
    }

    /** {stored, capacity} over every view, counting each compacting pool once. */
    private static <T> long[] total(List<Source<T>> sources) {
        long stored = 0, capacity = 0;
        for (Source<T> s : sources) {
            if (!s.valid()) continue;
            if (s.storage instanceof ReportsCapacity r) {
                stored += r.storedAmount();
                capacity += r.totalCapacity();
                continue;
            }
            for (StorageView<T> view : s.storage) {
                if (view instanceof net.storagenexus.util.PooledStorageView p && !p.isPrimaryView()) continue;
                stored += Math.max(0, view.getAmount());
                long cap = Math.max(0, view.getCapacity());
                // An empty chest or barrel slot reports the container limit (99); count it as one normal stack
                if (view.isResourceBlank() && s.storage instanceof ContainerStorage) cap = Math.min(cap, 64);
                capacity += cap;
            }
        }
        return new long[]{stored, capacity};
    }

    public List<Source<ItemVariant>> itemSources() { return items; }

    public List<Source<FluidVariant>> fluidSources() { return fluids; }

    /** Every Disk Drive's item disks on the network, as one insert-only storage, by priority and filter. */
    public Storage<ItemVariant> itemDiskTarget() {
        return diskTarget(items, StorageNetwork::allowsItem);
    }

    /** Every Disk Drive's fluid disks on the network, as one insert-only storage, by priority and filter. */
    public Storage<FluidVariant> fluidDiskTarget() {
        return diskTarget(fluids, StorageNetwork::allowsFluid);
    }

    private static <T> Storage<T> diskTarget(List<Source<T>> sources, java.util.function.BiPredicate<Source<T>, T> allows) {
        List<Source<T>> drives = sources.stream().filter(s -> s.storage instanceof net.storagenexus.disk.DriveStorage).toList();
        return new net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage<T>() {
            @Override
            public long insert(T resource, long maxAmount, TransactionContext tx) {
                long inserted = 0;
                for (Source<T> s : drives) {
                    if (inserted >= maxAmount) break;
                    if (!s.valid() || !allows.test(s, resource)) continue;
                    inserted += s.storage.insert(resource, maxAmount - inserted, tx);
                }
                return inserted;
            }
        };
    }

    /** How much of one fluid the network holds, in droplets. */
    public long countFluid(FluidVariant resource) {
        long total = 0;
        for (Source<FluidVariant> s : fluids) {
            if (!s.valid() || !allowsFluid(s, resource)) continue;
            for (StorageView<FluidVariant> view : s.storage) {
                if (!view.isResourceBlank() && view.getResource().equals(resource)) total += view.getAmount();
            }
        }
        return total;
    }

    /** How many of one item the network holds (respecting connector filters). */
    public long countItem(ItemVariant resource) {
        long total = 0;
        for (Source<ItemVariant> s : items) {
            if (!s.valid() || !allowsItem(s, resource)) continue;
            for (StorageView<ItemVariant> view : s.storage) {
                if (!view.isResourceBlank() && view.getResource().equals(resource)) total += view.getAmount();
            }
        }
        return total;
    }

    /** Totals of every item in the network. */
    public Map<ItemVariant, Long> listItems() {
        Map<ItemVariant, Long> out = new HashMap<>();
        for (Source<ItemVariant> s : items) {
            if (!s.valid()) continue;
            for (StorageView<ItemVariant> view : s.storage) {
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                if (!allowsItem(s, view.getResource())) continue;
                out.merge(view.getResource(), view.getAmount(), Long::sum);
            }
        }
        return out;
    }

    public Map<FluidVariant, Long> listFluids() {
        Map<FluidVariant, Long> out = new HashMap<>();
        for (Source<FluidVariant> s : fluids) {
            if (!s.valid()) continue;
            for (StorageView<FluidVariant> view : s.storage) {
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                if (!allowsFluid(s, view.getResource())) continue;
                out.merge(view.getResource(), view.getAmount(), Long::sum);
            }
        }
        return out;
    }
}
