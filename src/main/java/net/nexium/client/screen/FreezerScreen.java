package net.nexium.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.nexium.essentials.ironfarm.IronFarmItem;
import net.nexium.freezer.FreezerBlockEntity;
import net.nexium.freezer.FreezerMenu;
import net.nexium.freezer.FreezerMode;

import java.util.List;

/**
 * The Freezer screen, drawn with plain rectangles like the Iron Farm's: the water tank, a button that switches
 * what it makes, a progress bar and the output.
 */
public class FreezerScreen extends AbstractContainerScreen<FreezerMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0, BLOCKED = 0xFFE05050;
    private static final int WATER = 0xFF3A82CF, WATER_SURFACE = 0xFF8FD8F5, ICE = 0xFFCFEEFB;
    private static final int TANK_X = 12, TANK_Y = 20, TANK_W = 22, TANK_H = 54;
    private static final int BTN_X = 44, BTN_Y = 20, BTN_W = 62, BTN_H = 18;
    private static final int BAR_X = 44, BAR_Y = 46, BAR_W = 62, BAR_H = 6;

    public FreezerScreen(FreezerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FreezerMenu.WIDTH, FreezerMenu.HEIGHT);
        inventoryLabelY = FreezerMenu.INVENTORY_TOP - 11;
    }

    private Button modeButton, overclockButton;

    @Override
    protected void init() {
        super.init();
        overclockButton = addRenderableWidget(net.nexium.essentials.client.OverclockUi.create(leftPos, topPos, imageWidth, menu.containerId,
                FreezerMenu.BTN_OVERCLOCK, FreezerMenu.BTN_OVERCLOCK_BACK));
        net.nexium.essentials.client.OverclockUi.refresh(overclockButton, menu.overclock(), FreezerBlockEntity.ENERGY_PER_TICK);
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FreezerMenu.BTN_MODE);
                }).bounds(leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H).build());
    }

    /** The mode button's label, for the client test too. */
    public static Component modeLabel(FreezerMode mode) {
        return Component.translatable("screen.nexium.freezer.mode." + mode.key());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel(menu.mode()));
        net.nexium.essentials.client.OverclockUi.refresh(overclockButton, menu.overclock(), FreezerBlockEntity.ENERGY_PER_TICK);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component status;
        int colour = BLOCKED;
        if (menu.paused()) {
            status = Component.translatable("screen.nexium.freezer.paused");
        } else if (menu.full()) {
            status = Component.translatable("screen.nexium.freezer.full");
        } else if (menu.unpowered()) {
            status = Component.translatable("screen.nexium.no_power", net.nexium.freezer.FreezerBlockEntity.ENERGY_PER_TICK);
        } else if (menu.dry()) {
            status = Component.translatable("screen.nexium.freezer.dry");
        } else {
            // Overclocked, the same work takes that many times fewer ticks
            int left = Math.round((1.0F - menu.progress()) * menu.mode().ticks() / net.nexium.essentials.farm.Overclock.multiplier(menu.overclock()));
            status = Component.translatable("screen.nexium.freezer.next", IronFarmItem.time(left));
            colour = MUTED;
        }
        g.text(font, status, BAR_X, BAR_Y + BAR_H + 4, colour, false);
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

        // The tank: water from the bottom up
        int tx = leftPos + TANK_X, ty = topPos + TANK_Y;
        g.fill(tx - 1, ty - 1, tx + TANK_W + 1, ty + TANK_H + 1, SLOT_DARK);
        g.fill(tx, ty, tx + TANK_W, ty + TANK_H, SLOT_FILL);
        int filled = Math.round(TANK_H * Math.min(1.0F, menu.waterMilli() / 8000.0F));
        if (filled > 0) {
            g.fill(tx, ty + TANK_H - filled, tx + TANK_W, ty + TANK_H, WATER);
            g.fill(tx, ty + TANK_H - filled, tx + TANK_W, ty + TANK_H - filled + 1, WATER_SURFACE);
        }
        // Progress towards the next item
        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SLOT_FILL);
        int done = Math.round(menu.progress() * BAR_W);
        if (done > 0) g.fill(bx, by, bx + done, by + BAR_H, menu.full() || menu.paused() || menu.dry() || menu.unpowered() ? BLOCKED : ICE);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            net.nexium.client.Tooltips.show(g, font, List.of(
                    Component.translatable("screen.nexium.freezer.water", menu.waterMilli()),
                    Component.translatable("screen.nexium.freezer.tank").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            net.nexium.client.Tooltips.show(g, font, List.of(
                    Component.translatable("screen.nexium.freezer.mode.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (isHovering(BAR_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, mouseX, mouseY)) {
            net.nexium.client.Tooltips.show(g, font, List.of(
                    Component.translatable("screen.nexium.freezer.progress", Math.round(menu.progress() * 100))), mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    /** The middle of the Overclock button in GUI pixels (for the client test). */
    public int[] overclockCentre() {
        return new int[]{overclockButton.getX() + overclockButton.getWidth() / 2, overclockButton.getY() + overclockButton.getHeight() / 2};
    }
}
