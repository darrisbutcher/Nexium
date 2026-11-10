package net.storagenexus.essentials.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.farm.FarmMenu;
import net.storagenexus.essentials.ironfarm.IronFarmItem;

import java.util.List;

/**
 * The screen of every {@link FarmKind} farm, drawn with plain rectangles like the Iron Farm's: the input slot (when the
 * kind has one), a bar towards the next cycle, what it is doing, and the output.
 */
public class FarmScreen extends AbstractContainerScreen<FarmMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0;
    private static final int WORKING = 0xFFE8B23A, BLOCKED = 0xFFE05050;
    private static final int BAR_X = 30, BAR_Y = 21, BAR_W = 138, BAR_H = 6;

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FarmMenu.WIDTH, FarmMenu.HEIGHT);
        inventoryLabelY = FarmMenu.INVENTORY_TOP - 11;
    }

    private Button modeButton;

    @Override
    protected void init() {
        super.init();
        if (!menu.kind().modes().isEmpty()) {
            // Right of the status line: click to step through what the farm makes
            modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }).bounds(leftPos + 96, topPos + BAR_Y + BAR_H + 2, 72, 13).build());
            refreshModeButton();
        }
    }

    /** The mode's name on the button. */
    private void refreshModeButton() {
        var modes = menu.kind().modes();
        modeButton.setMessage(Component.translatable("screen.nexusessentials.farm.mode." + menu.kind().id() + "." + modes.get(Math.floorMod(menu.mode(), modes.size()))));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) refreshModeButton();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component status;
        int colour = BLOCKED;
        if (menu.paused()) {
            status = Component.translatable("screen.nexusessentials.farm_paused");
        } else if (menu.full()) {
            status = Component.translatable("screen.nexusessentials.farm_full");
        } else if (menu.starved()) {
            status = Component.translatable("screen.nexusessentials." + menu.kind().lang("needs"));
        } else {
            int left = Math.round((1.0F - menu.progress()) * menu.cycleTicks());
            status = Component.translatable("screen.nexusessentials.farm_next", IronFarmItem.time(left));
            colour = MUTED;
        }
        g.text(font, status, kind0().hasInput() ? BAR_X : FarmMenu.INPUT_X, BAR_Y + BAR_H + 4, colour, false);
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
        for (Slot s : menu.slots) {
            if (s.isActive()) slot(g, leftPos + s.x, topPos + s.y);
        }

        // A faded item shows what the input slot is for
        FarmKind kind = menu.kind();
        if (kind.hasInput() && menu.slots.get(FarmBlockEntity.INPUT_SLOT).getItem().isEmpty()) {
            int ix = leftPos + FarmMenu.INPUT_X, iy = topPos + FarmMenu.INPUT_Y;
            g.fakeItem(new ItemStack(kind.inputIcon()), ix, iy);
            g.fill(ix, iy, ix + 16, iy + 16, 0xB0333333);
        }

        // Progress towards the next cycle
        int barX = leftPos + (kind.hasInput() ? BAR_X : FarmMenu.INPUT_X), barY = topPos + BAR_Y;
        int barW = kind.hasInput() ? BAR_W : BAR_W + BAR_X - FarmMenu.INPUT_X;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + BAR_H + 1, SLOT_DARK);
        g.fill(barX, barY, barX + barW, barY + BAR_H, SLOT_FILL);
        int filled = Math.round(menu.progress() * barW);
        if (filled > 0) g.fill(barX, barY, barX + filled, barY + BAR_H, menu.full() || menu.paused() || menu.starved() ? BLOCKED : WORKING);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        FarmKind kind = menu.kind();
        int barX = kind.hasInput() ? BAR_X : FarmMenu.INPUT_X;
        int barW = kind.hasInput() ? BAR_W : BAR_W + BAR_X - FarmMenu.INPUT_X;
        if (isHovering(barX - 1, BAR_Y - 1, barW + 2, BAR_H + 2, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.nexusessentials.farm_progress", Math.round(menu.progress() * 100)),
                    Component.translatable("tooltip.nexusessentials." + kind.lang("makes")).withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.nexusessentials.farm_every", IronFarmItem.time(menu.cycleTicks())).withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        } else if (kind.hasInput() && hoveredSlot != null && hoveredSlot.index == FarmBlockEntity.INPUT_SLOT && !hoveredSlot.hasItem()) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("tooltip.nexusessentials." + kind.lang("input")).withStyle(ChatFormatting.GOLD)
            ), mouseX, mouseY);
        }
    }

    private FarmKind kind0() {
        return menu.kind();
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
