package net.storagenexus.essentials.sponge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Supplier;

/**
 * A sponge that reaches further: it soaks up water the way a sponge does (placed next to water, or when water comes next to it,
 * it removes the water it can reach through water, kelp and seagrass included, and turns into its wet version), but reaches
 * {@link #reach} blocks through the water instead of 6, and soaks up as much water as the sponges it stands for would together
 * ({@link #capacity}: 65 blocks for each).
 */
public class BigSpongeBlock extends SpongeBlock {
    private final int reach;
    private final int capacity;
    private final Supplier<Block> wet;

    public BigSpongeBlock(Properties properties, int reach, int capacity, Supplier<Block> wet) {
        super(properties);
        this.reach = reach;
        this.capacity = capacity;
        this.wet = wet;
    }

    /** How many blocks away, through the water, it takes water from. */
    public int reach() {
        return reach;
    }

    /** The most water blocks it takes in one go: 65 for each sponge it stands for. */
    public int capacity() {
        return capacity;
    }

    @Override
    protected void tryAbsorbWater(Level level, BlockPos pos) {
        if (absorb(level, pos)) {
            level.setBlock(pos, wet.get().defaultBlockState(), 2);
            level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Removes the water this sponge reaches; true if there was any. The same walk a vanilla sponge does, with this reach and capacity. */
    private boolean absorb(Level level, BlockPos origin) {
        return BlockPos.breadthFirstTraversal(origin, reach, capacity, (pos, consumer) -> {
            for (Direction direction : Direction.values()) consumer.accept(pos.relative(direction));
        }, pos -> {
            if (pos.equals(origin)) return BlockPos.TraversalNodeStatus.ACCEPT;
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER)) return BlockPos.TraversalNodeStatus.SKIP;
            if (state.getBlock() instanceof BucketPickup pickup && !pickup.pickupBlock(null, level, pos, state).isEmpty()) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
            if (state.getBlock() instanceof LiquidBlock) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)) {
                BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                dropResources(state, level, pos, blockEntity);
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else {
                return BlockPos.TraversalNodeStatus.SKIP;
            }
            return BlockPos.TraversalNodeStatus.ACCEPT;
        }) > 1;
    }
}
