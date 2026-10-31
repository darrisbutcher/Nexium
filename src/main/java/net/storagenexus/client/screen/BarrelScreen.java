package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.barrel.BarrelMenu;

import java.util.Locale;

/**
 * The barrel grid on the shared dark panel; wider barrels make a wider panel. The title row also
 * shows how many items the barrel holds out of how many it can hold, with a fill bar.
 */
public class BarrelScreen extends BaseScreen<BarrelMenu> {
    private static final int BAR_WIDTH = 60;

    public BarrelScreen(BarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BarrelMenu.imageWidth(menu.slotCount()), BarrelMenu.imageHeight(menu.slotCount()),
                BarrelMenu.inventoryTop(menu.slotCount()));
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        inventoryLabelX = (imageWidth - 162) / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        long used = 0, capacity = 0;
        for (int i = 0; i < menu.slotCount(); i++) {
            Slot slot = menu.getSlot(i);
            ItemStack stack = slot.getItem();
            // A filled slot holds as many as that item stacks to (16 for pearls); an empty one a full 64
            capacity += stack.isEmpty() ? 64 : Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
            used += stack.getCount();
        }
        Component text = Component.translatable("screen.storagenexus.barrel_usage", exact(used), exact(capacity));
        int right = imageWidth - 8;
        g.text(font, text, right - font.width(text), titleLabelY, palette().text(), false);

        // Fill bar on the inventory label row, right-aligned with the slots
        int barY = inventoryLabelY + 2, barLeft = right - BAR_WIDTH;
        g.fill(barLeft, barY, right, barY + 4, palette().slotDark());
        double fraction = capacity <= 0 ? 0 : Math.min(1.0, (double) used / capacity);
        int fill = (int) Math.round(BAR_WIDTH * fraction);
        int color = fraction < 0.75 ? 0xFF4CAF50 : fraction < 0.9 ? 0xFFE0C040 : 0xFFE05050;
        if (fill > 0) g.fill(barLeft, barY, barLeft + fill, barY + 4, color);
    }

    private static String exact(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
