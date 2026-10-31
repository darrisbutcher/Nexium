package net.storagenexus.drawer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.util.StorageEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds every drawer connected to it (touching it or each other) within 16 blocks,
 * with the reach restarting at each Controller Slave. Items only go into drawers that
 * already hold (or are locked to) that item.
 */
public class DrawerControllerBlockEntity extends BlockEntity {
    public static final int RANGE = 16;
    private static final int MAX_NODES = 4096;

    private final List<BlockPos> cluster = new ArrayList<>();
    private final Set<BlockPos> clusterSet = new HashSet<>();
    private long builtAtVersion = -1;

    private final ControllerItemStorage itemStorage = new ControllerItemStorage();
    private final ControllerFluidStorage fluidStorage = new ControllerFluidStorage();

    public DrawerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAWER_CONTROLLER, pos, state);
    }

    public Storage<ItemVariant> getItemStorage() { return itemStorage; }
    public Storage<FluidVariant> getFluidStorage() { return fluidStorage; }

    public int drawerCount() {
        refresh();
        return cluster.size();
    }

    /** Every drawer position in the cluster (used by the network to avoid counting drawers twice). */
    public Set<BlockPos> clusterPositions() {
        refresh();
        return Collections.unmodifiableSet(clusterSet);
    }

    private void refresh() {
        if (level == null) return;
        long v = StorageEvents.storageVersion();
        if (v == builtAtVersion) return;
        builtAtVersion = v;
        cluster.clear();
        clusterSet.clear();

        // BFS; each node remembers the controller/slave its reach is measured from.
        Map<BlockPos, BlockPos> anchorOf = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        anchorOf.put(worldPosition, worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && anchorOf.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            BlockPos anchor = anchorOf.get(pos);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (anchorOf.containsKey(next)) continue;
                BlockState state = level.getBlockState(next);
                boolean slave = state.getBlock() instanceof ControllerSlaveBlock;
                boolean drawer = state.getBlock() instanceof FacingDrawerBlock;
                if (!slave && !drawer) continue;
                if (chebyshev(next, anchor) > RANGE) continue;
                anchorOf.put(next, slave ? next : anchor);
                queue.add(next);
                if (drawer) {
                    cluster.add(next);
                    clusterSet.add(next);
                }
            }
        }
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }

    private List<BlockEntity> members() {
        refresh();
        List<BlockEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (BlockPos p : cluster) {
            BlockEntity be = level.getBlockEntity(p);
            if (be != null) out.add(be);
        }
        return out;
    }

    // ---- Player actions -------------------------------------------------------------------

    /** Returns how many items moved. */
    public int insertFromHand(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long n = itemStorage.insert(ItemVariant.of(stack), stack.getCount(), tx);
            tx.commit();
            stack.shrink((int) Math.min(n, stack.getCount()));
            return (int) n;
        }
    }

    public int insertAllFromInventory(Player player) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) total += insertFromHand(s);
        }
        inv.setChanged();
        return total;
    }

    // ---- Combined storages ----------------------------------------------------------------

    private class ControllerItemStorage implements Storage<ItemVariant> {
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
            long inserted = 0;
            for (BlockEntity be : members()) {
                if (inserted >= maxAmount) break;
                if (be instanceof DrawerBlockEntity drawer) {
                    for (int i = 0; i < drawer.slotCount() && inserted < maxAmount; i++) {
                        DrawerBlockEntity.DrawerSlot slot = drawer.slot(i);
                        if (slot.displayVariant().equals(resource)) inserted += slot.insert(resource, maxAmount - inserted, tx);
                    }
                } else if (be instanceof CompactingDrawerBlockEntity compacting) {
                    for (int i = 0; i < compacting.tierCount(); i++) {
                        if (compacting.tier(i).equals(resource)) {
                            inserted += compacting.getItemStorage().insert(resource, maxAmount - inserted, tx);
                            break;
                        }
                    }
                }
            }
            return inserted;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            long extracted = 0;
            for (BlockEntity be : members()) {
                if (extracted >= maxAmount) break;
                Storage<ItemVariant> s = be instanceof DrawerBlockEntity d ? d.getItemStorage()
                        : be instanceof CompactingDrawerBlockEntity c ? c.getItemStorage() : null;
                if (s != null) extracted += s.extract(resource, maxAmount - extracted, tx);
            }
            return extracted;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            List<StorageView<ItemVariant>> views = new ArrayList<>();
            for (BlockEntity be : members()) {
                Storage<ItemVariant> s = be instanceof DrawerBlockEntity d ? d.getItemStorage()
                        : be instanceof CompactingDrawerBlockEntity c ? c.getItemStorage() : null;
                if (s != null) for (StorageView<ItemVariant> v : s) views.add(v);
            }
            return views.iterator();
        }
    }

    private class ControllerFluidStorage implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext tx) {
            long inserted = 0;
            List<FluidDrawerBlockEntity> drawers = new ArrayList<>();
            for (BlockEntity be : members()) if (be instanceof FluidDrawerBlockEntity f) drawers.add(f);
            // First matching slots, then empty ones.
            for (int pass = 0; pass < 2 && inserted < maxAmount; pass++) {
                for (FluidDrawerBlockEntity f : drawers) {
                    for (int i = 0; i < f.slotCount() && inserted < maxAmount; i++) {
                        FluidDrawerBlockEntity.FluidSlot slot = f.slot(i);
                        boolean match = pass == 0 ? slot.displayVariant().equals(resource) : slot.displayVariant().isBlank();
                        if (match) inserted += slot.insert(resource, maxAmount - inserted, tx);
                    }
                }
            }
            return inserted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
            long extracted = 0;
            for (BlockEntity be : members()) {
                if (extracted >= maxAmount) break;
                if (be instanceof FluidDrawerBlockEntity f) extracted += f.getFluidStorage().extract(resource, maxAmount - extracted, tx);
            }
            return extracted;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            List<StorageView<FluidVariant>> views = new ArrayList<>();
            for (BlockEntity be : members()) {
                if (be instanceof FluidDrawerBlockEntity f) for (StorageView<FluidVariant> v : f.getFluidStorage()) views.add(v);
            }
            return views.iterator();
        }
    }
}
