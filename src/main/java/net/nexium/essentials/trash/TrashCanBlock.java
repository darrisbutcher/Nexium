package net.nexium.essentials.trash;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.util.Menus;

/**
 * A bin that destroys every item put in it. Pipe items into it with a hopper, a dropper, an Item Conduit or an Import/Export Bus
 * (it shows them an inventory that takes everything and keeps nothing), or right-click it for a screen with a slot that destroys
 * whatever is dropped on it. Nothing it destroys can be got back.
 */
public class TrashCanBlock extends Block {
    public TrashCanBlock(Properties properties) {
        super(properties);
    }

    /** Registers the inventory pipes see; called once from the mod initializer. */
    public static void init() {
        ItemStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, side) -> TrashStorage.INSTANCE, ModBlocks.TRASH_CAN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Menus.open(sp, Component.translatable("block.nexium.trash_can"), pos,
                    (id, inventory, p) -> new TrashCanMenu(id, inventory, ContainerLevelAccess.create(level, pos)));
        }
        return InteractionResult.SUCCESS;
    }
}
