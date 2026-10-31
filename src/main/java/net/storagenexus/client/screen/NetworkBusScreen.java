package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.network.NetworkBusBlock;
import net.storagenexus.network.NetworkBusMenu;

public class NetworkBusScreen extends BaseScreen<NetworkBusMenu> {
    private Button modeButton;

    public NetworkBusScreen(NetworkBusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133, 51);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = 10_000;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, NetworkBusMenu.BTN_MODE);
        }).bounds(leftPos + 8, topPos + 18, 90, 16).build());
    }

    private BlockState state() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getBlockState(menu.pos()) : null;
    }

    private Component modeLabel() {
        BlockState s = state();
        boolean export = s != null && s.getBlock() instanceof NetworkBusBlock && s.getValue(NetworkBusBlock.EXPORT);
        return Component.translatable(export ? "screen.storagenexus.bus_export" : "screen.storagenexus.bus_import");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        modeButton.setMessage(modeLabel());
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        BlockState s = state();
        if (s != null && s.getBlock() instanceof NetworkBusBlock) {
            Component name = NeighbourNames.describe(menu.pos(), s.getValue(NetworkBusBlock.FACING));
            g.text(font, Component.translatable("screen.storagenexus.bus_target", name), leftPos + 8, topPos + 38, palette().text(), false);
        }
        g.text(font, Component.translatable("screen.storagenexus.filter"), leftPos + 118, topPos + 24, palette().text(), false);
    }
}
