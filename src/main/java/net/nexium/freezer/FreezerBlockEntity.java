package net.nexium.freezer;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.registry.ModBlockEntities;
import net.nexium.registry.ModComponents;
import org.jspecify.annotations.Nullable;

import java.util.stream.IntStream;

/**
 * The Freezer's block entity: a water tank (8 buckets, water only, filled by hand or pipe), 9 output slots hoppers
 * can take from below, a mode (ice, packed ice, blue ice) and a dye colour. While it has water and room it works
 * towards one item, using the water up steadily over the mode's time; no water, a full output or a redstone signal
 * pauses it. Water, mode and progress stay in the dropped item.
 */
public class FreezerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    public static final int SLOTS = 9;
    public static final long CAPACITY = 8 * FluidConstants.BUCKET;
    /** The colour of an undyed Freezer's body: the frame's blue-grey. */
    public static final int UNDYED = 0x4D7A94;

    private static final int[] ALL = IntStream.range(0, SLOTS).toArray();
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private FreezerMode mode = FreezerMode.ICE;
    private int progress;
    /** The dye colour as RGB, or -1 if undyed. */
    private int color = -1;
    /** Not saved; worked out every tick. */
    private boolean full, dry;
    /** No energy for this tick's work (not saved; worked out again every tick). */
    private boolean unpowered;
    /** Nexus Energy the Freezer uses each tick it works. */
    public static final int ENERGY_PER_TICK = 20;
    /** The Nexus Energy it runs on: ten seconds' worth. */
    private final net.nexium.energy.EnergyBuffer energy = net.nexium.energy.EnergyBuffer.machine(ENERGY_PER_TICK * 200L, this::setChanged);

    public net.nexium.energy.EnergyBuffer energy() { return energy; }

    /** The overclock level: 0 off, 1 is 2x, 2 is 4x, 3 is 8x. */
    private int overclock;

    public int overclock() { return overclock; }

    /** Sets the overclock level; the energy buffer is resized to hold ten seconds of work at the new speed. */
    public void setOverclock(int level) {
        overclock = net.nexium.essentials.farm.Overclock.clamp(level);
        energy.setCapacity(net.nexium.essentials.farm.Overclock.capacity(ENERGY_PER_TICK, overclock));
        setChanged();
    }

    /** Whether the Freezer is waiting for Nexus Energy. */
    public boolean isUnpowered() { return unpowered; }

    private final SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return CAPACITY; }
        @Override protected boolean canInsert(FluidVariant variant) { return variant.isOf(Fluids.WATER); }
        @Override public boolean supportsExtraction() { return false; }
        /** Nobody empties the tank from outside; the Freezer itself uses the water up. */
        @Override public long extract(FluidVariant variant, long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) { return 0; }
        @Override protected void onFinalCommit() { setChanged(); }
    };

    public FreezerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FREEZER, pos, state);
    }

    public SingleVariantStorage<FluidVariant> tank() { return tank; }

    /** Switched off from its screen: no work and no power use (kept in the dropped item). */
    private boolean off;

    public boolean isOff() { return off; }

    public void setOff(boolean off) {
        this.off = off;
        setChanged();
    }

    // ---------------------------------------------------------------- working

    public static void serverTick(Level level, BlockPos pos, BlockState state, FreezerBlockEntity freezer) {
        if (freezer.off) return;
        freezer.work();
    }

    private void work() {
        full = !hasRoom();
        long cost = mode.cost();
        int ticks = Math.max(1, mode.ticks());
        // The water this tick takes: the share of the cost between this tick's progress and the next
        long take = cost * (progress + 1) / ticks - cost * progress / ticks;
        dry = tank.amount < take;
        // Overclocked, it does as many ticks of work as the energy it holds can pay for, up to its setting
        int run = net.nexium.essentials.farm.Overclock.ticksOfWork(overclock, energy.stored(), ENERGY_PER_TICK);
        unpowered = run < 1;
        if (!full && !dry && !isPaused() && !unpowered) {
            // ... and as many as the water in the tank lasts for
            while (run > 1 && tank.amount < cost * (progress + run) / ticks - cost * progress / ticks) run--;
            take = cost * (progress + run) / ticks - cost * progress / ticks;
            energy.use((long) ENERGY_PER_TICK * run);
            tank.amount -= take;
            if (tank.amount == 0) tank.variant = FluidVariant.blank();
            progress += run;
            if (progress >= ticks) {
                // What went past the end of the cycle counts towards the next one
                progress = Math.min(progress - ticks, run - 1);
                insert(new ItemStack(mode.output()));
            }
            setChanged();
        }
    }

    /** Whether the output can take another item of the current mode. */
    private boolean hasRoom() {
        ItemStack out = new ItemStack(mode.output());
        for (ItemStack s : items) {
            if (s.isEmpty() || (ItemStack.isSameItemSameComponents(s, out) && s.getCount() < s.getMaxStackSize())) return true;
        }
        return false;
    }

    private void insert(ItemStack stack) {
        for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                s.grow(1);
                stack.shrink(1);
            }
        }
        for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) items.set(i, stack.split(1));
        }
    }

    // ---------------------------------------------------------------- what the screen and tests read

    public FreezerMode mode() { return mode; }

    public void setMode(FreezerMode value) {
        if (value != mode) {
            mode = value;
            progress = 0;
            setChanged();
        }
    }

    /** Steps to the next mode (ice, packed ice, blue ice, back to ice); the progress starts over. */
    public FreezerMode cycleMode() {
        setMode(mode.next());
        return mode;
    }

    public int progress() { return progress; }

    public float progressFraction() {
        return Math.min(1.0F, (float) progress / Math.max(1, mode.ticks()));
    }

    public boolean isFull() { return full; }
    public boolean isDry() { return dry; }

    public long water() { return tank.amount; }

    /** Puts water straight into the tank, up to the capacity (tests and the item use it). */
    public void setWater(long droplets) {
        tank.amount = Math.max(0, Math.min(CAPACITY, droplets));
        tank.variant = tank.amount > 0 ? FluidVariant.of(Fluids.WATER) : FluidVariant.blank();
        setChanged();
    }

    public boolean isPaused() {
        BlockState state = getBlockState();
        return state.hasProperty(FreezerBlock.POWERED) && state.getValue(FreezerBlock.POWERED);
    }

    /** The dye colour as RGB: undyed is the frame's blue-grey. */
    public int color() {
        return color < 0 ? UNDYED : color;
    }

    public boolean isDyed() { return color >= 0; }

    // ---------------------------------------------------------------- container

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    /** Hoppers and pipes take the ice from below, and can't put anything in. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? ALL : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FreezerMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    // ---------------------------------------------------------------- saving, the item, and the client

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("off", off);
        ContainerHelper.saveAllItems(output, items);
        output.store("fluid", FluidVariant.CODEC, tank.variant);
        output.putLong("amount", tank.amount);
        output.putInt("mode", mode.ordinal());
        output.putInt("progress", progress);
        output.putInt("overclock", overclock);
        output.putInt("color", color);
        output.putLong("energy", energy.stored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        off = input.getBooleanOr("off", false);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        tank.variant = input.read("fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        tank.amount = Math.max(0, Math.min(CAPACITY, input.getLongOr("amount", 0L)));
        mode = FreezerMode.byOrdinal(input.getIntOr("mode", 0));
        progress = Math.max(0, input.getIntOr("progress", 0));
        overclock = net.nexium.essentials.farm.Overclock.clamp(input.getIntOr("overclock", 0));
        energy.setCapacity(net.nexium.essentials.farm.Overclock.capacity(ENERGY_PER_TICK, overclock));
        energy.set(input.getLongOr("energy", 0L));
        int before = color;
        color = input.getIntOr("color", -1);
        // The client draws the body in this colour, so it redraws the block when the colour arrives
        if (color != before && level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (off) components.set(net.nexium.essentials.registry.ModComponents.MACHINE_OFF, net.minecraft.util.Unit.INSTANCE);
        if (color >= 0) components.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
        if (tank.amount > 0) components.set(ModComponents.FREEZER_WATER, tank.amount);
        if (mode != FreezerMode.ICE) components.set(ModComponents.FREEZER_MODE, mode.ordinal());
        if (overclock > 0) components.set(net.nexium.essentials.registry.ModComponents.OVERCLOCK, overclock);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        if (components.get(net.nexium.essentials.registry.ModComponents.MACHINE_OFF) != null) off = true;
        DyedItemColor dyed = components.get(DataComponents.DYED_COLOR);
        color = dyed == null ? -1 : dyed.rgb() & 0xFFFFFF;
        Long water = components.get(ModComponents.FREEZER_WATER);
        if (water != null) setWater(water);
        Integer saved = components.get(ModComponents.FREEZER_MODE);
        if (saved != null) mode = FreezerMode.byOrdinal(saved);
        Integer savedOverclock = components.get(net.nexium.essentials.registry.ModComponents.OVERCLOCK);
        if (savedOverclock != null) setOverclock(savedOverclock);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("off");
        output.discard("color");
        output.discard("amount");
        output.discard("fluid");
        output.discard("mode");
        output.discard("overclock");
    }
}
