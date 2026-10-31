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
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.storagenexus.util.Fmt;
import org.jetbrains.annotations.Nullable;

/** Pulls fluid from source blocks next to it and pushes it into conduits, fluid drawers or the network. */
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

    /** The fluid a bucket (or any fluid container) holds, or null. */
    private static @Nullable FluidVariant fluidIn(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Storage<FluidVariant> storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
        if (storage == null) return null;
        for (StorageView<FluidVariant> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0) return view.getResource();
        }
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FluidIntakeBlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        FluidVariant fluid = fluidIn(stack);
        if (fluid == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            be.setFilter(fluid);
            player.sendOverlayMessage(Component.translatable("message.storagenexus.intake_filter_set",
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(fluid)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidIntakeBlockEntity be) {
            if (player.isSecondaryUseActive()) {
                be.setFilter(FluidVariant.blank());
                player.sendOverlayMessage(Component.translatable("message.storagenexus.intake_filter_cleared"));
            } else {
                Component filter = be.filter().isBlank() ? Component.translatable("message.storagenexus.intake_any")
                        : net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(be.filter());
                player.sendOverlayMessage(Component.translatable("message.storagenexus.intake_status", Fmt.fluid(be.bufferAmount()), filter));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
