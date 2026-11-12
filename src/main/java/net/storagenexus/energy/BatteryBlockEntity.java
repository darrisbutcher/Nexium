package net.storagenexus.energy;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/**
 * Holds Nexus Energy: generators and Energy Conduits fill it, Energy Conduits and the machines touching it take from
 * it. The six cells on its side show the charge.
 */
public class BatteryBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    private final EnergyBuffer energy;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY, pos, state);
        long capacity = state.getBlock() instanceof BatteryBlock battery ? battery.capacity() : 250_000;
        energy = new EnergyBuffer(capacity, true, true, this::setChanged);
    }

    public EnergyBuffer energy() { return energy; }

    /** The cells that show charge: 0 when empty, 1 for any charge, and 6 once it is more than five sixths full. */
    public int charge() {
        if (energy.stored() <= 0) return 0;
        return (int) Math.min(6, (6 * energy.stored() + energy.capacity() - 1) / energy.capacity());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity battery) {
        battery.showCharge(level, state);
        // Gives to the machines touching it (not to other batteries, which would only pass it back and forth)
        EnergyPush.push(level, pos, battery.energy, false);
    }

    private void showCharge(Level level, BlockState state) {
        int charge = charge();
        if (state.getValue(BatteryBlock.CHARGE) != charge) {
            level.setBlock(worldPosition, state.setValue(BatteryBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BatteryMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    /** The charge goes with the Battery when it is broken, and comes back when it is placed. */
    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.stored() > 0) components.set(net.storagenexus.registry.ModComponents.BATTERY_CHARGE, energy.stored());
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Long charge = components.get(net.storagenexus.registry.ModComponents.BATTERY_CHARGE);
        if (charge != null) energy.set(charge);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy.stored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.set(input.getLongOr("energy", 0L));
    }
}
