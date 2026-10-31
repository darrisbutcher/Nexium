package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Colours for the plain-rectangle panels and slot frames the mod's screens draw. */
public record Palette(int outline, int body, int highlight, int shadow,
                      int slotFill, int slotDark, int slotLight, int text) {
    /** Vanilla-style light grey. */
    public static final Palette LIGHT = new Palette(0xFF000000, 0xFFC6C6C6, 0xFFFFFFFF, 0xFF555555,
            0xFF8B8B8B, 0xFF555555, 0xFFFFFFFF, 0xFF404040);
    /** Matches vanilla's dark inventory: grey panel, slots with a dark top-left and light bottom-right edge. */
    public static final Palette DARK = new Palette(0xFF000000, 0xFF535353, 0xFF6B6B6B, 0xFF262626,
            0xFF333333, 0xFF262626, 0xFF6B6B6B, 0xFFE0E0E0);

    public void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, outline);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, highlight);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, highlight);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, shadow);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, shadow);
    }

    /** A recessed 16x16 slot whose top-left corner is (x, y). */
    public void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, slotDark);
        g.fill(x, y, x + 17, y + 17, slotLight);
        g.fill(x, y, x + 16, y + 16, slotFill);
    }
}
