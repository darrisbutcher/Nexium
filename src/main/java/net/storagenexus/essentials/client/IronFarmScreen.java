package net.storagenexus.essentials.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmItem;
import net.storagenexus.essentials.ironfarm.IronFarmMenu;

import java.util.List;
import java.util.Locale;

/**
 * The Iron Farm screen, drawn with plain rectangles like vanilla's dark inventory: the bread slot, a bar
 * towards the next golem, what the bread is doing, and the output.
 */
public class IronFarmScreen extends AbstractContainerScreen<IronFarmMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0;
    private static final int IRON = 0xFFD8D8D8, BOOSTED = 0xFFE8B23A, COOLDOWN = 0xFF6FC7E8, FULL = 0xFFE05050;
    private static final int BAR_X = 30, BAR_Y = 21, BAR_W = 138, BAR_H = 6;

    public IronFarmScreen(IronFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IronFarmMenu.WIDTH, IronFarmMenu.HEIGHT);
        inventoryLabelY = IronFarmMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component status;
        int colour;
        if (menu.paused()) {
            status = Component.translatable("screen.nexusessentials.iron_farm_paused");
            colour = FULL;
        } else if (menu.full()) {
            status = Component.translatable("screen.nexusessentials.iron_farm_full");
            colour = FULL;
        } else if (menu.unpowered()) {
            status = Component.translatable("screen.nexusessentials.no_power", net.storagenexus.essentials.ironfarm.IronFarmBlockEntity.ENERGY_PER_TICK);
            colour = FULL;
        } else if (menu.boosting()) {
            status = Component.translatable("screen.nexusessentials.iron_farm_boosting", menu.speedUp(), seconds(menu.boostSeconds()));
            colour = BOOSTED;
        } else if (menu.coolingDown()) {
            status = Component.translatable("screen.nexusessentials.iron_farm_cooling", seconds(menu.cooldownSeconds()));
            colour = COOLDOWN;
        } else {
            status = Component.translatable("screen.nexusessentials.iron_farm_normal", menu.speedUp());
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

        // A faded loaf shows what the bread slot is for; during the cooldown it is tinted blue instead
        int bx = leftPos + IronFarmMenu.BREAD_X, by = topPos + IronFarmMenu.BREAD_Y;
        if (menu.slots.get(IronFarmBlockEntity.BREAD_SLOT).getItem().isEmpty()) {
            g.fakeItem(new ItemStack(Items.BREAD), bx, by);
            g.fill(bx, by, bx + 16, by + 16, menu.coolingDown() ? 0xC0284A5A : 0xB0333333);
        }

        // Progress towards the next golem
        int barX = leftPos + BAR_X, barY = topPos + BAR_Y;
        g.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, SLOT_DARK);
        g.fill(barX, barY, barX + BAR_W, barY + BAR_H, SLOT_FILL);
        int filled = Math.round(menu.progress() * BAR_W);
        if (filled > 0) g.fill(barX, barY, barX + filled, barY + BAR_H, menu.full() || menu.paused() || menu.unpowered() ? FULL : menu.boosting() ? BOOSTED : IRON);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(BAR_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.nexusessentials.iron_farm_next", Math.round(menu.progress() * 100)),
                    Component.translatable("screen.nexusessentials.iron_farm_rate", ironPerHour(1)).withStyle(ChatFormatting.GRAY),
                    Component.translatable("screen.nexusessentials.iron_farm_rate_boosted", menu.speedUp(), ironPerHour(menu.speedUp())).withStyle(ChatFormatting.GOLD)
            ), mouseX, mouseY);
        } else if (menu.coolingDown() && hoveredSlot != null && hoveredSlot.index == IronFarmBlockEntity.BREAD_SLOT
                && !hoveredSlot.hasItem()) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.nexusessentials.iron_farm_slot_locked", seconds(menu.cooldownSeconds())).withStyle(ChatFormatting.AQUA)
            ), mouseX, mouseY);
        }
    }

    /** Uses the server's numbers (from the menu), which can differ from this client's config. */
    private String ironPerHour(int speedUp) {
        return String.format(Locale.ROOT, "%,d", 72000L * speedUp / menu.golemTicks() * 4);
    }

    private static String seconds(int seconds) {
        return IronFarmItem.time(seconds * 20L);
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
