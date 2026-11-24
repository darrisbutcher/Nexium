package net.nexium.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.nexium.registry.ModBlockEntities;
import org.jspecify.annotations.Nullable;

/** The Redstone Reactor: nine fuel rods, ice for coolant, and the most Nexus Energy of any generator. */
public class RedstoneReactorBlock extends BaseEntityBlock {
    public RedstoneReactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneReactorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.REDSTONE_REACTOR, RedstoneReactorBlockEntity::serverTick);
    }

    /** A bucket of water fills the tank; anything else opens the screen. */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof RedstoneReactorBlockEntity reactor)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        var held = net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack).find(net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM);
        if (held == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil.interactWithFluidStorage(reactor.tank(), player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RedstoneReactorBlockEntity reactor) player.openMenu(reactor);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
