package net.nexium.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.nexium.Nexium;

/**
 * The inventory key (E, or whatever it is set to) closes a Nexium window, as it closes vanilla's inventory, chests and the like, and
 * as Esc does: it calls the screen's own {@code onClose}. Container screens already behave like that (vanilla's does it), so this is
 * for the others: the Codex, the quest book, the settings, the Conduit Scanner, the Power Meter, the Elevator, the Armor menu, the
 * block list and the tag picker. While a text field is being typed in, the key types: it never closes the window then.
 */
public final class CloseOnInventoryKey {
    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!closesOnInventoryKey(screen)) return;
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                if (!client.options.keyInventory.matches(event) || !s.shouldCloseOnEsc() || typing(s)) return true;
                s.onClose();
                return false; // done: the screen does not see the key as well
            });
        });
    }

    /** A Nexium screen that is not a container screen (those close on the key by themselves). */
    static boolean closesOnInventoryKey(Screen screen) {
        return !(screen instanceof AbstractContainerScreen<?>) && screen.getClass().getName().startsWith("net.nexium.");
    }

    /** Whether a text field the screen has focused would take the key as a letter. */
    static boolean typing(Screen screen) {
        GuiEventListener focus = screen.getFocused();
        while (focus instanceof ContainerEventHandler container && container.getFocused() != null) focus = container.getFocused();
        return focus instanceof EditBox box && box.canConsumeInput();
    }

    private CloseOnInventoryKey() {}
}
