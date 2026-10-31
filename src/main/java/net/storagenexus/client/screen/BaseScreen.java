package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Draws the classic grey panel and slot frames with plain rectangles, so no GUI textures
 * are needed.
 */
public abstract class BaseScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int LIGHT = 0xFFFFFFFF;
    public static final int DARK = 0xFF555555;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int TEXT = 0xFF404040;

    protected BaseScreen(M menu, Inventory inventory, Component title, int width, int height, int inventoryTop) {
        super(menu, inventory, title, width, height);
        this.inventoryLabelY = inventoryTop - 11;
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, LIGHT);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, DARK);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, DARK);
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, DARK);
        g.fill(x, y, x + 17, y + 17, LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) slot(g, leftPos + s.x, topPos + s.y);
        renderExtraBg(g, mouseX, mouseY);
    }

    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {}
}
