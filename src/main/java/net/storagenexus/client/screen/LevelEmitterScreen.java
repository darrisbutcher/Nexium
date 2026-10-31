package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.network.LevelEmitterMenu;

import java.util.Locale;

public class LevelEmitterScreen extends BaseScreen<LevelEmitterMenu> {
    private Button modeButton;

    public LevelEmitterScreen(LevelEmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 182, 100);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = 10_000;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> click(LevelEmitterMenu.BTN_MODE))
                .bounds(leftPos + 32, topPos + 20, 136, 16).build());
        int[] steps = LevelEmitterMenu.STEPS;
        for (int i = 0; i < steps.length; i++) {
            int step = steps[steps.length - 1 - i];
            int j = steps.length - 1 - i;
            addRenderableWidget(Button.builder(Component.literal("-" + step), b -> click(LevelEmitterMenu.BTN_SUB + j))
                    .bounds(leftPos + 8 + i * 40, topPos + 42, 38, 14).build());
        }
        for (int i = 0; i < steps.length; i++) {
            int j = i;
            addRenderableWidget(Button.builder(Component.literal("+" + steps[i]), b -> click(LevelEmitterMenu.BTN_ADD + j))
                    .bounds(leftPos + 8 + i * 40, topPos + 70, 38, 14).build());
        }
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component modeLabel() {
        return Component.translatable(menu.below() ? "screen.storagenexus.emitter_below" : "screen.storagenexus.emitter_atleast");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        modeButton.setMessage(modeLabel());
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Component amount = Component.literal(String.format(Locale.ROOT, "%,d", menu.amount()));
        g.text(font, amount, leftPos + 88 - font.width(amount) / 2, topPos + 60, palette().text(), false);
        Component line = menu.item().isEmpty()
                ? Component.translatable("screen.storagenexus.emitter_pick")
                : Component.translatable("screen.storagenexus.emitter_now", String.format(Locale.ROOT, "%,d", menu.now()),
                        Component.translatable(menu.powered() ? "screen.storagenexus.emitter_on" : "screen.storagenexus.emitter_off"));
        g.text(font, line, leftPos + 8, topPos + 88, palette().text(), false);
    }
}
