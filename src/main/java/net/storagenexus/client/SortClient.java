package net.storagenexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.storagenexus.sort.InventorySorter;
import net.storagenexus.sort.InventorySorter.Region;
import net.storagenexus.sort.SortPayload;

/**
 * The sort key (O by default, so it never fights JEI's, REI's or EMI's R): over a container's slots it sorts the
 * container, over the main inventory it sorts that, and never both and never the hotbar. It works with the mouse over
 * an item as well as over an empty slot, and sorts A to Z.
 */
public final class SortClient {
    public static final KeyMapping SORT = new KeyMapping("key.storagenexus.sort_inventory",
            InputConstants.KEY_O, NexusEssentialsClient.CATEGORY);

    private SortClient() {}

    public static void init() {
        KeyMappingHelper.registerKeyMapping(SORT);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container) || screen instanceof CreativeModeInventoryScreen) return;
            // Drawn after everything else, so it is on top of the panel even for a tall barrel
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> drawMessage(g, s.width));
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> {
                if (!SORT.matches(event) || s.getFocused() instanceof EditBox) return;
                press(container);
            });
        });
    }

    /** The line saying which order was used, shown over the top of the screen for a couple of seconds. */
    private static Component message;
    private static long messageUntil;
    private static final long MESSAGE_MS = 2500;

    private static void drawMessage(net.minecraft.client.gui.GuiGraphicsExtractor g, int screenWidth) {
        if (message == null || System.currentTimeMillis() > messageUntil) return;
        var font = Minecraft.getInstance().font;
        int w = font.width(message);
        int x = (screenWidth - w) / 2, y = 8;
        g.fill(x - 6, y - 4, x + w + 6, y + 12, 0xE0101010);
        g.fill(x - 6, y - 4, x + w + 6, y - 3, 0xFFE8A62A);
        g.text(font, message, x, y, 0xFFFFFFFF, true);
    }

    /** Handles one press with the mouse where it is now. Returns whether a sort was requested. */
    public static boolean press(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        double x = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double y = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        return pressAt(screen, x, y);
    }

    /** Same, with the mouse at a given GUI position. */
    public static boolean pressAt(AbstractContainerScreen<?> screen, double x, double y) {
        Region region = InventorySorter.regionAt(screen.getMenu(), x - screen.leftPos, y - screen.topPos);
        if (region == null) return false;
        ClientPlayNetworking.send(new SortPayload(screen.getMenu().containerId, region == Region.PLAYER));
        message = Component.translatable("message.storagenexus.sort");
        messageUntil = System.currentTimeMillis() + MESSAGE_MS;
        return true;
    }
}
