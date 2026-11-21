package net.nexium.essentials.hostile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.nexium.util.Menus;
import org.jspecify.annotations.Nullable;

/**
 * The Hostile Mob Switch. Right-click it for its screen, which turns the world's hostile mob spawning off and on (see
 * {@link HostileMobSwitch}). A redstone signal on it follows too: power turns the spawning off, and losing the power turns it back on.
 * Made at the Farm Assembler from 140 hostile mobs, so it drops with any tool.
 */
public class HostileMobSwitchBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public HostileMobSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    /** A switch placed against a powered source starts powered, which does not by itself change the setting. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Menus.open(sp, Component.translatable("block.nexium.hostile_mob_switch"), pos,
                    (id, inventory, p) -> new HostileMobSwitchMenu(id, inventory, ContainerLevelAccess.create(level, pos)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (!level.isClientSide()) HostileMobSwitch.get(level.getServer()).setBlocking(powered);
    }
}
