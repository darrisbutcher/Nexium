package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.util.Menus;
import net.storagenexus.util.StorageEvents;
import org.jetbrains.annotations.Nullable;

/** Reads every inventory touching it (all six sides) into the network. */
public class StorageConnectorBlock extends ArmedNetworkBlock {
    public StorageConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ConnectorBlockEntity be) {
            Menus.open(sp, getName(), pos, (id, inv, p) -> new ConnectorMenu(id, inv, be));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        StorageEvents.storageChanged();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock,
                                   @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        // a chest placed, broken or swapped next to us
        if (level.getBlockEntity(pos) instanceof ConnectorBlockEntity be && be.neighbourInventoriesChanged()) {
            StorageEvents.storageChanged();
        }
    }
}
