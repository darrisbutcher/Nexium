package net.storagenexus.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.barrel.BarrelMenu;

/** The barrel grid on the shared dark panel; wider barrels make a wider panel. */
public class BarrelScreen extends BaseScreen<BarrelMenu> {
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
}
