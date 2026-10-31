package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.util.Menus;

/**
 * Reads only the inventory it faces, through a whitelist/blacklist filter and with a priority,
 * so ores can go to one area and junk to another.
 */
public class FilteredConnectorBlock extends StorageConnectorBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public FilteredConnectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** The side it reads always has its own snout, so it never gets an ordinary arm too. */
    @Override
    protected BlockState withArms(BlockState state, Level level, BlockPos pos) {
        state = super.withArms(state, level, pos);
        return state.setValue(PROPS.get(state.getValue(FACING)), false);
    }

    /** Faces the block it was placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withArms(defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING)) return state;
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected int shapeMask(BlockState state) {
        return super.shapeMask(state) | 1 << state.getValue(FACING).get3DDataValue();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = rotation.rotate(state.getValue(FACING));
        return state.setValue(PROPS.get(state.getValue(FACING)), false).setValue(FACING, facing);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ConnectorBlockEntity be) {
            Menus.open(sp, getName(), pos, (id, inv, p) -> new ConnectorMenu(id, inv, be));
        }
        return InteractionResult.SUCCESS;
    }
}
