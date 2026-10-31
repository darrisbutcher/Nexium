package net.storagenexus.drawer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/** Drawer that auto-converts between up to three forms (e.g. nuggets, ingots and blocks). */
public class CompactingDrawerBlock extends FacingDrawerBlock {
    public CompactingDrawerBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected int frontSlots(BlockState state) {
        return 3;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompactingDrawerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.COMPACTING_DRAWER, CompactingDrawerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useFrontWithItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                 InteractionHand hand, int slot, boolean doubleClick) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CompactingDrawerBlockEntity be) {
            if (doubleClick) be.insertAllFromInventory(slot, player);
            else be.insertFromHand(slot, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useFrontEmpty(BlockState state, Level level, BlockPos pos, Player player, int slot, boolean doubleClick) {
        if (!level.isClientSide() && doubleClick && level.getBlockEntity(pos) instanceof CompactingDrawerBlockEntity be) {
            be.insertAllFromInventory(slot, player);
        }
        return InteractionResult.SUCCESS;
    }
}
