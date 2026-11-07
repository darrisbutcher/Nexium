package net.storagenexus.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import org.jetbrains.annotations.Nullable;

/** Keeps the 3x3 chunks around it loaded. Placing it, or right-clicking it, outlines those chunks for a minute. */
public class DimensionalAnchorBlock extends BaseEntityBlock {
    public DimensionalAnchorBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.DIMENSIONAL_ANCHOR, DimensionalAnchorBlockEntity::serverTick);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalAnchorBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DimensionalAnchorBlockEntity be) {
            be.activate();
            if (placer instanceof Player p && level instanceof ServerLevel server) {
                be.toggleDisplay(server);
                p.sendOverlayMessage(Component.translatable("message.storagenexus.anchor_shown"));
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof DimensionalAnchorBlockEntity be) {
            player.sendOverlayMessage(Component.translatable(be.toggleDisplay(server)
                    ? "message.storagenexus.anchor_shown" : "message.storagenexus.anchor_hidden"));
        }
        return InteractionResult.SUCCESS;
    }
}
