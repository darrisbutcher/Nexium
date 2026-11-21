package net.nexium.essentials.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.nexium.client.screen.BaseScreen;
import net.nexium.essentials.hostile.HostileMobSwitchMenu;

import java.util.List;

/**
 * The Hostile Mob Switch: whether hostile mobs are spawning now, a button to flip it, and what it does. The setting is the world's.
 */
public class HostileMobSwitchScreen extends BaseScreen<HostileMobSwitchMenu> {
    private static final int WIDTH = 176, HEIGHT = 128;
    private static final int GOOD = 0xFF6EE66E, BAD = 0xFFFF8A5A, MUTED = 0xFFAAAAAA;

    private Button toggle;

    public HostileMobSwitchScreen(HostileMobSwitchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, HostileMobSwitchMenu.BUTTON_TOGGLE);
        }).bounds(leftPos + 8, topPos + 36, WIDTH - 16, 20).build());
        updateLabel();
    }

    private void updateLabel() {
        toggle.setMessage(Component.translatable(menu.blocking() ? "screen.nexium.hostile_switch.allow" : "screen.nexium.hostile_switch.block"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabel();
    }

    /** The centre of the button, in screen coordinates (for tests, which click it with the real mouse). */
    public int[] buttonCentre() {
        return new int[]{leftPos + WIDTH / 2, topPos + 36 + 10};
    }

    /** Whether the screen shows hostile spawning as switched off (for tests). */
    public boolean showsBlocking() {
        return menu.blocking();
    }

    /** Only the title: there is no inventory here. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, palette().text(), false);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean blocking = menu.blocking();
        g.text(font, Component.translatable(blocking ? "screen.nexium.hostile_switch.blocked" : "screen.nexium.hostile_switch.spawning"),
                leftPos + 8, topPos + 22, blocking ? GOOD : BAD, false);
        List<FormattedCharSequence> help = font.split(Component.translatable("screen.nexium.hostile_switch.help"), WIDTH - 16);
        int y = topPos + 64;
        for (FormattedCharSequence line : help) {
            g.text(font, line, leftPos + 8, y, MUTED, false);
            y += 10;
        }
    }
}
