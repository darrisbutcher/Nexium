package net.storagenexus.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.storagenexus.upgrade.UpgradeMenu;
import net.storagenexus.util.Fmt;
import net.storagenexus.util.KeepContents;
import net.storagenexus.util.Menus;
import net.storagenexus.util.StorageEvents;
import org.jetbrains.annotations.Nullable;

public class FluidTankBlock extends BaseEntityBlock {
    public FluidTankBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FLUID_TANK, FluidDrawerBlockEntity::serverTick);
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        KeepContents.dropInCreative(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be)) return InteractionResult.PASS;
        if (stack.getItem() instanceof UpgradeItem up && up.kind().isStorageUpgrade()) {
            if (!level.isClientSide()) {
                boolean ok = be.tryInstall(stack);
                player.sendOverlayMessage(Component.translatable(ok ? "message.storagenexus.upgrade_installed"
                        : "message.storagenexus.upgrade_rejected", stack.getHoverName()));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            if (!FluidStorageUtil.interactWithFluidStorage(be.slot(0), player, hand)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            if (player.isSecondaryUseActive()) {
                Menus.open(sp, getName(), pos, (id, inv, p) -> new UpgradeMenu(id, inv, be, be));
            } else {
                FluidDrawerBlockEntity.FluidSlot slot = be.slot(0);
                Component name = slot.isResourceBlank() ? Component.translatable("message.storagenexus.empty")
                        : net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(slot.getResource());
                player.sendOverlayMessage(Component.translatable("message.storagenexus.tank_contents", name,
                        Fmt.fluid(slot.getAmount()), Fmt.fluid(slot.capacity())));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
