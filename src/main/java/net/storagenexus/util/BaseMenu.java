package net.storagenexus.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Shared player-inventory layout and shift-click handling for simple menus. */
public abstract class BaseMenu extends AbstractContainerMenu {
    protected int customSlotCount = 0;
    protected int playerSlotStart = -1;

    protected BaseMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    protected Slot addCustomSlot(Slot slot) {
        customSlotCount++;
        return addSlot(slot);
    }

    protected void addPlayerSlots(Inventory inventory, int left, int top) {
        playerSlotStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, left + col * 18, top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < playerSlotStart) {
            if (!moveItemStackTo(stack, playerSlotStart, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (customSlotCount == 0 || !moveItemStackTo(stack, 0, playerSlotStart, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
