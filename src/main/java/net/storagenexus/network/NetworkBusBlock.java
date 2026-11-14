package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.util.Menus;
import org.jetbrains.annotations.Nullable;

/**
 * Import/Export Bus: moves items and fluids between the inventory it faces and the network.
 * One block with a mode switch: Import pulls from the inventory into the network, Export pushes
 * from the network into the inventory. An optional filter limits what moves.
 */
public class NetworkBusBlock extends ArmedNetworkBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** False: Import (inventory to network). True: Export (network to inventory). */
    public static final BooleanProperty EXPORT = BooleanProperty.create("export");

    public NetworkBusBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(EXPORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, EXPORT);
    }

    /** Faces the block it was placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withArms(defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    /** This state turned to face another side, with the arms worked out again: the old side may need one, the new one never does. */
    public BlockState turnedTo(BlockState state, Level level, BlockPos pos, Direction face) {
        return withArms(state.setValue(FACING, face), level, pos);
    }

    /** The side it works on has its own snout, so it never gets an ordinary arm too. */
    @Override
    protected BlockState withArms(BlockState state, Level level, BlockPos pos) {
        state = super.withArms(state, level, pos);
        return state.setValue(PROPS.get(state.getValue(FACING)), false);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkBusBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.NETWORK_BUS, NetworkBusBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof NetworkBusBlockEntity be) {
            Menus.open(sp, getName(), pos, (id, inv, p) -> new NetworkBusMenu(id, inv, be));
        }
        return InteractionResult.SUCCESS;
    }
}
