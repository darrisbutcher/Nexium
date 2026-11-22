package net.nexium.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.nexium.energy.CellHolder;
import net.nexium.energy.CellMenu;
import net.nexium.energy.ItemEnergy;

import java.text.NumberFormat;
import java.util.Locale;

/** The screen of a Jetpack or Tablet: one slot for a battery cell, how charged it is, and what the item uses. */
public class CellScreen extends BaseScreen<CellMenu> {
    private static final int ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A, BAR_BACK = 0xFF1E1E22;

    public CellScreen(CellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, CellMenu.INVENTORY_TOP);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        ItemStack cell = menu.cellStack();
        long capacity = ItemEnergy.capacity(cell), stored = ItemEnergy.stored(cell);
        int bx = leftPos + 12, by = topPos + 52;
        g.fill(bx - 1, by - 1, bx + 153, by + 9, BAR_BACK);
        if (capacity > 0 && stored > 0) {
            int w = Math.max(1, (int) Math.round(152.0 * stored / capacity));
            g.fill(bx, by, bx + w, by + 8, ENERGY);
            g.fill(bx, by, bx + w, by + 2, ENERGY_TOP);
        }
        Component line = cell.isEmpty() ? Component.translatable("screen.nexium.cell.empty")
                : Component.translatable("screen.nexium.cell.charge", f.format(stored), f.format(capacity));
        g.text(font, line, leftPos + 8, topPos + 64, palette().text(), false);
        CellHolder holder = menu.holderItem();
        if (holder != null) g.text(font, Component.translatable(holder.useKey()), leftPos + 8, topPos + 74, 0xFFA0A0A0, false);
    }
}
