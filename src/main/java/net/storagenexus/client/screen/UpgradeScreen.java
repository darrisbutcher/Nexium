package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModItems;
import net.storagenexus.upgrade.UpgradeMenu;
import net.storagenexus.upgrade.Upgradeable;

import java.util.Locale;

public class UpgradeScreen extends BaseScreen<UpgradeMenu> {
    public UpgradeScreen(UpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, UpgradeMenu.INVENTORY_Y + 82, UpgradeMenu.INVENTORY_Y);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.storagenexus.upgrades"), leftPos + 30, topPos + 24, palette().text(), false);
        // Capacity with the upgrades that are in now: each section first, then the whole drawer
        int sections = menu.capacitySlots();
        String unit = menu.holdsFluid() ? "buckets" : "stacks";
        line(g, Component.translatable("screen.storagenexus.upgrades.multiplier", menu.multiplier()), 0);
        if (sections > 1) {
            line(g, Component.translatable("screen.storagenexus.upgrades.per_section." + unit,
                    amount((double) menu.totalCapacity() / sections)), 1);
            line(g, Component.translatable("screen.storagenexus.upgrades.in_total." + unit, amount(menu.totalCapacity())), 2);
        } else {
            line(g, Component.translatable("screen.storagenexus.upgrades.total." + unit, amount(menu.totalCapacity())), 1);
        }
        // Faded Lock Upgrade in the empty lock slot, so it is clear what goes there
        if (menu.getSlot(Upgradeable.LOCK_SLOT).getItem().isEmpty()) {
            int x = leftPos + UpgradeMenu.LOCK_SLOT_X, y = topPos + UpgradeMenu.SLOT_Y;
            g.item(new ItemStack(ModItems.LOCK_UPGRADE), x, y);
            g.nextStratum(); // items draw in their own pass; make sure the fade lands on top
            g.fill(x, y, x + 16, y + 16, 0xB0333333);
        }
    }

    /** One text line under the slots, shrunk if needed so it never runs past the panel. */
    private void line(GuiGraphicsExtractor g, Component text, int row) {
        int x = leftPos + 8, y = topPos + UpgradeMenu.TEXT_Y + row * 10;
        int room = imageWidth - 16, width = font.width(text);
        if (width <= room) {
            g.text(font, text, x, y, palette().text(), false);
            return;
        }
        float scale = (float) room / width;
        g.pose().pushMatrix();
        g.pose().translate(x, y + (1 - scale) * 4);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, palette().text(), false);
        g.pose().popMatrix();
    }

    /** 13.5 for a 2x2 drawer's share of 54 stacks; whole numbers without the decimal. */
    private static String amount(double n) {
        return n == Math.floor(n) ? String.format(Locale.ROOT, "%,d", (long) n) : String.format(Locale.ROOT, "%,.1f", n);
    }
}
