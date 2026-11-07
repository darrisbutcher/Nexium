package net.storagenexus.drawer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.util.StorageEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Finds every drawer connected to it (touching it or each other) that is within 12 blocks of
 * the controller or of a Controller Slave. Slaves count when they are themselves within reach of
 * the controller or another counted slave, so they can be chained. Items only go into drawers that
 * already hold (or are locked to) that item.
 */
public class DrawerControllerBlockEntity extends BlockEntity {
    public static final int RANGE = 12;
    private static final int MAX_NODES = 4096;

    private final List<BlockPos> cluster = new ArrayList<>();
    private final Set<BlockPos> clusterSet = new HashSet<>();
    /** The controller and every slave in the cluster: each one's reach is measured from it. */
    private final List<BlockPos> anchors = new ArrayList<>();
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
        anchors.clear();
        anchors.add(worldPosition);

        // 1. Everything connected to the controller through touching drawers and slaves.
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> slaves = new ArrayList<>();
        List<BlockPos> drawers = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && seen.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (seen.contains(next)) continue;
                BlockState state = level.getBlockState(next);
                boolean slave = state.getBlock() instanceof ControllerSlaveBlock;
                boolean drawer = state.getBlock() instanceof FacingDrawerBlock;
                if (!slave && !drawer) continue;
                seen.add(next);
                queue.add(next);
                (slave ? slaves : drawers).add(next);
            }
        }
        // 2. Slaves count once they are in reach of the controller or of another counted slave.
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Iterator<BlockPos> it = slaves.iterator(); it.hasNext(); ) {
                BlockPos slave = it.next();
                if (inReach(slave)) {
                    anchors.add(slave);
                    it.remove();
                    grew = true;
                }
            }
        }
        // 3. Drawers in reach of any of them.
        for (BlockPos d : drawers) {
            if (inReach(d)) {
                cluster.add(d);
                clusterSet.add(d);
            }
        }
    }

    private boolean inReach(BlockPos pos) {
        for (BlockPos a : anchors) if (chebyshev(pos, a) <= RANGE) return true;
        return false;
    }

    /** How long the reach outline stays up after sneak + right-clicking the controller. */
    private static final int RANGE_DISPLAY_TICKS = 60 * 20;
    /** Particles last about 3 seconds; redraw a little sooner so the outline stays solid. */
    private static final int RANGE_REDRAW_TICKS = 40;
    private long rangeDisplayUntil = -1;

    /** Starts showing the reach for a minute, or stops if it is showing. Returns whether it is now shown. */
    public boolean toggleRangeDisplay(ServerLevel level) {
        long now = level.getGameTime();
        if (rangeDisplayUntil > now) {
            rangeDisplayUntil = -1;
            return false;
        }
        rangeDisplayUntil = now + RANGE_DISPLAY_TICKS;
        showRange(level);
        return true;
    }

    /** Whether the reach outline is showing. */
    public boolean rangeShown(Level level) {
        return rangeDisplayUntil > level.getGameTime();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrawerControllerBlockEntity be) {
        long now = level.getGameTime();
        if (be.rangeDisplayUntil > now && (be.rangeDisplayUntil - now) % RANGE_REDRAW_TICKS == 0
                && level instanceof ServerLevel server) {
            be.showRange(server);
        }
    }

    /**
     * Outlines the reach of the controller and of every slave with particles (about 3 seconds
     * per call), and marks each connected drawer.
     */
    public void showRange(ServerLevel level) {
        refresh();
        for (BlockPos a : anchors) outline(level, a);
        for (BlockPos d : cluster) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, true, true,
                    d.getX() + 0.5, d.getY() + 1.1, d.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0);
        }
    }

    private static void outline(ServerLevel level, BlockPos anchor) {
        double[] lo = {anchor.getX() - RANGE, anchor.getY() - RANGE, anchor.getZ() - RANGE};
        double[] hi = {anchor.getX() + RANGE + 1, anchor.getY() + RANGE + 1, anchor.getZ() + RANGE + 1};
        double[][] ends = {lo, hi};
        // The 12 edges of the box: for each axis, the 4 lines running along it.
        for (int axis = 0; axis < 3; axis++) {
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            for (double[] eu : ends) {
                for (double[] ev : ends) {
                    for (double t = lo[axis]; t <= hi[axis]; t += 1) {
                        double[] p = new double[3];
                        p[axis] = t;
                        p[u] = eu[u];
                        p[v] = ev[v];
                        level.sendParticles(ParticleTypes.END_ROD, true, true, p[0], p[1], p[2], 1, 0, 0, 0, 0);
                    }
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
