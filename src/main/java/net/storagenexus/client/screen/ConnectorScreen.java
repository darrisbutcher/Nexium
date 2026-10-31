package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.network.ConnectorMenu;

public class ConnectorScreen extends BaseScreen<ConnectorMenu> {
    public ConnectorScreen(ConnectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133, 51);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = 10_000; // no room for the "Inventory" label under the buttons
        int y = topPos + 30;
        addRenderableWidget(Button.builder(Component.literal("-10"), b -> click(ConnectorMenu.BTN_MINUS_10)).bounds(leftPos + 8, y, 24, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> click(ConnectorMenu.BTN_MINUS_1)).bounds(leftPos + 34, y, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> click(ConnectorMenu.BTN_PLUS_1)).bounds(leftPos + 90, y, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> click(ConnectorMenu.BTN_PLUS_10)).bounds(leftPos + 112, y, 24, 14).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.storagenexus.priority"), leftPos + 8, topPos + 19, TEXT, false);
        String p = Integer.toString(menu.priority());
        g.text(font, p, leftPos + 72 - font.width(p) / 2, topPos + 34, TEXT, false);
        g.text(font, Component.translatable("screen.storagenexus.filter"), leftPos + 118, topPos + 24, TEXT, false);
    }
}
