package net.storagenexus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Storage blocks keep their contents in the dropped item (see the loot tables' copy_components).
 * Creative players get no loot, so like shulker boxes a non-empty block still drops itself then.
 */
public final class KeepContents {
    public static void dropInCreative(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide() || !player.preventsBlockDrops()) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;
        DataComponentMap components = be.collectComponents();
        if (components.isEmpty()) return;
        ItemStack stack = new ItemStack(state.getBlock());
        stack.applyComponents(components);
        Block.popResource(level, pos, stack);
    }

    private KeepContents() {}
}
