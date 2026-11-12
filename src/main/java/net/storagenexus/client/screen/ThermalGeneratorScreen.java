package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.energy.ThermalGeneratorBlockEntity;
import net.storagenexus.energy.ThermalGeneratorMenu;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** The Thermal Generator's screen: a lava tank and an energy bar, drawn with plain rectangles like the Freezer's. */
public class ThermalGeneratorScreen extends AbstractContainerScreen<ThermalGeneratorMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0;
    private static final int LAVA = 0xFFE2641A, LAVA_SURFACE = 0xFFFFC04A, ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A;
    private static final int TANK_X = 12, TANK_Y = 20, TANK_W = 22, TANK_H = 54;
    private static final int BAR_X = 48, BAR_Y = 20, BAR_W = 116, BAR_H = 12;

    public ThermalGeneratorScreen(ThermalGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ThermalGeneratorMenu.WIDTH, ThermalGeneratorMenu.HEIGHT);
        inventoryLabelY = ThermalGeneratorMenu.INVENTORY_TOP - 11;
    }

    private static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        g.text(font, Component.translatable("screen.storagenexus.generator.stored", number(menu.energy()), number(ThermalGeneratorBlockEntity.CAPACITY)),
                BAR_X, BAR_Y + BAR_H + 4, TEXT, false);
        Component status = menu.burning()
                ? Component.translatable("screen.storagenexus.generator.making", ThermalGeneratorBlockEntity.OUTPUT)
                : menu.energy() >= ThermalGeneratorBlockEntity.CAPACITY
                ? Component.translatable("screen.storagenexus.generator.full")
                : Component.translatable("screen.storagenexus.thermal.no_lava");
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

        int tx = leftPos + TANK_X, ty = topPos + TANK_Y;
        g.fill(tx - 1, ty - 1, tx + TANK_W + 1, ty + TANK_H + 1, SLOT_DARK);
        g.fill(tx, ty, tx + TANK_W, ty + TANK_H, SLOT_FILL);
        int filled = Math.round(TANK_H * Math.min(1.0F, menu.lavaMilli() / 4000.0F));
        if (filled > 0) {
            g.fill(tx, ty + TANK_H - filled, tx + TANK_W, ty + TANK_H, LAVA);
            g.fill(tx, ty + TANK_H - filled, tx + TANK_W, ty + TANK_H - filled + 1, LAVA_SURFACE);
        }
        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SLOT_FILL);
        int full = Math.round(BAR_W * Math.min(1.0F, (float) menu.energy() / ThermalGeneratorBlockEntity.CAPACITY));
        if (full > 0) {
            g.fill(bx, by, bx + full, by + BAR_H, ENERGY);
            g.fill(bx, by, bx + full, by + 2, ENERGY_TOP);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.storagenexus.thermal.lava", menu.lavaMilli()),
                    Component.translatable("screen.storagenexus.thermal.tank").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.storagenexus.generator.stored", number(menu.energy()), number(ThermalGeneratorBlockEntity.CAPACITY)),
                    Component.translatable("screen.storagenexus.generator.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
