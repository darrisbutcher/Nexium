package net.storagenexus.essentials.ironfarm;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModBlockEntities;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.stream.IntStream;

/**
 * A 1-block iron farm: four 3-villager golem farms in a box. Every {@code ironFarmGolemTicks} it "kills a golem"
 * and keeps the drops (3-5 iron) in 27 output slots, which hoppers can only take from below.
 *
 * <p>It needs only Nexus Energy: no bread or other boost. A full output or a redstone signal pauses the farm.
 */
public class IronFarmBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos>,
        net.storagenexus.util.ComparatorReadable {
    public static final int OUTPUT_SLOTS = 27;
    public static final int BREAD_SLOT = OUTPUT_SLOTS;
    public static final int SIZE = OUTPUT_SLOTS + 1;
    /** Most a golem can drop, so there is always room for a whole golem before one is made. */
    public static final int MAX_IRON = 5;

    private static final int[] OUTPUT = IntStream.range(0, OUTPUT_SLOTS).toArray();
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private int boostLeft;
    private long boostUsed;
    private long cooldown;
    /** The output had no room for another golem last tick (not saved; worked out again every tick). */
    private boolean full;
    /** No energy for this tick's work (not saved; worked out again every tick). */
    private boolean unpowered;
    /** Nexus Energy the Iron Farm uses each tick it works. */
    public static final int ENERGY_PER_TICK = 40;
    /** The Nexus Energy it runs on: ten seconds' worth. */
    private final net.storagenexus.energy.EnergyBuffer energy =
            net.storagenexus.energy.EnergyBuffer.machine(ENERGY_PER_TICK * net.storagenexus.essentials.farm.FarmBlockEntity.ENERGY_TICKS, this::setChanged);

    public net.storagenexus.energy.EnergyBuffer energy() { return energy; }

    /** Whether the farm is waiting for Nexus Energy. */
    public boolean isUnpowered() { return unpowered; }

    public IronFarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_FARM, pos, state);
    }

    // ---------------------------------------------------------------- working

    public static void serverTick(Level level, BlockPos pos, BlockState state, IronFarmBlockEntity farm) {
        farm.work(level.getRandom());
    }

    private void work(RandomSource random) {
        NexusConfig config = NexusConfig.get();
        // An older version's bread, and boost and cooldown, are gone: the bread is dropped
        if (!items.get(BREAD_SLOT).isEmpty() && level != null) {
            ItemStack old = items.get(BREAD_SLOT);
            items.set(BREAD_SLOT, ItemStack.EMPTY);
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, old);
        }
        boostLeft = 0;
        boostUsed = 0;
        cooldown = 0;
        boolean wasFull = full;
        full = !hasRoomForGolem();
        // A comparator reads 15 while the farm is stopped for lack of room
        if (full != wasFull && level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        unpowered = energy.stored() < ENERGY_PER_TICK;
        if (!full && !isPaused() && !unpowered) {
            energy.use(ENERGY_PER_TICK);
            progress += 1;
            int perGolem = config.ironFarmGolemTicks;
            while (progress >= perGolem && hasRoomForGolem()) {
                progress -= perGolem;
                killGolem(random);
            }
            // Out of room part way through a boosted tick: wait at the finish line
            progress = Math.min(progress, perGolem);
            full = !hasRoomForGolem();
        }
        setChanged();
    }

    private void killGolem(RandomSource random) {
        insert(new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(3)));
    }

    /** What a comparator reads: how full the output is, as for a chest, and 15 once there is no room for another golem. */
    @Override
    public int comparatorSignal() {
        return full ? 15 : net.storagenexus.util.ComparatorReadable.signal(
                net.storagenexus.essentials.farm.FarmBlockEntity.outputFill(items, OUTPUT_SLOTS));
    }

    private boolean hasRoomForGolem() {
        return room(Items.IRON_INGOT.getDefaultInstance(), MAX_IRON, 0);
    }

    /** Whether the output slots can take {@code count} of {@code stack} and also {@code poppies} poppies. */
    private boolean room(ItemStack stack, int count, int poppies) {
        int emptySlots = 0, spaceForStack = 0, spaceForPoppies = 0;
        ItemStack poppy = Items.POPPY.getDefaultInstance();
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) emptySlots++;
            else if (ItemStack.isSameItemSameComponents(s, stack)) spaceForStack += s.getMaxStackSize() - s.getCount();
            else if (ItemStack.isSameItemSameComponents(s, poppy)) spaceForPoppies += s.getMaxStackSize() - s.getCount();
        }
        int slotsNeeded = slotsFor(count - spaceForStack, stack.getMaxStackSize()) + slotsFor(poppies - spaceForPoppies, poppy.getMaxStackSize());
        return slotsNeeded <= emptySlots;
    }

    private static int slotsFor(int count, int maxStack) {
        return count <= 0 ? 0 : (count + maxStack - 1) / maxStack;
    }

    private void insert(ItemStack stack) {
        for (int i = 0; i < OUTPUT_SLOTS && !stack.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.split(stack.getMaxStackSize()));
            }
        }
    }

    // ---------------------------------------------------------------- what the screen and tests read

    /** Work towards the next golem, 0 to 1. */
    public float progressFraction() {
        return Math.min(1.0F, (float) progress / NexusConfig.get().ironFarmGolemTicks);
    }

    public int boostLeft() { return boostLeft; }
    public long boostUsed() { return boostUsed; }
    public long cooldown() { return cooldown; }
    public boolean isFull() { return full; }

    /** A redstone signal stops the farm. */
    public boolean isPaused() {
        BlockState state = getBlockState();
        return state.hasProperty(IronFarmBlock.POWERED) && state.getValue(IronFarmBlock.POWERED);
    }

    public IronFarmTimers timers() {
        return new IronFarmTimers(progress, boostLeft, boostUsed, cooldown);
    }

    public void setTimers(IronFarmTimers timers) {
        progress = Math.max(0, timers.progress());
        setChanged();
    }

    /** The farm takes no bread (or anything else) any more: it only needs power. */
    public boolean acceptsBread() {
        return false;
    }

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
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    /** Hoppers and pipes reach the output from below only, and can't put anything in. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot < OUTPUT_SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new IronFarmMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    // ---------------------------------------------------------------- saving, and keeping the timers in the item

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.store("timers", IronFarmTimers.CODEC, timers());
        output.putLong("energy", energy.stored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        setTimersQuietly(input.read("timers", IronFarmTimers.CODEC).orElse(IronFarmTimers.NONE));
        energy.set(input.getLongOr("energy", 0L));
    }

    private void setTimersQuietly(IronFarmTimers timers) {
        progress = Math.max(0, timers.progress());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        IronFarmTimers timers = timers();
        if (!timers.isNone()) components.set(ModComponents.IRON_FARM_TIMERS, timers);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        IronFarmTimers timers = components.get(ModComponents.IRON_FARM_TIMERS);
        if (timers != null) setTimersQuietly(timers);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("timers");
    }
}
