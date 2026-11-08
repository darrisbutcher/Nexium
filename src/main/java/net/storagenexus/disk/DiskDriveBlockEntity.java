package net.storagenexus.disk;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.drawer.ControllerSlaveBlock;
import net.storagenexus.drawer.DrawerControllerBlock;
import net.storagenexus.drawer.FacingDrawerBlock;
import net.storagenexus.filter.FilterData;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.util.StorageEvents;
import net.storagenexus.util.SyncedBlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Disk Drive: 8 storage disks, a priority (above chests by default, so new items go to disks first) and a filter
 * slot. The network reads its item disks and fluid disks as two storages ({@link DriveStorage}). The disks' tooltips
 * and the lights on the front are refreshed from their contents twice a second.
 */
public class DiskDriveBlockEntity extends SyncedBlockEntity {
    public static final int SLOTS = 8;
    /** New drives start above a connector's default of 0. */
    public static final int DEFAULT_PRIORITY = 10;

    private final SimpleContainer disks = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            disksChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof StorageDiskItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final SimpleContainer filterSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            DiskDriveBlockEntity.this.setChanged();
            StorageEvents.storageChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterItem;
        }
    };
    private int priority = DEFAULT_PRIORITY;
    private final int[] seenVersion = new int[SLOTS];
    private final DriveStorage<ItemVariant> itemStorage = new DriveStorage<>(this::itemDisks);
    private final DriveStorage<FluidVariant> fluidStorage = new DriveStorage<>(this::fluidDisks);

    public DiskDriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISK_DRIVE, pos, state);
    }

    public SimpleContainer disks() { return disks; }

    public SimpleContainer filterContainer() { return filterSlot; }

    public int priority() { return priority; }

    public void setPriority(int value) {
        priority = Math.max(net.storagenexus.network.ConnectorBlockEntity.MIN_PRIORITY,
                Math.min(net.storagenexus.network.ConnectorBlockEntity.MAX_PRIORITY, value));
        setChanged();
        StorageEvents.storageChanged();
    }

    public @Nullable FilterData filterData() {
        ItemStack s = filterSlot.getItem(0);
        return s.getItem() instanceof FilterItem fi ? fi.data(s) : null;
    }

    public Storage<ItemVariant> itemStorage() { return itemStorage; }

    public Storage<FluidVariant> fluidStorage() { return fluidStorage; }

    private List<DiskContents<ItemVariant>> itemDisks() {
        List<DiskContents<ItemVariant>> out = new ArrayList<>();
        if (!(level instanceof ServerLevel server)) return out;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = disks.getItem(i);
            if (s.getItem() instanceof StorageDiskItem d && !d.isFluid()) out.add(StorageDiskItem.items(server.getServer(), s));
        }
        return out;
    }

    private List<DiskContents<FluidVariant>> fluidDisks() {
        List<DiskContents<FluidVariant>> out = new ArrayList<>();
        if (!(level instanceof ServerLevel server)) return out;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = disks.getItem(i);
            if (s.getItem() instanceof StorageDiskItem d && d.isFluid()) out.add(StorageDiskItem.fluids(server.getServer(), s));
        }
        return out;
    }

    private void disksChanged() {
        if (level instanceof ServerLevel) {
            for (int i = 0; i < SLOTS; i++) {
                seenVersion[i] = -1;
                ItemStack s = disks.getItem(i);
                if (s.getItem() instanceof StorageDiskItem) StorageDiskItem.ensureId(s);
            }
            refreshUsage();
        }
        StorageEvents.storageChanged();
        sync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DiskDriveBlockEntity be) {
        if (level.getGameTime() % 10 == 0 && be.refreshUsage()) be.sync();
    }

    /** Writes each disk's current usage on it when it changed. Returns whether any did. */
    private boolean refreshUsage() {
        if (!(level instanceof ServerLevel server)) return false;
        boolean changed = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = disks.getItem(i);
            if (!(s.getItem() instanceof StorageDiskItem d)) continue;
            DiskContents<?> c = d.isFluid() ? StorageDiskItem.fluids(server.getServer(), s) : StorageDiskItem.items(server.getServer(), s);
            if (c.version() == seenVersion[i]) continue;
            seenVersion[i] = c.version();
            DiskUsage usage = new DiskUsage(c.stored(), c.types());
            if (!usage.equals(s.get(ModComponents.DISK_USAGE))) {
                s.set(ModComponents.DISK_USAGE, usage);
                changed = true;
            }
        }
        return changed;
    }

    /** Result of {@link #moveToDisks}: items moved, and fluid moved in droplets. */
    public record Moved(long items, long fluid) {}

    /**
     * Moves everything the network's other storage holds (chests, barrels and the like, never drawers or other drives'
     * disks) onto the disks of every Disk Drive on the network, by their priority and filters.
     */
    public Moved moveToDisks() {
        if (!(level instanceof ServerLevel server)) return new Moved(0, 0);
        StorageNetwork network = StorageNetwork.find(server, worldPosition);
        if (network == null) return new Moved(0, 0);
        long items = move(server, network.itemSources(), network.itemDiskTarget());
        long fluid = move(server, network.fluidSources(), network.fluidDiskTarget());
        refreshUsage();
        sync();
        return new Moved(items, fluid);
    }

    private static <T extends TransferVariant<?>> long move(ServerLevel level, List<StorageNetwork.Source<T>> sources, Storage<T> target) {
        long moved = 0;
        for (StorageNetwork.Source<T> s : sources) {
            if (!s.valid() || s.storage() instanceof DriveStorage || isDrawer(level, s.pos())) continue;
            moved += StorageUtil.move(s.storage(), target, v -> true, Long.MAX_VALUE, null);
        }
        return moved;
    }

    /** Drawers keep what is in them: they are usually set up on purpose. */
    private static boolean isDrawer(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null) return false;
        var block = level.getBlockState(pos).getBlock();
        return block instanceof FacingDrawerBlock || block instanceof DrawerControllerBlock || block instanceof ControllerSlaveBlock;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("priority", priority);
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) list.add(disks.getItem(i));
        output.store("disks", ItemStack.OPTIONAL_CODEC.listOf(), list);
        output.store("filter", ItemStack.OPTIONAL_CODEC, filterSlot.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        priority = input.getIntOr("priority", DEFAULT_PRIORITY);
        List<ItemStack> list = input.read("disks", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOTS; i++) disks.getItems().set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        filterSlot.getItems().set(0, input.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        java.util.Arrays.fill(seenVersion, -1);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, disks);
            Containers.dropContents(level, pos, filterSlot);
        }
    }
}
