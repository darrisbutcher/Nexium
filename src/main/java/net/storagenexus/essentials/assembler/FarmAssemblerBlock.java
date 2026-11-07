package net.storagenexus.essentials.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.util.Menus;
import org.jspecify.annotations.Nullable;

/** Makes the one-block farms and the Master Librarian. Right-click it for its screen. */
public class FarmAssemblerBlock extends BaseEntityBlock {
    public FarmAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FarmAssemblerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof FarmAssemblerBlockEntity assembler) {
            Menus.open(sp, Component.translatable("block.nexusessentials.farm_assembler"), pos,
                    (id, inventory, p) -> new FarmAssemblerMenu(id, inventory, assembler));
        }
        return InteractionResult.SUCCESS;
    }
}
