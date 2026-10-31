package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.upgrade.UpgradeMenu;

public class UpgradeScreen extends BaseScreen<UpgradeMenu> {
    public UpgradeScreen(UpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133, 51);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.storagenexus.upgrades"), leftPos + 8, topPos + 24, palette().text(), false);
    }
}
