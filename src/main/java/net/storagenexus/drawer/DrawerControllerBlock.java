package net.storagenexus.drawer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.util.StorageEvents;
import org.jetbrains.annotations.Nullable;

/** One input for a whole drawer wall: right-click to sort items into matching drawers. */
public class DrawerControllerBlock extends BaseEntityBlock {
    public DrawerControllerBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrawerControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.DRAWER_CONTROLLER, DrawerControllerBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        StorageEvents.storageChanged();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        StorageEvents.storageChanged();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DrawerControllerBlockEntity be)) return InteractionResult.PASS;
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND; // info, reach, double-click dump
        if (stack.getItem() instanceof BucketItem || stack.is(Items.BUCKET)) {
            if (!level.isClientSide()) FluidStorageUtil.interactWithFluidStorage(be.getFluidStorage(), player, hand);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            boolean dbl = DrawerInteraction.isDoubleClick(player, pos, level.getGameTime());
            int moved = dbl ? be.insertAllFromInventory(player) : be.insertFromHand(stack);
            if (moved == 0 && !dbl) {
                player.sendOverlayMessage(Component.translatable("message.storagenexus.no_matching_drawer"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DrawerControllerBlockEntity be) {
            if (DrawerInteraction.isDoubleClick(player, pos, level.getGameTime())) {
                be.insertAllFromInventory(player);
            } else {
                boolean showing = level instanceof ServerLevel server && be.toggleRangeDisplay(server);
                player.sendOverlayMessage(Component.translatable(showing ? "message.storagenexus.controller_info"
                        : "message.storagenexus.controller_info_hidden", be.drawerCount(), DrawerControllerBlockEntity.RANGE));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
