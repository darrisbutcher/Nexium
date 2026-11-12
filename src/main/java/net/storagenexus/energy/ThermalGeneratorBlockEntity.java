package net.storagenexus.energy;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/**
 * Burns lava into Nexus Energy: {@value #OUTPUT} NE a tick, a bucket lasting {@value #BUCKET_TICKS} ticks (about eight
 * minutes, 800,000 NE). It takes lava only, in from outside only, and lights the next bucket when its buffer has room.
 */
public class ThermalGeneratorBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    public static final int OUTPUT = 80;
    public static final int BUCKET_TICKS = 10_000;
    public static final long CAPACITY = 80_000;
    public static final long TANK = 4 * FluidConstants.BUCKET;

    private final EnergyBuffer energy = EnergyBuffer.generator(CAPACITY, this::setChanged);
    private final SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return TANK; }
        @Override protected boolean canInsert(FluidVariant variant) { return variant.isOf(Fluids.LAVA); }
        @Override protected boolean canExtract(FluidVariant variant) { return false; }
        @Override protected void onFinalCommit() { ThermalGeneratorBlockEntity.this.setChanged(); }
    };
    private int burnLeft;

    public ThermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_GENERATOR, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    public SingleVariantStorage<FluidVariant> tank() { return tank; }

    public int burnLeft() { return burnLeft; }

    /** Lava in the tank, in droplets (a bucket is {@value FluidConstants#BUCKET}). */
    public long lava() { return tank.amount; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermalGeneratorBlockEntity generator) {
        generator.work(level);
    }

    private void work(Level level) {
        boolean wasBurning = burnLeft > 0;
        if (burnLeft <= 0 && energy.capacity() - energy.stored() >= OUTPUT && tank.amount >= FluidConstants.BUCKET) {
            tank.amount -= FluidConstants.BUCKET;
            if (tank.amount == 0) tank.variant = FluidVariant.blank();
            burnLeft = BUCKET_TICKS;
        }
        if (burnLeft > 0) {
            burnLeft--;
            energy.add(OUTPUT, false);
            setChanged();
        }
        EnergyPush.push(level, worldPosition, energy, true);
        if (wasBurning != burnLeft > 0) {
            level.setBlock(worldPosition, getBlockState().setValue(ThermalGeneratorBlock.LIT, burnLeft > 0), Block.UPDATE_ALL);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ThermalGeneratorMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy.stored());
        output.store("fluid", FluidVariant.CODEC, tank.variant);
        output.putLong("amount", tank.amount);
        output.putInt("burn_left", burnLeft);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.set(input.getLongOr("energy", 0L));
        tank.variant = input.read("fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        tank.amount = Math.max(0, Math.min(TANK, input.getLongOr("amount", 0L)));
        burnLeft = Math.max(0, input.getIntOr("burn_left", 0));
    }
}
