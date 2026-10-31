package net.storagenexus.network;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
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

    public record Source<T>(Storage<T> storage, int priority, @Nullable FilterData filter, @Nullable BlockEntity owner) {
        boolean valid() {
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
        if (e != null && e.version == v && t - e.gameTime < CACHE_TICKS) return e.network;
        StorageNetwork net = scan(level, start);
        CACHE.put(key, new CacheEntry(net, v, t));
        return net;
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
        for (Face f : faces) {
            if (claimedByController.contains(f.pos)) continue;
            if (!seen.add(f.pos)) continue; // one inventory, one entry, even if two connectors touch it
            BlockEntity owner = level.getBlockEntity(f.pos);
            Storage<ItemVariant> is = ItemStorage.SIDED.find(level, f.pos, f.side);
            if (is != null) items.add(new Source<>(is, f.priority, f.filter, owner));
            Storage<FluidVariant> fs = FluidStorage.SIDED.find(level, f.pos, f.side);
            if (fs != null) fluids.add(new Source<>(fs, f.priority, f.filter, owner));
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

    /** Inserts preferring higher priority and inventories that already hold the item. */
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

    private static <T> long insert(List<Source<T>> sources, T resource, long maxAmount, TransactionContext tx,
                                   Predicate<Source<T>> allowed) {
        long inserted = 0;
        for (int pass = 0; pass < 2 && inserted < maxAmount; pass++) {
            for (Source<T> s : sources) {
                if (inserted >= maxAmount) break;
                if (!s.valid() || !allowed.test(s) || !s.storage.supportsInsertion()) continue;
                if (pass == 0 && !contains(s.storage, resource)) continue;
                inserted += s.storage.insert(resource, maxAmount - inserted, tx);
            }
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
