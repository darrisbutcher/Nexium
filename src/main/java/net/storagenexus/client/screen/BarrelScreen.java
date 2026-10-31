package net.storagenexus.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.barrel.BarrelMenu;

/** The barrel grid on the shared dark panel. */
public class BarrelScreen extends BaseScreen<BarrelMenu> {
    public BarrelScreen(BarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, BarrelMenu.imageHeight(menu.rows()), BarrelMenu.inventoryTop(menu.rows()));
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
    }
}
