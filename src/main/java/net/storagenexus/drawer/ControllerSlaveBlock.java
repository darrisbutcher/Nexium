package net.storagenexus.drawer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.util.StorageEvents;

/**
 * Extends a Drawer Controller's cluster. The 16-block reach restarts from every slave,
 * so a chain of slaves can cover a large drawer wall.
 */
public class ControllerSlaveBlock extends Block {
    public ControllerSlaveBlock(Properties properties) {
        super(properties);
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
}
