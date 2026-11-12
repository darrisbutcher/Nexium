package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.energy.BatteryMenu;

import java.text.NumberFormat;
import java.util.Locale;

/** The Battery's screen: one wide energy bar and the numbers, drawn with plain rectangles. */
public class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0;
    private static final int ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A;
    private static final int BAR_X = 12, BAR_Y = 24, BAR_W = 152, BAR_H = 20;

    public BatteryScreen(BatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BatteryMenu.WIDTH, BatteryMenu.HEIGHT);
        inventoryLabelY = BatteryMenu.INVENTORY_TOP - 11;
    }

    private static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        g.text(font, Component.translatable("screen.storagenexus.generator.stored", number(menu.stored()), number(menu.capacity())),
                BAR_X, BAR_Y + BAR_H + 6, TEXT, false);
        int percent = menu.capacity() <= 0 ? 0 : (int) (100L * menu.stored() / menu.capacity());
        g.text(font, Component.translatable("screen.storagenexus.battery.percent", percent), BAR_X, BAR_Y + BAR_H + 18, MUTED, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BODY);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, HIGHLIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, HIGHLIGHT);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, SHADOW);
        for (Slot s : menu.slots) slot(g, leftPos + s.x, topPos + s.y);
        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SLOT_FILL);
        int full = menu.capacity() <= 0 ? 0 : Math.round(BAR_W * Math.min(1.0F, (float) menu.stored() / menu.capacity()));
        if (full > 0) {
            g.fill(bx, by, bx + full, by + BAR_H, ENERGY);
            g.fill(bx, by, bx + full, by + 3, ENERGY_TOP);
        }
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
