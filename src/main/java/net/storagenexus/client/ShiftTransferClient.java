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
 * Two shift-click tweaks for any chest-like screen, with an empty cursor:
 * <ul>
 * <li>Shift + left click on an item sends every stack of that same item in that inventory to the other one, not just
 * the stack clicked.</li>
 * <li>Shift + left click and drag moves every item the cursor passes over into the other inventory.</li>
 * </ul>
 * Each stack is moved with the ordinary shift-click, so where it lands is the screen's own rule. The survival
 * inventory, the creative menu and crafting results are left alone.
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
        return !(screen.getMenu() instanceof InventoryMenu) && screen.getMenu().getCarried().isEmpty();
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

    /** Shift + left press: moves every stack of the clicked item, and starts a drag. Returns whether it took the click. */
    private static boolean click(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !Minecraft.getInstance().hasShiftDown() || !applies(screen)) return false;
        Slot slot = slotAt(screen, event.x(), event.y());
        if (slot == null) return false;
        if (slot.hasItem() && !movable(slot)) return false; // a result slot and the like keep their own shift-click
        dragging = true;
        lastX = event.x();
        lastY = event.y();
        visited.clear();
        visited.add(slot.index);
        if (slot.hasItem()) {
            ItemStack wanted = slot.getItem().copy();
            quickMove(screen, slot);
            for (Slot other : screen.getMenu().slots) {
                if (other == slot || other.container != slot.container || !movable(other)) continue;
                if (ItemStack.isSameItemSameComponents(other.getItem(), wanted)) {
                    visited.add(other.index);
                    quickMove(screen, other);
                }
            }
        }
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
            if (slot != null && visited.add(slot.index) && movable(slot)) quickMove(screen, slot);
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
