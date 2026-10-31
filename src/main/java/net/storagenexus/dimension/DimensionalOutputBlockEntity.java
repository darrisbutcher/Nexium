package net.storagenexus.dimension;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.filter.FilterData;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.network.NetworkBlock;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModBlockEntities;

/**
 * Receives items and fluids from its linked Input and pushes them into whatever is around it, on any
 * side: chests, drawers, Drawer Controllers, item conduits, or any storage-network block
 * (then into the whole network).
 */
public class DimensionalOutputBlockEntity extends LinkedBlockEntity {
    private final SimpleContainer filterSlot = new SimpleContainer(1) {
        @Override public void setChanged() { super.setChanged(); DimensionalOutputBlockEntity.this.setChanged(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof FilterItem; }
        @Override public int getMaxStackSize() { return 1; }
    };
    private boolean inputOnline;
    /** Which side gets the first try next time, so no single neighbour hogs everything. */
    private int nextSide;
    /** Bit per Direction (3D data value): sides the Output pushes into. All on by default. */
    private int sideMask = ALL_SIDES;
    public static final int ALL_SIDES = 0b111111;

    public DimensionalOutputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_OUTPUT, pos, state);
    }

    public SimpleContainer filterSlot() { return filterSlot; }
    public boolean inputOnline() { return inputOnline; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalOutputBlockEntity be) {
        if (level.getGameTime() % 40 != 0) return;
        LinkRegistry reg = be.registry();
        if (reg == null) return;
        LinkRegistry.Link link = reg.get(be.linkId);
        if (be.linkId != null && (link == null || !link.outputs().contains(be.globalPos()))) {
            be.setLink(null);
            return;
        }
        boolean online = false;
        if (link != null && link.input() != null) {
            GlobalPos in = link.input();
            ServerLevel l = ((ServerLevel) level).getServer().getLevel(in.dimension());
            online = l != null && l.isLoaded(in.pos());
        }
        be.inputOnline = online;
        if (be.refreshName()) be.sync();
    }

    public boolean accepts(ItemVariant variant) {
        ItemStack f = filterSlot.getItem(0);
        if (!(f.getItem() instanceof FilterItem fi)) return true;
        FilterData data = fi.data(f);
        return data.matches(variant);
    }

    /**
     * Pushes into everything around the Output: storage networks, inventories and item
     * conduits, on all six sides. Sides take turns going first. Dimensional Inputs are skipped
     * so items cannot loop back into a link. Returns how many went in.
     */
    public long receive(ItemVariant variant, long amount, TransactionContext tx) {
        if (level == null) return 0;
        Direction[] sides = Direction.values();
        int first = nextSide;
        nextSide = (nextSide + 1) % sides.length;
        long moved = 0;
        for (int i = 0; i < sides.length && moved < amount; i++) {
            Direction side = sides[(first + i) % sides.length];
            if (isSideEnabled(side)) moved += pushTo(side, variant, amount - moved, tx);
        }
        return moved;
    }

    public boolean acceptsFluid(FluidVariant variant) {
        ItemStack f = filterSlot.getItem(0);
        return !(f.getItem() instanceof FilterItem fi) || fi.data(f).matches(variant);
    }

    /** Fluid counterpart of {@link #receive}: storage networks and any fluid storage on the enabled sides. */
    public long receiveFluid(FluidVariant variant, long amount, TransactionContext tx) {
        if (level == null) return 0;
        Direction[] sides = Direction.values();
        int first = nextSide;
        long moved = 0;
        for (int i = 0; i < sides.length && moved < amount; i++) {
            Direction side = sides[(first + i) % sides.length];
            if (!isSideEnabled(side)) continue;
            BlockPos target = worldPosition.relative(side);
            if (level.getBlockEntity(target) instanceof DimensionalInputBlockEntity) continue;
            if (level.getBlockState(target).getBlock() instanceof NetworkBlock) {
                StorageNetwork net = StorageNetwork.find(level, target);
                if (net != null) moved += net.insertFluid(variant, amount - moved, tx);
                continue;
            }
            Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, target, side.getOpposite());
            if (storage != null) moved += storage.insert(variant, amount - moved, tx);
        }
        return moved;
    }

    public boolean isSideEnabled(Direction side) {
        return (sideMask & (1 << side.get3DDataValue())) != 0;
    }

    public int sideMask() { return sideMask; }

    public void toggleSide(Direction side) {
        sideMask ^= 1 << side.get3DDataValue();
        setChanged();
    }

    private long pushTo(Direction side, ItemVariant variant, long amount, TransactionContext tx) {
        BlockPos target = worldPosition.relative(side);
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof DimensionalInputBlockEntity) return 0;
        if (be instanceof ConduitBundleBlockEntity conduit) return conduit.acceptPushed(side.getOpposite(), variant, amount, tx);
        if (level.getBlockState(target).getBlock() instanceof NetworkBlock) {
            StorageNetwork net = StorageNetwork.find(level, target);
            return net == null ? 0 : net.insertItem(variant, amount, tx);
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, target, side.getOpposite());
        return storage == null ? 0 : storage.insert(variant, amount, tx);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("filter", ItemStack.OPTIONAL_CODEC, filterSlot.getItem(0));
        output.putInt("sides", sideMask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filterSlot.setItem(0, input.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        sideMask = input.getIntOr("sides", ALL_SIDES) & ALL_SIDES;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        Containers.dropContents(level, pos, filterSlot);
        LinkRegistry reg = registry();
        if (reg != null) reg.removeOutput(globalPos());
    }
}
