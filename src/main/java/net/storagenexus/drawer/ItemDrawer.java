package net.storagenexus.drawer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Player-facing actions shared by normal and compacting drawers. */
public interface ItemDrawer {
    int slotCount();

    /** Inserts as much of the stack as fits into the slot, shrinking it. */
    void insertFromHand(int slot, ItemStack stack);

    /** Double-click: moves every matching item from the player's inventory into the slot. */
    void insertAllFromInventory(int slot, Player player);

    /** Left-click: gives the player a stack (or one item when sneaking). */
    void extractToPlayer(int slot, Player player, boolean single);
}
