package net.storagenexus.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
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
import net.storagenexus.drawer.FacingDrawerBlock;
import net.storagenexus.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/** Fluid drawer (1x1 or 2x2). Click a slot with a bucket to fill or empty it. */
public class FluidDrawerBlock extends FacingDrawerBlock {
    private final int slots;

    public FluidDrawerBlock(Properties properties, int slots) {
        super(properties);
        this.slots = slots;
    }

    public int slots() { return slots; }


    @Override
    protected int frontSlots(BlockState state) { return slots; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidDrawerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FLUID_DRAWER, FluidDrawerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useFrontWithItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                 InteractionHand hand, int slot, boolean doubleClick) {
        if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity be && slot < be.slotCount()) {
            if (!level.isClientSide()) FluidStorageUtil.interactWithFluidStorage(be.slot(slot), player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useFrontEmpty(BlockState state, Level level, BlockPos pos, Player player, int slot, boolean doubleClick) {
        return InteractionResult.PASS;
    }
}
