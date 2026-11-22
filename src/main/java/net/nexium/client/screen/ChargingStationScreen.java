package net.nexium.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.nexium.energy.ChargingStationBlockEntity;
import net.nexium.energy.ChargingStationMenu;
import net.nexium.energy.ItemEnergy;

import java.text.NumberFormat;
import java.util.Locale;

/** The Charging Station's screen: four slots, each with a charge bar under it, and what the station itself holds. */
public class ChargingStationScreen extends BaseScreen<ChargingStationMenu> {
    private static final int ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A, BAR_BACK = 0xFF1E1E22;

    public ChargingStationScreen(ChargingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, ChargingStationMenu.INVENTORY_TOP);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int i = 0; i < ChargingStationBlockEntity.SLOTS; i++) {
            int x = leftPos + ChargingStationMenu.slotX(i), y = topPos + ChargingStationMenu.SLOT_Y + 20;
            g.fill(x, y, x + 16, y + 4, BAR_BACK);
            ItemStack stack = menu.getSlot(i).getItem();
            long capacity = ItemEnergy.capacity(stack);
            if (capacity <= 0) continue;
            int w = (int) Math.round(14.0 * ItemEnergy.stored(stack) / capacity);
            if (w > 0) {
                g.fill(x + 1, y + 1, x + 1 + w, y + 3, ENERGY);
                g.fill(x + 1, y + 1, x + 1 + w, y + 2, ENERGY_TOP);
            }
        }
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        g.text(font, Component.translatable("screen.nexium.station.stored", f.format(menu.stored()), f.format(ChargingStationBlockEntity.BUFFER)),
                leftPos + 8, topPos + 56, palette().text(), false);
        g.text(font, Component.translatable("screen.nexium.station.rate", f.format(ChargingStationBlockEntity.RATE)),
                leftPos + 8, topPos + 68, palette().text(), false);
    }
}
