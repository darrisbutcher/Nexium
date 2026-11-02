package net.storagenexus.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hover hints for a menu's buttons, drawn beside the panel level with the hovered button, on whichever side has more
 * room and wrapped to fit it, so a hint never covers the menu itself the way a tooltip at the mouse does.
 */
final class SideHints {
    /** Tooltip background and border around the text. */
    private static final int FRAME = 4;
    private static final int GAP = 4;
    private static final int MAX_WIDTH = 170;

    private final Map<AbstractWidget, Component> hints = new LinkedHashMap<>();

    void set(AbstractWidget widget, Component hint) {
        hints.put(widget, hint);
    }

    void render(GuiGraphicsExtractor g, int panelLeft, int panelRight, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int roomRight = screenWidth - panelRight - GAP - 2 * FRAME;
        int roomLeft = panelLeft - GAP - 2 * FRAME;
        boolean right = roomRight >= roomLeft;
        int wrap = Math.max(40, Math.min(MAX_WIDTH, right ? roomRight : roomLeft));
        for (Map.Entry<AbstractWidget, Component> e : hints.entrySet()) {
            AbstractWidget widget = e.getKey();
            if (!widget.visible || !widget.isHovered()) continue;
            List<FormattedCharSequence> lines = minecraft.font.split(e.getValue(), wrap);
            ClientTooltipPositioner beside = (width, height, x, y, tooltipWidth, tooltipHeight) -> {
                int left = right ? panelRight + GAP + FRAME : panelLeft - GAP - FRAME - tooltipWidth;
                int top = Math.max(FRAME, Math.min(widget.getY(), height - tooltipHeight - FRAME));
                return new Vector2i(Math.max(FRAME, left), top);
            };
            g.setTooltipForNextFrame(minecraft.font, lines, Optional.empty(), beside, mouseX, mouseY, false,
                    Tooltip.create(e.getValue()).style());
            return;
        }
    }
}
