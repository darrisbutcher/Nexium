package net.storagenexus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a shulker box's contents as a 9 by 3 grid under its name, in slots tinted the colour of the box (purple for an undyed
 * one), each with its item and count.
 */
public final class ClientShulkerPreview implements ClientTooltipComponent {
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final int SLOT_SIZE = 18;
    /** An undyed shulker box is this purple. */
    private static final int UNDYED = 0x956795;

    private final NonNullList<ItemStack> items = NonNullList.withSize(ShulkerPreview.COLUMNS * ShulkerPreview.ROWS, ItemStack.EMPTY);
    private final int tint;

    public ClientShulkerPreview(ShulkerPreview preview) {
        preview.contents().copyInto(items);
        this.tint = tint(preview.color());
    }

    /** The box's colour, pulled halfway to white so the slots stay light enough to read items on. */
    private static int tint(DyeColor color) {
        int rgb = color == null ? UNDYED : color.getTextureDiffuseColor();
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r += (255 - r) / 2;
        g += (255 - g) / 2;
        b += (255 - b) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public int getHeight(Font font) {
        return ShulkerPreview.ROWS * SLOT_SIZE + 4;
    }

    @Override
    public int getWidth(Font font) {
        return ShulkerPreview.COLUMNS * SLOT_SIZE;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        for (int i = 0; i < items.size(); i++) {
            int sx = x + (i % ShulkerPreview.COLUMNS) * SLOT_SIZE;
            int sy = y + 2 + (i / ShulkerPreview.COLUMNS) * SLOT_SIZE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, sx, sy, SLOT_SIZE, SLOT_SIZE, tint);
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            graphics.item(stack, sx + 1, sy + 1);
            graphics.itemDecorations(font, stack, sx + 1, sy + 1);
        }
    }
}
