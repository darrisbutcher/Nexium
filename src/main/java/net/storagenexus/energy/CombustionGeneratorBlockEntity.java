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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/**
 * Burns furnace fuel into Nexus Energy: {@value #OUTPUT} NE a tick for as long as the fuel would burn in a furnace (a
 * piece of coal: 80 seconds, 32,000 NE). It only lights the next piece when its buffer has room, so nothing is burnt
 * for nothing. What it makes goes to the machines touching it, and Energy Conduits take it further.
 */
public class CombustionGeneratorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    public static final int OUTPUT = 20;
    public static final long CAPACITY = 20_000;
    private static final int[] FUEL_SLOT = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final EnergyBuffer energy = EnergyBuffer.generator(CAPACITY, this::setChanged);
    /** Ticks the piece that is burning has left, and how long it burns in all (for the flame on the screen). */
    private int burnLeft, burnTotal;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    public int burnLeft() { return burnLeft; }

    public int burnTotal() { return burnTotal; }

    public boolean isBurning() { return burnLeft > 0; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity generator) {
        generator.work(level);
    }

    private void work(Level level) {
        boolean wasBurning = burnLeft > 0;
        // Light the next piece only when a tick's output fits
        if (burnLeft <= 0 && energy.capacity() - energy.stored() >= OUTPUT) {
            ItemStack fuel = items.get(0);
            int ticks = fuel.isEmpty() ? 0 : burnTicks(level, fuel);
            if (ticks > 0) {
                net.minecraft.world.item.ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
                ItemStack left = remainder == null ? ItemStack.EMPTY : remainder.create();
                fuel.shrink(1);
                if (fuel.isEmpty() && !left.isEmpty()) items.set(0, left); // a lava bucket leaves its bucket
                burnLeft = burnTotal = ticks;
            }
        }
        if (burnLeft > 0) {
            burnLeft--;
            energy.add(OUTPUT, false);
            setChanged();
        }
        EnergyPush.push(level, worldPosition, energy, true);
        if (wasBurning != burnLeft > 0) {
            level.setBlock(worldPosition, getBlockState().setValue(CombustionGeneratorBlock.LIT, burnLeft > 0), Block.UPDATE_ALL);
        }
    }

    /** Anything a furnace burns. */
    public static boolean isFuel(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL);
    }

    /** How many ticks this would burn in a furnace (0 if it isn't fuel). */
    public static int burnTicks(Level level, ItemStack stack) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || !isFuel(stack)) return 0;
        var context = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY)).create(java.util.Optional.empty());
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(stack,
                net.minecraft.core.component.DataComponents.COOKING_FUEL, net.minecraft.world.item.component.CookingFuel::burnTime, context, 0);
    }

    // ---------------------------------------------------------------- container: one fuel slot

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
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isFuel(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FUEL_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers below take only what is no longer fuel: the empty bucket a lava bucket leaves. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && !canPlaceItem(slot, stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CombustionGeneratorMenu(containerId, inventory, this);
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
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        energy.set(input.getLongOr("energy", 0L));
        burnLeft = Math.max(0, input.getIntOr("burn_left", 0));
        burnTotal = Math.max(0, input.getIntOr("burn_total", 0));
    }
}
