package net.storagenexus.energy;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.registry.ModItems;

/**
 * The Redstone Reactor, the top generator: nine Redstone Fuel Rods, each making 200 NE a tick at the default depth
 * (1,800 with all nine; it runs on any one to nine). One depth for all the rods, from Retracted 2 to Inserted 2, trades
 * power against heat and rod wear. Heat is carried off by ice in three coolant slots. Output holds to 60% heat, falls
 * steadily from there to 10% at 90%, and at 90% the reactor shuts off until it is back under 50%. It never explodes.
 */
public class RedstoneReactorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    public static final int RODS = 9, COOLANT = 3, SIZE = RODS + COOLANT;
    public static final int DEPTHS = 5, DEFAULT_DEPTH = 2;
    /** NE a tick for each rod, heat made and rod wear (times the default), by depth. */
    public static final int[] POWER = {100, 150, 200, 250, 300};
    public static final double[] HEAT = {0.25, 0.5, 1.0, 1.6, 2.25};
    public static final double[] WEAR = {0.5, 0.75, 1.0, 1.25, 1.5};
    public static final long CAPACITY = 1_000_000;
    /** How long a Redstone Fuel Rod lasts at the default depth, in ticks (eight hours). */
    public static final int ROD_LIFE = 576_000;

    /** Heat is a fraction, 0 to 1. A rod makes {@value #HEAT_PER_ROD} a tick at the default depth; the casing sheds {@value #PASSIVE_COOLING}. */
    public static final double HEAT_PER_ROD = 2.0e-5;
    public static final double PASSIVE_COOLING = 2.0e-5;
    public static final double FULL_OUTPUT_BELOW = 0.6, REDLINE = 0.9, RESTART_BELOW = 0.5, COOLANT_FROM = 0.25;
    /** What a piece of ice, packed ice and blue ice each take off a tick while it cools, and how long a piece lasts. */
    public static final double[] COOLING = {0, 2.0e-5, 3.5e-5, 5.5e-5};
    public static final int COOLANT_TICKS = 600;

    private static final int[] ALL_SLOTS = java.util.stream.IntStream.range(0, SIZE).toArray();

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = EnergyBuffer.generator(CAPACITY, this::setChanged);
    private int depth = DEFAULT_DEPTH;
    private double heat;
    private boolean shutDown;
    private double wearCarry;
    /** What each coolant slot is cooling with now (0 none, 1 ice, 2 packed, 3 blue) and the ticks it has left. */
    private final int[] coolKind = new int[COOLANT];
    private final int[] coolLeft = new int[COOLANT];
    /** NE a tick made on the last tick, for the screen. */
    private int output;

    public RedstoneReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_REACTOR, pos, state);
    }

    public EnergyBuffer energy() { return energy; }
    public int depth() { return depth; }
    public double heat() { return heat; }
    public boolean isShutDown() { return shutDown; }
    public int output() { return output; }

    public void setHeat(double heat) { this.heat = Math.max(0, Math.min(1, heat)); }

    public void setDepth(int depth) {
        this.depth = Math.max(0, Math.min(DEPTHS - 1, depth));
        setChanged();
    }

    /** A rod that still has life in it. */
    public static boolean isLiveRod(ItemStack stack) {
        return stack.is(ModItems.REDSTONE_FUEL_ROD);
    }

    /** 0 = not a coolant, 1 ice, 2 packed ice, 3 blue ice. */
    public static int coolantKind(ItemStack stack) {
        if (stack.is(Items.ICE)) return 1;
        if (stack.is(Items.PACKED_ICE)) return 2;
        if (stack.is(Items.BLUE_ICE)) return 3;
        return 0;
    }

    public int rodCount() {
        int n = 0;
        for (int i = 0; i < RODS; i++) if (isLiveRod(items.get(i))) n++;
        return n;
    }

    /** What the heat lets through: all of it up to 60%, then falling steadily to a tenth at the redline. */
    public static double heatFactor(double heat) {
        if (heat <= FULL_OUTPUT_BELOW) return 1.0;
        if (heat >= REDLINE) return 0.0;
        return 1.0 - 0.9 * (heat - FULL_OUTPUT_BELOW) / (REDLINE - FULL_OUTPUT_BELOW);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RedstoneReactorBlockEntity reactor) {
        reactor.work(level);
    }

    private void work(Level level) {
        int rods = rodCount();
        if (shutDown && heat < RESTART_BELOW) shutDown = false;
        int made = 0;
        if (!shutDown && rods > 0) {
            made = (int) Math.round(rods * POWER[depth] * heatFactor(heat));
            // With no room for it the rods idle: nothing is made, heated or worn
            if (energy.capacity() - energy.stored() < made) {
                made = 0;
            } else {
                energy.add(made, false);
                heat += rods * HEAT_PER_ROD * HEAT[depth];
                wearCarry += WEAR[depth];
                while (wearCarry >= 1.0) {
                    wearCarry -= 1.0;
                    wearRods();
                }
            }
        }
        if (output != made) {
            output = made;
            setChanged();
        }
        heat -= PASSIVE_COOLING;
        cool();
        heat = Math.max(0, Math.min(1, heat));
        if (!shutDown && heat >= REDLINE) shutDown = true;
        EnergyPush.push(level, worldPosition, energy, true);
        if (made > 0) setChanged();
    }

    private void wearRods() {
        for (int i = 0; i < RODS; i++) {
            ItemStack rod = items.get(i);
            if (!isLiveRod(rod)) continue;
            rod.setDamageValue(rod.getDamageValue() + 1);
            if (rod.getDamageValue() >= rod.getMaxDamage()) items.set(i, new ItemStack(ModItems.SPENT_FUEL_ROD));
        }
    }

    /** Ice takes heat off only above {@value #COOLANT_FROM}, so it isn't spent while the core is cool. */
    private void cool() {
        if (heat <= COOLANT_FROM) return;
        for (int c = 0; c < COOLANT; c++) {
            if (coolLeft[c] <= 0) {
                ItemStack piece = items.get(RODS + c);
                int kind = coolantKind(piece);
                if (kind == 0) continue;
                piece.shrink(1);
                coolKind[c] = kind;
                coolLeft[c] = COOLANT_TICKS;
            }
            heat -= COOLING[coolKind[c]];
            coolLeft[c]--;
            setChanged();
        }
    }

    // ---------------------------------------------------------------- container: nine rods, three coolants

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
        return slot < RODS ? isLiveRod(stack) : coolantKind(stack) > 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers below take the spent rods. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && stack.is(ModItems.SPENT_FUEL_ROD);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RedstoneReactorMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("energy", energy.stored());
        output.putInt("depth", depth);
        output.putDouble("heat", heat);
        output.putBoolean("shut_down", shutDown);
        output.putDouble("wear", wearCarry);
        output.putIntArray("cool_kind", coolKind);
        output.putIntArray("cool_left", coolLeft);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        energy.set(input.getLongOr("energy", 0L));
        depth = Math.max(0, Math.min(DEPTHS - 1, input.getIntOr("depth", DEFAULT_DEPTH)));
        heat = Math.max(0, Math.min(1, input.getDoubleOr("heat", 0.0)));
        shutDown = input.getBooleanOr("shut_down", false);
        wearCarry = Math.max(0, input.getDoubleOr("wear", 0.0));
        int[] kinds = input.getIntArray("cool_kind").orElse(new int[0]);
        int[] left = input.getIntArray("cool_left").orElse(new int[0]);
        for (int c = 0; c < COOLANT; c++) {
            coolKind[c] = c < kinds.length ? Math.max(0, Math.min(COOLING.length - 1, kinds[c])) : 0;
            coolLeft[c] = c < left.length ? Math.max(0, left[c]) : 0;
        }
    }
}
