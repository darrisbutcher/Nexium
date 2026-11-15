package net.storagenexus.essentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.storagenexus.essentials.farm.Overclock;

/** The Overclock button every powered farm screen has, top right: click for faster, Shift-click for slower. */
public final class OverclockUi {
    public static final int WIDTH = 46, HEIGHT = 13, RIGHT = 8, TOP = 3;

    /** The button for a screen at {@code left}, {@code top} and {@code width}; {@code forward} and {@code back} are menu button ids. */
    public static Button create(int left, int top, int width, int containerId, int forward, int back) {
        return Button.builder(Component.empty(), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(containerId, mc.hasShiftDown() ? back : forward);
        }).bounds(left + width - RIGHT - WIDTH, top + TOP, WIDTH, HEIGHT).build();
    }

    /** "OC off", "OC x2", "OC x4" or "OC x8". */
    public static Component label(int level) {
        return level <= 0 ? Component.translatable("screen.nexusessentials.overclock.off")
                : Component.translatable("screen.nexusessentials.overclock.on", Overclock.multiplier(level));
    }

    /** Refreshes the label and the tooltip, which says what the machine draws at this level. */
    public static void refresh(Button button, int level, int energyPerTick) {
        button.setMessage(label(level));
        button.setTooltip(Tooltip.create(Component.translatable("screen.nexusessentials.overclock.tip",
                Overclock.multiplier(level), (long) energyPerTick * Overclock.multiplier(level))));
    }

    private OverclockUi() {}
}
