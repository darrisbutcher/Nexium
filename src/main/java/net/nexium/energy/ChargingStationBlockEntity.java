package net.nexium.energy;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds up to {@link #SLOTS} battery cells (or Jetpacks and Tablets with a cell in them) and charges each one a tick at up to
 * {@link #RATE} NE, from the energy its buffer takes in from touching Energy Conduits and Batteries.
 */
public class ChargingStationBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    public static final int SLOTS = 4;
    /** NE a tick, for each slot. */
    public static final long RATE = 1_000;
    public static final long BUFFER = 20_000;

    private final EnergyBuffer energy = EnergyBuffer.machine(BUFFER, this::setChanged);
    private final SimpleContainer slots = new SimpleContainer(SLOTS) {
        @Override public void setChanged() {
            super.setChanged();
            ChargingStationBlockEntity.this.setChanged();
        }
    };

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_STATION, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    public SimpleContainer slots() { return slots; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingStationBlockEntity station) {
        station.charge();
    }

    /** One tick: each slot takes what it can of the buffer, up to the rate. */
    public void charge() {
        if (energy.stored() <= 0) return;
        for (int i = 0; i < SLOTS && energy.stored() > 0; i++) {
            ItemStack stack = slots.getItem(i);
            if (stack.isEmpty() || !ItemEnergy.chargeable(stack)) continue;
            long room = ItemEnergy.insert(stack, Math.min(RATE, energy.stored()), true);
            if (room <= 0) continue;
            // Energy leaving the buffer for a cell is used up here: remove, not extract (it is no output to the outside)
            long taken = energy.remove(room, false);
            ItemEnergy.insert(stack, taken, false);
            slots.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChargingStationMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, slots);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy.stored());
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) list.add(slots.getItem(i));
        output.store("slots", ItemStack.OPTIONAL_CODEC.listOf(), list);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.set(input.getLongOr("energy", 0L));
        List<ItemStack> list = input.read("slots", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOTS; i++) slots.setItem(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
    }
}
