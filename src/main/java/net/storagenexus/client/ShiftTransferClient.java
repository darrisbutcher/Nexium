package net.storagenexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Two transfer tweaks for any chest-like screen, with an empty cursor:
 * <ul>
 * <li>Space + left click on an item sends every stack of that same item in that inventory to the other one, not just
 * the stack clicked.</li>
 * <li>Shift + left click and drag moves every item the cursor passes over into the other inventory (the first press is
 * the ordinary shift-click).</li>
 * </ul>
 * Each stack is moved with the ordinary shift-click, so where it lands is the screen's own rule. In the survival
 * inventory screen only the shift drag is added, between the hotbar and the main inventory (the other slots there keep
 * vanilla's clicks and Space + click is off). The creative menu and crafting results are left alone.
 */
public final class ShiftTransferClient {
    private static boolean dragging;
    private static double lastX, lastY;
    private static final Set<Integer> visited = new HashSet<>();

    private ShiftTransferClient() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container) || screen instanceof CreativeModeInventoryScreen) return;
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !click(container, event));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !drag(container, event));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !release());
        });
    }

    /** Whether this screen takes part at all. */
    private static boolean applies(AbstractContainerScreen<?> screen) {
        return screen.getMenu().getCarried().isEmpty();
    }

    /** The player's own inventory screen, where only the hotbar and the main inventory take part. */
    private static boolean survivalInventory(AbstractContainerScreen<?> screen) {
        return screen.getMenu() instanceof InventoryMenu;
    }

    /** Whether the slot may be touched in this screen: all of them, but in the survival inventory only the hotbar and main inventory. */
    private static boolean allowed(AbstractContainerScreen<?> screen, Slot slot) {
        return !survivalInventory(screen) || slot.container instanceof Inventory && slot.getContainerSlot() <= 35;
    }

    /** The slot a shift-click may move: a real storage slot, not a crafting result, armor or the offhand. */
    private static boolean movable(Slot slot) {
        if (!slot.isActive() || !slot.hasItem() || slot instanceof ResultSlot) return false;
        if (slot.container.getContainerSize() <= 1) return false;
        if (slot.container instanceof Inventory && slot.getContainerSlot() > 35) return false;
        return slot.mayPickup(Minecraft.getInstance().player);
    }

    private static Slot slotAt(AbstractContainerScreen<?> screen, double x, double y) {
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            double left = screen.leftPos + slot.x, top = screen.topPos + slot.y;
            if (x >= left && x < left + 16 && y >= top && y < top + 16) return slot;
        }
        return null;
    }

    private static void quickMove(AbstractContainerScreen<?> screen, Slot slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
    }

    /** Whether the Space bar is held. */
    private static boolean spaceDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_SPACE);
    }

    /**
     * Space + left press: sends every stack of the clicked item to the other inventory. Shift + left press: the ordinary
     * shift-click of that one stack, and a drag may follow. Returns whether it took the click.
     */
    private static boolean click(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !applies(screen)) return false;
        boolean space = spaceDown() && !survivalInventory(screen);
        if (!space && !Minecraft.getInstance().hasShiftDown()) return false;
        Slot slot = slotAt(screen, event.x(), event.y());
        if (slot == null || !allowed(screen, slot)) return false;
        if (slot.hasItem() && !movable(slot)) return false; // a result slot and the like keep their own shift-click
        if (space) {
            if (!slot.hasItem()) return true;
            ItemStack wanted = slot.getItem().copy();
            quickMove(screen, slot);
            for (Slot other : screen.getMenu().slots) {
                if (other == slot || other.container != slot.container || !movable(other)) continue;
                if (ItemStack.isSameItemSameComponents(other.getItem(), wanted)) quickMove(screen, other);
            }
            return true;
        }
        dragging = true;
        lastX = event.x();
        lastY = event.y();
        visited.clear();
        visited.add(slot.index);
        if (slot.hasItem()) quickMove(screen, slot);
        return true;
    }

    /** While dragging with shift held: every slot the cursor crosses is shift-clicked once. Returns whether it took the drag. */
    private static boolean drag(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (!dragging) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !Minecraft.getInstance().hasShiftDown() || !screen.getMenu().getCarried().isEmpty()) {
            dragging = false;
            return false;
        }
        // The cursor can jump several slots between two events: look along the way
        double dx = event.x() - lastX, dy = event.y() - lastY;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 4.0));
        for (int i = 1; i <= steps; i++) {
            Slot slot = slotAt(screen, lastX + dx * i / steps, lastY + dy * i / steps);
            if (slot != null && allowed(screen, slot) && visited.add(slot.index) && movable(slot)) quickMove(screen, slot);
        }
        lastX = event.x();
        lastY = event.y();
        return true;
    }

    /** The button comes up: the drag is over. Returns whether a drag was going (and so the release is ours). */
    private static boolean release() {
        boolean was = dragging;
        dragging = false;
        visited.clear();
        return was;
    }
}
