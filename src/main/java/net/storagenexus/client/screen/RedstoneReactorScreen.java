package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.energy.RedstoneReactorBlockEntity;
import net.storagenexus.energy.RedstoneReactorMenu;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** The Redstone Reactor's screen, drawn with plain rectangles like the other generators'. */
public class RedstoneReactorScreen extends AbstractContainerScreen<RedstoneReactorMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0, WARN = 0xFFFF6A5A;
    private static final int ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A;
    private static final int HEAT_COOL = 0xFF48B44A, HEAT_WARM = 0xFFE0B83A, HEAT_HOT = 0xFFE0402A, MARK = 0xFFFFFFFF;
    private static final int HEAT_X = 96, HEAT_Y = 20, HEAT_W = 72, HEAT_H = 10;
    private static final int BAR_X = 8, BAR_Y = 86, BAR_W = 160, BAR_H = 6;

    public RedstoneReactorScreen(RedstoneReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, RedstoneReactorMenu.WIDTH, RedstoneReactorMenu.HEIGHT);
        inventoryLabelY = RedstoneReactorMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.reactor.retract"), b -> press(RedstoneReactorMenu.BTN_RETRACT))
                .bounds(leftPos + 96, topPos + 68, 35, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.reactor.insert"), b -> press(RedstoneReactorMenu.BTN_INSERT))
                .bounds(leftPos + 133, topPos + 68, 35, 14).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        int heat = menu.heatPermille();
        g.text(font, Component.translatable("screen.storagenexus.reactor.heat", heat / 10, heat % 10), HEAT_X, HEAT_Y + HEAT_H + 3, heat >= 600 ? WARN : TEXT, false);
        Component status = menu.shutDown() ? Component.translatable("screen.storagenexus.reactor.shut_down")
                : Component.translatable("screen.storagenexus.reactor.output", number(menu.output()));
        g.text(font, status, HEAT_X, HEAT_Y + HEAT_H + 14, menu.shutDown() ? WARN : MUTED, false);
        g.text(font, Component.translatable("screen.storagenexus.reactor.depth." + menu.depth()), HEAT_X, HEAT_Y + HEAT_H + 25, MUTED, false);
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

        // The heat bar: green to 60%, yellow to the 90% redline, red above, and a mark where the heat is
        int hx = leftPos + HEAT_X, hy = topPos + HEAT_Y;
        g.fill(hx - 1, hy - 1, hx + HEAT_W + 1, hy + HEAT_H + 1, SLOT_DARK);
        g.fill(hx, hy, hx + HEAT_W, hy + HEAT_H, SLOT_FILL);
        int heat = Math.min(1000, menu.heatPermille());
        int filled = Math.round(HEAT_W * heat / 1000.0F);
        for (int i = 0; i < filled; i++) {
            int colour = i < HEAT_W * 0.6F ? HEAT_COOL : i < HEAT_W * 0.9F ? HEAT_WARM : HEAT_HOT;
            g.fill(hx + i, hy, hx + i + 1, hy + HEAT_H, colour);
        }
        g.fill(hx + Math.round(HEAT_W * 0.9F), hy - 1, hx + Math.round(HEAT_W * 0.9F) + 1, hy + HEAT_H + 1, MARK);

        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SLOT_FILL);
        int full = Math.round(BAR_W * Math.min(1.0F, (float) menu.energy() / RedstoneReactorBlockEntity.CAPACITY));
        if (full > 0) {
            g.fill(bx, by, bx + full, by + BAR_H, ENERGY);
            g.fill(bx, by, bx + full, by + 1, ENERGY_TOP);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(HEAT_X, HEAT_Y, HEAT_W, HEAT_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.storagenexus.reactor.heat_tip1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("screen.storagenexus.reactor.heat_tip2").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.storagenexus.generator.stored", number(menu.energy()), number(RedstoneReactorBlockEntity.CAPACITY))), mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
