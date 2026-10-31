package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModItems;
import net.storagenexus.upgrade.UpgradeMenu;
import net.storagenexus.upgrade.Upgradeable;

public class UpgradeScreen extends BaseScreen<UpgradeMenu> {
    public UpgradeScreen(UpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, UpgradeMenu.INVENTORY_Y + 82, UpgradeMenu.INVENTORY_Y);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.storagenexus.upgrades"), leftPos + 30, topPos + 24, palette().text(), false);
        // Total capacity with the upgrades that are in now
        String total = String.format("%,d", menu.totalCapacity());
        String each = String.format("%,d", menu.totalCapacity() / menu.capacitySlots());
        String unit = menu.holdsFluid() ? "buckets" : "stacks";
        g.text(font, Component.translatable("screen.storagenexus.upgrades.multiplier", menu.multiplier()),
                leftPos + 8, topPos + 42, palette().text(), false);
        Component amount = menu.capacitySlots() > 1
                ? Component.translatable("screen.storagenexus.upgrades.total_split." + unit, total, each)
                : Component.translatable("screen.storagenexus.upgrades.total." + unit, total);
        g.text(font, amount, leftPos + 8, topPos + 53, palette().text(), false);
        // Faded Lock Upgrade in the empty lock slot, so it is clear what goes there
        if (menu.getSlot(Upgradeable.LOCK_SLOT).getItem().isEmpty()) {
            int x = leftPos + UpgradeMenu.LOCK_SLOT_X, y = topPos + UpgradeMenu.SLOT_Y;
            g.item(new ItemStack(ModItems.LOCK_UPGRADE), x, y);
            g.nextStratum(); // items draw in their own pass; make sure the fade lands on top
            g.fill(x, y, x + 16, y + 16, 0xB0333333);
        }
    }
}
