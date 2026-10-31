package net.storagenexus.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.filter.FilterMenu;

public class FilterScreen extends BaseScreen<FilterMenu> {
    private Button whitelist;
    private Button components;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, 84);
    }

    @Override
    protected void init() {
        super.init();
        whitelist = addRenderableWidget(Button.builder(Component.empty(), b -> click(FilterMenu.BTN_WHITELIST))
                .bounds(leftPos + 8, topPos + 44, 78, 16).build());
        components = addRenderableWidget(Button.builder(Component.empty(), b -> click(FilterMenu.BTN_COMPONENTS))
                .bounds(leftPos + 90, topPos + 44, 78, 16).build());
        updateLabels();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void updateLabels() {
        whitelist.setMessage(Component.translatable(menu.whitelist() ? "screen.storagenexus.whitelist" : "screen.storagenexus.blacklist"));
        components.setMessage(Component.translatable(menu.matchComponents() ? "screen.storagenexus.match_exact" : "screen.storagenexus.match_item"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }
}
