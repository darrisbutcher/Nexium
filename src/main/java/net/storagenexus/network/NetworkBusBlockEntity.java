package net.storagenexus.network;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.filter.FilterData;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The work of an Import/Export Bus: every half second, up to 32 items and a bucket of fluid. */
public class NetworkBusBlockEntity extends BlockEntity {
    private static final int INTERVAL = 10;
    public static final int ITEMS_PER_RUN = 32;
    public static final long FLUID_PER_RUN = FluidConstants.BUCKET;

    private final SimpleContainer filterSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            NetworkBusBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterItem;
        }
    };

    public NetworkBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_BUS, pos, state);
    }

    public SimpleContainer filterContainer() { return filterSlot; }

    public boolean exporting() {
        return getBlockState().getValue(NetworkBusBlock.EXPORT);
    }

    public void toggleMode() {
        if (level != null) level.setBlock(worldPosition, getBlockState().cycle(NetworkBusBlock.EXPORT), 3);
    }

    private @Nullable FilterData filterData() {
        ItemStack s = filterSlot.getItem(0);
        return s.getItem() instanceof FilterItem fi ? fi.data(s) : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NetworkBusBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        be.run(level, pos, state);
    }

    /** One transfer pass; public so tests can run it without waiting for the tick. */
    public void run(Level level, BlockPos pos, BlockState state) {
        Direction face = state.getValue(NetworkBusBlock.FACING);
        BlockPos target = pos.relative(face);
        if (level.getBlockState(target).getBlock() instanceof NetworkBlock) return;
        StorageNetwork net = StorageNetwork.find(level, pos);
        if (net == null) return;
        FilterData filter = filterData();
        boolean export = state.getValue(NetworkBusBlock.EXPORT);

        Storage<ItemVariant> items = ItemStorage.SIDED.find(level, target, face.getOpposite());
        if (items != null) {
            if (export) exportItems(net, items, filter);
            else importItems(net, items, filter);
        }
        Storage<FluidVariant> fluids = FluidStorage.SIDED.find(level, target, face.getOpposite());
        if (fluids != null) {
            if (export) exportFluids(net, fluids, filter);
            else importFluids(net, fluids, filter);
        }
    }

    // ---- Items --------------------------------------------------------------------------------

    private static void importItems(StorageNetwork net, Storage<ItemVariant> source, @Nullable FilterData filter) {
        List<ItemVariant> found = new ArrayList<>();
        for (StorageView<ItemVariant> view : source) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            ItemVariant v = view.getResource();
            if ((filter == null || filter.matches(v)) && !found.contains(v)) found.add(v);
        }
        long budget = ITEMS_PER_RUN;
        for (ItemVariant v : found) {
            if (budget <= 0) return;
            long fits;
            try (Transaction sim = Transaction.openOuter()) {
                fits = net.insertItem(v, budget, sim);
            }
            if (fits <= 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long taken = source.extract(v, fits, tx);
                if (taken > 0 && net.insertItem(v, taken, tx) == taken) {
                    tx.commit();
                    budget -= taken;
                }
            }
        }
    }

    private static void exportItems(StorageNetwork net, Storage<ItemVariant> target, @Nullable FilterData filter) {
        long budget = ITEMS_PER_RUN;
        for (Map.Entry<ItemVariant, Long> e : net.listItems().entrySet()) {
            if (budget <= 0) return;
            ItemVariant v = e.getKey();
            if (filter != null && !filter.matches(v)) continue;
            long want = Math.min(budget, e.getValue());
            long fits;
            try (Transaction sim = Transaction.openOuter()) {
                fits = target.insert(v, want, sim);
            }
            if (fits <= 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long taken = net.extractItem(v, fits, tx);
                if (taken > 0 && target.insert(v, taken, tx) == taken) {
                    tx.commit();
                    budget -= taken;
                }
            }
        }
    }

    // ---- Fluids -------------------------------------------------------------------------------

    private static void importFluids(StorageNetwork net, Storage<FluidVariant> source, @Nullable FilterData filter) {
        for (StorageView<FluidVariant> view : source) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            FluidVariant v = view.getResource();
            if (filter != null && !filter.matches(v)) continue;
            long fits;
            try (Transaction sim = Transaction.openOuter()) {
                fits = net.insertFluid(v, FLUID_PER_RUN, sim);
            }
            if (fits <= 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long taken = source.extract(v, fits, tx);
                if (taken > 0 && net.insertFluid(v, taken, tx) == taken) {
                    tx.commit();
                    return;
                }
            }
        }
    }

    private static void exportFluids(StorageNetwork net, Storage<FluidVariant> target, @Nullable FilterData filter) {
        for (Map.Entry<FluidVariant, Long> e : net.listFluids().entrySet()) {
            FluidVariant v = e.getKey();
            if (filter != null && !filter.matches(v)) continue;
            long want = Math.min(FLUID_PER_RUN, e.getValue());
            long fits;
            try (Transaction sim = Transaction.openOuter()) {
                fits = target.insert(v, want, sim);
            }
            if (fits <= 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long taken = net.extractFluid(v, fits, tx);
                if (taken > 0 && target.insert(v, taken, tx) == taken) {
                    tx.commit();
                    return;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("filter", ItemStack.OPTIONAL_CODEC, filterSlot.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filterSlot.setItem(0, input.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, filterSlot);
    }
}
