package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.storagenexus.dimension.DimensionalOutputBlock;
import net.storagenexus.fluid.FluidIntakeBlock;
import net.storagenexus.util.PipeShapes;
import net.storagenexus.util.StorageEvents;

import java.util.Map;

/**
 * A small hub with conduit-style arms towards everything it can link to: network blocks and
 * anything with an inventory. Base of the connectors, the Import/Export Bus and the Level Emitter.
 */
public abstract class ArmedNetworkBlock extends BaseEntityBlock implements NetworkBlock {
    public static final Map<Direction, BooleanProperty> PROPS = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final PipeShapes SHAPES = new PipeShapes(5, 11, 6.5, 9.5);

    protected ArmedNetworkBlock(Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any();
        for (BooleanProperty p : PROPS.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPS.values().forEach(builder::add);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * An arm shows towards network blocks and anything with a block entity (chests, machines, drawers...),
     * but not a Fluid Intake: that needs an Import/Export Bus.
     */
    protected static boolean hasArm(BlockState neighbor) {
        Block block = neighbor.getBlock();
        if (block instanceof FluidIntakeBlock) return false;
        return block instanceof NetworkBlock || block instanceof DimensionalOutputBlock || neighbor.hasBlockEntity();
    }

    /** Arm flags for all sides; subclasses can claim a side for something else. */
    protected BlockState withArms(BlockState state, Level level, BlockPos pos) {
        for (Direction d : Direction.values()) state = state.setValue(PROPS.get(d), hasArm(level.getBlockState(pos.relative(d))));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withArms(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPS.get(direction), hasArm(neighborState));
    }

    protected int shapeMask(BlockState state) {
        int mask = 0;
        for (Direction d : Direction.values()) if (state.getValue(PROPS.get(d))) mask |= 1 << d.get3DDataValue();
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(shapeMask(state));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) StorageEvents.storageChanged();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        StorageEvents.storageChanged();
    }
}
