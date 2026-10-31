package net.storagenexus.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.util.PipeShapes;
import net.storagenexus.util.StorageEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** The block that conduit items place. Holds item and/or fluid conduits. */
public class ConduitBundleBlock extends BaseEntityBlock {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;
    /** Tier of each conduit type (0 = none), so the model can show it. */
    public static final IntegerProperty ITEM_TIER = IntegerProperty.create("item_tier", 0, 2);
    public static final IntegerProperty FLUID_TIER = IntegerProperty.create("fluid_tier", 0, 2);
    public static final PipeShapes SHAPES = new PipeShapes(6.5, 9.5); // half-size conduits

    public ConduitBundleBlock(Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any().setValue(ITEM_TIER, 0).setValue(FLUID_TIER, 0);
        for (BooleanProperty p : CONNECTIONS.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
        builder.add(ITEM_TIER, FLUID_TIER);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConduitBundleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CONDUIT_BUNDLE, ConduitBundleBlockEntity::serverTick);
    }

    public static BlockState computeState(BlockState state, Level level, BlockPos pos, ConduitBundleBlockEntity be) {
        int mask = be.connectionMask();
        BlockState s = state.setValue(ITEM_TIER, be.tier(ConduitType.ITEM))
                .setValue(FLUID_TIER, be.tier(ConduitType.FLUID));
        for (Direction d : Direction.values()) s = s.setValue(CONNECTIONS.get(d), (mask & (1 << d.get3DDataValue())) != 0);
        return s;
    }

    public static int mask(BlockState state) {
        int mask = 0;
        for (Direction d : Direction.values()) if (state.getValue(CONNECTIONS.get(d))) mask |= 1 << d.get3DDataValue();
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be && be.facade() != null) return Shapes.block();
        return SHAPES.get(mask(state));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (level instanceof Level l && l.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be) {
            BlockState updated = computeState(state, l, pos, be);
            if (updated != state) StorageEvents.conduitsChanged();
            return updated;
        }
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be)) return;
        BlockState updated = computeState(state, level, pos, be);
        if (updated != state) {
            StorageEvents.conduitsChanged();
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    // ---- Facades ----------------------------------------------------------------------------

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ConduitItem || stack.getItem() instanceof ConduitWrenchItem) return InteractionResult.PASS;
        if (stack.getItem() instanceof BlockItem bi && level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be && be.facade() == null) {
            BlockState facade = bi.getBlock().defaultBlockState();
            if (facade.isCollisionShapeFullBlock(level, pos) && !(bi.getBlock() instanceof BaseEntityBlock)) {
                if (!level.isClientSide()) {
                    be.setFacade(facade);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
