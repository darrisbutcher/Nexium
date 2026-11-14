package net.storagenexus.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.terminal.TerminalMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * The scroll wheel over a stack moves one item at a time between the player's inventory (the hotbar too) and the other
 * inventory of the screen, with an empty cursor:
 * <ul>
 * <li>Scroll down sends one item of the stack under the mouse to the other inventory.</li>
 * <li>Scroll up pulls one matching item from the other inventory into the stack under the mouse (or, when that stack is
 * full, into its inventory).</li>
 * </ul>
 * It is done with ordinary slot clicks (pick the stack up, put one down, put the rest back), so each slot's own rules
 * decide what fits. Chests, barrels and machines only: the survival inventory screen, the creative menu and the
 * terminals keep vanilla's wheel.
 */
public final class ScrollTransferClient {
    /** The part of a notch not yet used, for mice and touchpads that scroll in fractions. */
    private static double pending;

    private ScrollTransferClient() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container) || !takesPart(container)) return;
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> !scroll(container, x, y, vertical));
        });
    }

    /** Whether the screen is one with a second inventory to move to and from. */
    private static boolean takesPart(AbstractContainerScreen<?> screen) {
        return !(screen instanceof CreativeModeInventoryScreen)
                && !(screen.getMenu() instanceof InventoryMenu)
                && !(screen.getMenu() instanceof TerminalMenu);
    }

    /** A slot of the player's own storage: the main inventory or the hotbar. */
    private static boolean isPlayer(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() <= 35;
    }

    /** A slot that takes part: shown, not a crafting result, and not armor or the offhand. */
    private static boolean usable(Slot slot) {
        if (!slot.isActive() || slot instanceof ResultSlot) return false;
        return !(slot.container instanceof Inventory) || slot.getContainerSlot() <= 35;
    }

    /** Every usable slot of the other inventory: the player's for a machine slot, the machine's for a player slot. */
    private static List<Slot> otherSide(AbstractContainerScreen<?> screen, Slot from) {
        boolean wantPlayer = !isPlayer(from);
        List<Slot> list = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (usable(slot) && isPlayer(slot) == wantPlayer) list.add(slot);
        }
        return list;
    }

    /** Every usable slot in the same inventory as this one. */
    private static List<Slot> sameSide(AbstractContainerScreen<?> screen, Slot of) {
        List<Slot> list = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (usable(slot) && (isPlayer(of) ? isPlayer(slot) : !isPlayer(slot) && slot.container == of.container)) list.add(slot);
        }
        return list;
    }

    /** Whether one more of this item fits on top of what the slot holds. */
    private static boolean takesMore(Slot slot, ItemStack stack) {
        return slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), stack)
                && slot.getItem().getCount() < slot.getMaxStackSize(stack) && slot.mayPlace(stack);
    }

    /** Where one item goes: a stack of the same item with room first, then an empty slot. */
    private static Slot target(List<Slot> slots, ItemStack stack, Slot not) {
        for (Slot slot : slots) {
            if (slot != not && takesMore(slot, stack)) return slot;
        }
        for (Slot slot : slots) {
            if (slot != not && !slot.hasItem() && slot.mayPlace(stack) && slot.getMaxStackSize(stack) > 0) return slot;
        }
        return null;
    }

    /** Whether a slot can give its stack up and take it back: needed to lift it for a moment. */
    private static boolean liftable(Slot slot, Player player) {
        return slot.hasItem() && slot.mayPickup(player) && slot.mayPlace(slot.getItem());
    }

    private static void click(AbstractContainerScreen<?> screen, Slot slot, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, button, ContainerInput.PICKUP, mc.player);
    }

    /** Lifts the whole stack, sets one down on the target and puts the rest back where it came from. */
    private static void moveOne(AbstractContainerScreen<?> screen, Slot from, Slot to) {
        click(screen, from, 0);
        click(screen, to, 1);
        if (!screen.getMenu().getCarried().isEmpty()) click(screen, from, 0);
    }

    /** One item of the hovered stack to the other inventory. Returns whether one moved. */
    private static boolean send(AbstractContainerScreen<?> screen, Slot hovered, Player player) {
        if (!liftable(hovered, player)) return false;
        Slot to = target(otherSide(screen, hovered), hovered.getItem(), hovered);
        if (to == null) return false;
        moveOne(screen, hovered, to);
        return true;
    }

    /** One matching item from the other inventory into the hovered stack. Returns whether one moved. */
    private static boolean pull(AbstractContainerScreen<?> screen, Slot hovered, Player player) {
        ItemStack wanted = hovered.getItem();
        Slot from = null;
        for (Slot slot : otherSide(screen, hovered)) {
            if (!liftable(slot, player) || !ItemStack.isSameItemSameComponents(slot.getItem(), wanted)) continue;
            if (from == null || slot.getItem().getCount() < from.getItem().getCount()) from = slot; // the smallest pile first
        }
        if (from == null) return false;
        ItemStack stack = from.getItem();
        Slot to = takesMore(hovered, stack) ? hovered : target(sameSide(screen, hovered), stack, hovered);
        if (to == null) return false;
        moveOne(screen, from, to);
        return true;
    }

    /**
     * One wheel notch over a stack. Down sends an item to the other inventory, up pulls one in. Returns whether the
     * scroll was ours (a stack under the mouse), so the screen does not also handle it.
     */
    private static boolean scroll(AbstractContainerScreen<?> screen, double x, double y, double amount) {
        Minecraft mc = Minecraft.getInstance();
        if (amount == 0 || mc.player == null || !screen.getMenu().getCarried().isEmpty()) return false;
        Slot hovered = ShiftTransferClient.slotAt(screen, x, y);
        if (hovered == null || !usable(hovered) || !hovered.hasItem()) return false;
        if (Math.signum(amount) != Math.signum(pending)) pending = 0;
        pending += amount;
        int notches = (int) Math.max(-64, Math.min(64, pending));
        pending -= notches;
        for (int i = 0; i < Math.abs(notches) && hovered.hasItem(); i++) {
            boolean moved = notches < 0 ? send(screen, hovered, mc.player) : pull(screen, hovered, mc.player);
            if (!moved) break;
        }
        return true;
    }
}
