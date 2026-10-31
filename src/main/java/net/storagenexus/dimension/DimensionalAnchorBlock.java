package net.storagenexus.dimension;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Loads a 3x3 chunk area. Right-click to switch between Always and On-demand (tablets only). */
public class DimensionalAnchorBlock extends BaseEntityBlock {
    public DimensionalAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DimensionalAnchorBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
            if (placer instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.storagenexus.anchor_mode.always"), true);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DimensionalAnchorBlockEntity be) {
            be.toggleMode();
            player.displayClientMessage(Component.translatable(be.isOnDemand()
                    ? "message.storagenexus.anchor_mode.on_demand" : "message.storagenexus.anchor_mode.always"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
