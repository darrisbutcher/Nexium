package net.nexium.essentials.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** The On/Off button of a machine's screen: right of the "Inventory" label, where it covers no title or other button. */
public final class PowerSwitchUi {
    public static final int WIDTH = 28;

    /** The button for a screen at {@code left}, {@code top} and {@code width} whose player inventory starts at {@code inventoryTop}. */
    public static Button create(int left, int top, int width, int containerId, int buttonId, int inventoryTop) {
        return Button.builder(Component.empty(), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(containerId, buttonId);
        }).bounds(left + width - OverclockUi.RIGHT - WIDTH, top + inventoryTop - 14, WIDTH, OverclockUi.HEIGHT).build();
    }

    /** "On" in green or "Off" in red, with a tooltip saying what the switch does. */
    public static void refresh(Button button, boolean off) {
        button.setMessage(Component.translatable(off ? "screen.nexium.switch.off" : "screen.nexium.switch.on")
                .withStyle(off ? ChatFormatting.RED : ChatFormatting.GREEN));
        button.setTooltip(Tooltip.create(Component.translatable(off ? "screen.nexium.switch.tip_off" : "screen.nexium.switch.tip_on")));
    }

    private PowerSwitchUi() {}
}
