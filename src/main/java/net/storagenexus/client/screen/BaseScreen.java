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
    protected BaseScreen(M menu, Inventory inventory, Component title, int width, int height, int inventoryTop) {
        super(menu, inventory, title, width, height);
        this.inventoryLabelY = inventoryTop - 11;
    }

    /** The colours this screen is drawn in: all mod screens match vanilla's dark inventory. */
    protected Palette palette() {
        return Palette.DARK;
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
