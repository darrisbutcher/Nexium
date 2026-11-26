package net.nexium.energy;

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
import net.nexium.registry.ModBlockEntities;

/**
 * Burns lava into Nexus Energy. Tier 1 makes {@value #OUTPUT} NE a tick, a bucket lasting {@value #BUCKET_TICKS} ticks (about eight
 * minutes, 800,000 NE); Tier 2 makes 320 and burns a bucket in 100 seconds, Tier 3 makes 1,280 and burns one in 25 (640,000 NE a
 * bucket). The output can be turned down to 25%, 50% or 75% (or off), and the lava burns that much slower. It takes lava only, in
 * from outside only, and lights the next bucket when its buffer has room.
 */
public class ThermalGeneratorBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    public static final int OUTPUT = 80;
    public static final int BUCKET_TICKS = 10_000;
    public static final long CAPACITY = 80_000;
    public static final long TANK = 4 * FluidConstants.BUCKET;
    /** Tier 1, 2 and 3: NE a tick, ticks a bucket lasts at full output, energy buffer, and tank in buckets. */
    private static final int[] TIER_OUTPUT = {80, 320, 1280}, TIER_BUCKET_TICKS = {10_000, 2_000, 500}, TIER_TANK_BUCKETS = {4, 8, 16};
    private static final long[] TIER_CAPACITY = {80_000, 320_000, 1_280_000};
    /** The output steps: off, 25%, 50%, 75% and 100% (a quarter each). */
    public static final int THROTTLE_STEPS = 4;

    private final int tier;
    private final EnergyBuffer energy;
    private final SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return tankCapacity(); }
        @Override protected boolean canInsert(FluidVariant variant) { return variant.isOf(Fluids.LAVA); }
        @Override protected boolean canExtract(FluidVariant variant) { return false; }
        @Override protected void onFinalCommit() { ThermalGeneratorBlockEntity.this.setChanged(); }
    };
    /** What is left of the lit bucket, in quarter ticks (a bucket is four times its ticks; each tick burns as many as the throttle steps). */
    private int burnLeft;
    private int throttle = THROTTLE_STEPS;

    public ThermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_GENERATOR, pos, state);
        this.tier = state.getBlock() instanceof ThermalGeneratorBlock block ? block.tier() : 1;
        this.energy = EnergyBuffer.generator(TIER_CAPACITY[tier - 1], this::setChanged);
    }

    public static int output(int tier) { return TIER_OUTPUT[tier - 1]; }
    public static int bucketTicks(int tier) { return TIER_BUCKET_TICKS[tier - 1]; }
    public static long capacity(int tier) { return TIER_CAPACITY[tier - 1]; }
    public static long tankCapacity(int tier) { return TIER_TANK_BUCKETS[tier - 1] * FluidConstants.BUCKET; }

    public int tier() { return tier; }
    public int output() { return output(tier); }
    public long tankCapacity() { return tankCapacity(tier); }

    /** 0 (off) to {@value #THROTTLE_STEPS} (full): how many quarters of the tier's output it makes. */
    public int throttle() { return throttle; }

    public void setThrottle(int steps) {
        throttle = Math.max(0, Math.min(THROTTLE_STEPS, steps));
        setChanged();
    }

    /** NE a tick it makes right now while it burns. */
    public int currentOutput() { return output() * throttle / THROTTLE_STEPS; }

    public EnergyBuffer energy() { return energy; }

    public SingleVariantStorage<FluidVariant> tank() { return tank; }

    /** What is left of the lit bucket, in ticks at full output (rounded up). */
    public int burnLeft() { return (burnLeft + THROTTLE_STEPS - 1) / THROTTLE_STEPS; }

    /** Lava in the tank, in droplets (a bucket is {@value FluidConstants#BUCKET}). */
    public long lava() { return tank.amount; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermalGeneratorBlockEntity generator) {
        generator.work(level);
    }

    private void work(Level level) {
        boolean wasBurning = getBlockState().getValue(ThermalGeneratorBlock.LIT);
        if (throttle > 0 && burnLeft <= 0 && energy.capacity() - energy.stored() >= output() && tank.amount >= FluidConstants.BUCKET) {
            tank.amount -= FluidConstants.BUCKET;
            if (tank.amount == 0) tank.variant = FluidVariant.blank();
            burnLeft = bucketTicks(tier) * THROTTLE_STEPS;
        }
        boolean burning = throttle > 0 && burnLeft > 0;
        if (burning) {
            // Each quarter tick burned gives a quarter of the tier's output, so a bucket gives the same energy at any speed
            int used = Math.min(throttle, burnLeft);
            burnLeft -= used;
            energy.add(output() * used / THROTTLE_STEPS, false);
            setChanged();
        }
        EnergyPush.push(level, worldPosition, energy, true);
        if (wasBurning != burning) {
            level.setBlock(worldPosition, getBlockState().setValue(ThermalGeneratorBlock.LIT, burning), Block.UPDATE_ALL);
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
        output.putInt("burn_quarters", 1);
        output.putInt("throttle", throttle);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.set(input.getLongOr("energy", 0L));
        tank.variant = input.read("fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        tank.amount = Math.max(0, Math.min(tankCapacity(), input.getLongOr("amount", 0L)));
        // Before 1.28.0 the time left was in ticks; now it is in quarter ticks
        burnLeft = Math.max(0, input.getIntOr("burn_left", 0)) * (input.getIntOr("burn_quarters", 0) == 1 ? 1 : THROTTLE_STEPS);
        throttle = Math.max(0, Math.min(THROTTLE_STEPS, input.getIntOr("throttle", THROTTLE_STEPS)));
    }
}
