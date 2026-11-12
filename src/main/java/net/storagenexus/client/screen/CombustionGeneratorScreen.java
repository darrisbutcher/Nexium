package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.energy.CombustionGeneratorBlockEntity;
import net.storagenexus.energy.CombustionGeneratorMenu;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** The Combustion Generator's screen, drawn with plain rectangles like the Freezer's: fuel, flame and an energy bar. */
public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0;
    private static final int FLAME = 0xFFF58C28, FLAME_HOT = 0xFFFFD65A, ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A;
    private static final int FLAME_X = 45, FLAME_Y = 28, FLAME_W = 14, FLAME_H = 14;
    private static final int BAR_X = 76, BAR_Y = 20, BAR_W = 88, BAR_H = 12;

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CombustionGeneratorMenu.WIDTH, CombustionGeneratorMenu.HEIGHT);
        inventoryLabelY = CombustionGeneratorMenu.INVENTORY_TOP - 11;
    }

    private static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        g.text(font, Component.translatable("screen.storagenexus.generator.stored", number(menu.energy()), number(CombustionGeneratorBlockEntity.CAPACITY)),
                BAR_X, BAR_Y + BAR_H + 4, TEXT, false);
        Component status = menu.burn() > 0
                ? Component.translatable("screen.storagenexus.generator.making", CombustionGeneratorBlockEntity.OUTPUT)
                : menu.energy() >= CombustionGeneratorBlockEntity.CAPACITY
                ? Component.translatable("screen.storagenexus.generator.full")
                : Component.translatable("screen.storagenexus.generator.no_fuel");
        g.text(font, status, BAR_X, BAR_Y + BAR_H + 16, MUTED, false);
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

        // The flame over the fuel slot burns down as the piece is used up
        int fx = leftPos + FLAME_X, fy = topPos + FLAME_Y;
        g.fill(fx - 1, fy - 1, fx + FLAME_W + 1, fy + FLAME_H + 1, SLOT_DARK);
        g.fill(fx, fy, fx + FLAME_W, fy + FLAME_H, SLOT_FILL);
        int lit = Math.round(FLAME_H * menu.burn());
        if (lit > 0) {
            g.fill(fx, fy + FLAME_H - lit, fx + FLAME_W, fy + FLAME_H, FLAME);
            g.fill(fx + 4, fy + FLAME_H - Math.max(1, lit / 2), fx + FLAME_W - 4, fy + FLAME_H, FLAME_HOT);
        }
        // The energy it holds
        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SLOT_FILL);
        int full = Math.round(BAR_W * Math.min(1.0F, (float) menu.energy() / CombustionGeneratorBlockEntity.CAPACITY));
        if (full > 0) {
            g.fill(bx, by, bx + full, by + BAR_H, ENERGY);
            g.fill(bx, by, bx + full, by + 2, ENERGY_TOP);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.storagenexus.generator.stored", number(menu.energy()), number(CombustionGeneratorBlockEntity.CAPACITY)),
                    Component.translatable("screen.storagenexus.generator.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
