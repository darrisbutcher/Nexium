package net.storagenexus.drawer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.upgrade.UpgradeItem;
import net.storagenexus.upgrade.UpgradeMenu;
import net.storagenexus.upgrade.Upgradeable;
import net.storagenexus.util.Menus;
import net.storagenexus.util.StorageEvents;

/**
 * Base for every block with a drawer-style front: item drawers, the compacting drawer
 * and fluid drawers. Handles facing, installing upgrades by right-clicking with them,
 * and sneak + empty hand to open the upgrade slots.
 */
public abstract class FacingDrawerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected FacingDrawerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) StorageEvents.storageChanged();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        StorageEvents.storageChanged();
    }

    /** Number of front slots, used for hit testing. */
    protected abstract int frontSlots(BlockState state);

    /** Right-click on the front with an item (not an upgrade). */
    protected abstract InteractionResult useFrontWithItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                          Player player, InteractionHand hand, int slot, boolean doubleClick);

    /** Right-click on the front with an empty hand. */
    protected abstract InteractionResult useFrontEmpty(BlockState state, Level level, BlockPos pos, Player player,
                                                       int slot, boolean doubleClick);

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (stack.getItem() instanceof UpgradeItem up && up.kind().isStorageUpgrade() && be instanceof Upgradeable upgradeable) {
            if (!level.isClientSide()) {
                if (upgradeable.tryInstall(stack)) {
                    player.displayClientMessage(Component.translatable("message.storagenexus.upgrade_installed", stack.getHoverName()), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.storagenexus.upgrade_rejected"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        int slot = DrawerInteraction.hitSlot(state, hit, frontSlots(state));
        if (slot < 0) return InteractionResult.PASS;
        boolean dbl = !level.isClientSide() && DrawerInteraction.isDoubleClick(player, pos, level.getGameTime());
        return useFrontWithItem(stack, state, level, pos, player, hand, slot, dbl);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (player.isSecondaryUseActive() && be instanceof Upgradeable upgradeable) {
            if (player instanceof ServerPlayer sp) {
                Menus.open(sp, getName(), pos, (id, inv, p) -> new UpgradeMenu(id, inv, upgradeable, be));
            }
            return InteractionResult.SUCCESS;
        }
        int slot = DrawerInteraction.hitSlot(state, hit, frontSlots(state));
        if (slot < 0) return InteractionResult.PASS;
        boolean dbl = !level.isClientSide() && DrawerInteraction.isDoubleClick(player, pos, level.getGameTime());
        return useFrontEmpty(state, level, pos, player, slot, dbl);
    }
}
