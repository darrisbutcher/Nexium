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
        super(menu, inventory, title, 176, 133, 51);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.storagenexus.upgrades"), leftPos + 30, topPos + 24, palette().text(), false);
        // Faded Lock Upgrade in the empty lock slot, so it is clear what goes there
        if (menu.getSlot(Upgradeable.LOCK_SLOT).getItem().isEmpty()) {
            int x = leftPos + UpgradeMenu.LOCK_SLOT_X, y = topPos + UpgradeMenu.SLOT_Y;
            g.item(new ItemStack(ModItems.LOCK_UPGRADE), x, y);
            g.nextStratum(); // items draw in their own pass; make sure the fade lands on top
            g.fill(x, y, x + 16, y + 16, 0xB0333333);
        }
    }
}
