package net.nexium.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nexium.autocraft.PatternEncoderMenu;

/** The Pattern Encoder: a ghost grid (or input list), the result, a blank pattern slot, and Encode / Mode / Clear buttons. */
public class PatternEncoderScreen extends BaseScreen<PatternEncoderMenu> {
    private Button mode;

    public PatternEncoderScreen(PatternEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, PatternEncoderMenu.INVENTORY_TOP + 82, PatternEncoderMenu.INVENTORY_TOP);
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeText(), b -> press(PatternEncoderMenu.BTN_MODE))
                .bounds(leftPos + 8, topPos + 82, 60, 16).tooltip(Tooltip.create(Component.translatable("screen.nexium.encoder.mode.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.encoder.clear"), b -> press(PatternEncoderMenu.BTN_CLEAR))
                .bounds(leftPos + 72, topPos + 82, 36, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.encoder.encode"), b -> press(PatternEncoderMenu.BTN_ENCODE))
                .bounds(leftPos + 116, topPos + 98, 52, 16).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component modeText() {
        return Component.translatable(menu.processing() ? "screen.nexium.encoder.processing" : "screen.nexium.encoder.crafting");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mode != null) mode.setMessage(modeText());
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.nexium.encoder.arrow"), leftPos + 92, topPos + 24, palette().text(), false);
    }
}
