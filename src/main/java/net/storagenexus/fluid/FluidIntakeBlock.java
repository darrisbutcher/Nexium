package net.storagenexus.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.upgrade.UpgradeItem;
import net.storagenexus.util.Fmt;
import org.jetbrains.annotations.Nullable;

/** Pulls fluid from source blocks next to it and pushes it into conduits, tanks or the network. */
public class FluidIntakeBlock extends BaseEntityBlock {
    public FluidIntakeBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidIntakeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FLUID_INTAKE, FluidIntakeBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof UpgradeItem up && up.kind() == UpgradeItem.Kind.INTAKE_RANGE
                && level.getBlockEntity(pos) instanceof FluidIntakeBlockEntity be) {
            if (!level.isClientSide()) {
                if (be.installRangeUpgrade()) {
                    stack.shrink(1);
                    player.sendOverlayMessage(Component.translatable("message.storagenexus.upgrade_installed", stack.getHoverName()));
                } else {
                    player.sendOverlayMessage(Component.translatable("message.storagenexus.upgrade_rejected"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidIntakeBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.storagenexus.intake_status",
                    Fmt.fluid(be.bufferAmount()), be.hasRangeUpgrade() ? "5x5" : "adjacent"));
        }
        return InteractionResult.SUCCESS;
    }
}
