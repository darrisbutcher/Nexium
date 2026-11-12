package net.storagenexus.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/** Makes {@value #OUTPUT} NE a tick in daylight with open sky (a quarter of that in rain), and gives it to what touches it. */
public class SolarPanelBlockEntity extends BlockEntity {
    public static final int OUTPUT = 20;
    public static final long CAPACITY = 400;

    private final EnergyBuffer energy = EnergyBuffer.generator(CAPACITY, this::setChanged);

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    /** In rain it still makes a quarter. */
    public static final int RAIN_OUTPUT = OUTPUT / 4;

    /** What it makes now: all of it in daylight with open sky above, a quarter in rain, nothing at night or under a roof. */
    public static int output(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || !level.canSeeSky(pos.above())
                || !level.getBlockState(pos.above()).propagatesSkylightDown()) return 0;
        // Rain darkens the sky, so daylight is read from the time of day
        long time = level.getOverworldClockTime() % 24000L;
        if (level.dimensionType().hasFixedTime() || time >= 12500L && time < 23500L) return 0;
        return level.isRainingAt(pos.above()) ? RAIN_OUTPUT : OUTPUT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        int made = output(level, pos);
        if (made > 0) panel.energy.add(made, false);
        EnergyPush.push(level, pos, panel.energy, true);
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
