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
        Palette.LIGHT.panel(g, x, y, w, h);
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        Palette.LIGHT.slot(g, x, y);
    }

    /** The colours this screen is drawn in. */
    protected Palette palette() {
        return Palette.LIGHT;
    }

    /** Title and inventory labels in the palette's text colour. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, palette().text(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, palette().text(), false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        palette().panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) palette().slot(g, leftPos + s.x, topPos + s.y);
        renderExtraBg(g, mouseX, mouseY);
    }

    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {}
}
