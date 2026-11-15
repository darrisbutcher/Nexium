package net.storagenexus.essentials.sponge;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WetSpongeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** A wet big sponge: it dries in a furnace, and at once where water evaporates (the Nether), as a wet sponge does. */
public class BigWetSpongeBlock extends WetSpongeBlock {
    private final Supplier<Block> dry;

    public BigWetSpongeBlock(Properties properties, Supplier<Block> dry) {
        super(properties);
        this.dry = dry;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.setBlockAndUpdate(pos, dry.get().defaultBlockState());
            level.levelEvent(2009, pos, 0);
            level.playSound(null, pos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F, (1.0F + level.getRandom().nextFloat() * 0.2F) * 0.7F);
        }
    }
}
