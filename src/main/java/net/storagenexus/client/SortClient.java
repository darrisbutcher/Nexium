package net.storagenexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.storagenexus.sort.InventorySorter;
import net.storagenexus.sort.InventorySorter.Region;
import net.storagenexus.sort.SortOrder;
import net.storagenexus.sort.SortPayload;

/**
 * The sort key (R by default): over a container's slots it sorts the container, over the main inventory it sorts
 * that, and never both and never the hotbar. Pressing it again over the same area steps through alphabetical,
 * creative-menu and by-amount order. With a recipe viewer installed, R over an item belongs to the viewer.
 */
public final class SortClient {
    public static final KeyMapping SORT = new KeyMapping("key.storagenexus.sort",
            InputConstants.KEY_R, NexusEssentialsClient.CATEGORY);

    private static AbstractContainerScreen<?> lastScreen;
    private static Region lastRegion;
    private static SortOrder lastOrder;

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

    private static boolean recipeViewerLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("jei") || loader.isModLoaded("roughlyenoughitems") || loader.isModLoaded("emi");
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
        Slot hovered = screen.hoveredSlot;
        if (hovered != null && hovered.hasItem() && recipeViewerLoaded()) return false;
        Region region = InventorySorter.regionAt(screen.getMenu(), x - screen.leftPos, y - screen.topPos);
        if (region == null) return false;
        SortOrder order = screen == lastScreen && region == lastRegion && lastOrder != null
                ? lastOrder.next() : SortOrder.ALPHABETICAL;
        lastScreen = screen;
        lastRegion = region;
        lastOrder = order;
        ClientPlayNetworking.send(new SortPayload(screen.getMenu().containerId, region == Region.PLAYER, order.ordinal()));
        message = Component.translatable("message.storagenexus.sort", Component.translatable("message.storagenexus.sort." + order.key));
        messageUntil = System.currentTimeMillis() + MESSAGE_MS;
        return true;
    }
}
