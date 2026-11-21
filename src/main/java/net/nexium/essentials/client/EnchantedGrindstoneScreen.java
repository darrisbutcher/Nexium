package net.nexium.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.nexium.client.screen.BaseScreen;
import net.nexium.essentials.grindstone.EnchantedGrindstoneMenu;
import net.nexium.essentials.grindstone.EnchantmentPulling;

import java.util.List;

import static net.nexium.essentials.grindstone.EnchantedGrindstoneMenu.*;

/**
 * The Enchanted Grindstone: the item on the left, a plain Book under it, and a list of the enchantments on the item. Click one to
 * pull it onto a book of its own. A row is dim while there is no Book to use; with more enchantments than rows, the list scrolls
 * (mouse wheel).
 */
public class EnchantedGrindstoneScreen extends BaseScreen<EnchantedGrindstoneMenu> {
    private static final int WELL = 0xFF18181E, ROW = 0xFF2A2A34, ROW_HOT = 0xFF464682, ROW_EDGE = 0xFF9696E6;
    private static final int TEXT = 0xFFF0F0F0, MUTED = 0xFFAAAAAA, DIM = 0xFF787878, CURSE = 0xFFFF6B6B, CURSE_DIM = 0xFF8A4A4A, LEVEL = 0xFFFFD866;
    private static final int SCROLLBAR = 0xFF5A5A82, SCROLLBAR_BACK = 0xFF0C0C10;

    /** Index of the first row showing. */
    private int scroll;

    public EnchantedGrindstoneScreen(EnchantedGrindstoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT, INVENTORY_Y);
    }

    private int maxScroll() {
        return Math.max(0, menu.entries().size() - VISIBLE_ROWS);
    }

    private boolean scrolls() {
        return menu.entries().size() > VISIBLE_ROWS;
    }

    /** The rows' width: a scroll bar takes a few pixels on the right. */
    private int rowWidth() {
        return LIST_W - 2 - (scrolls() ? 4 : 0);
    }

    /** The first row showing (for the client test). */
    public int scroll() {
        return scroll;
    }

    public void setScroll(int rows) {
        scroll = Math.max(0, Math.min(maxScroll(), rows));
    }

    /** The centre of the row for the {@code index}th enchantment of the list, in screen coordinates (for tests, which click it); null when it is not showing. */
    public int[] rowCentre(int index) {
        if (index < scroll || index >= scroll + VISIBLE_ROWS || index >= menu.entries().size()) return null;
        return new int[]{leftPos + LIST_X + 1 + rowWidth() / 2, rowTop(index) + ROW_H / 2};
    }

    /** The centre of a slot, in screen coordinates (for tests, which click it). */
    public int[] slotCentre(Slot slot) {
        return new int[]{leftPos + slot.x + 8, topPos + slot.y + 8};
    }

    private int rowTop(int index) {
        return topPos + LIST_Y + 1 + (index - scroll) * ROW_H;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrolls() && scrollY != 0 && inside(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_W, VISIBLE_ROWS * ROW_H + 2)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && minecraft != null && minecraft.gameMode != null && menu.hasBook()) {
            List<EnchantmentPulling.Entry> entries = menu.entries();
            for (int i = scroll; i < Math.min(entries.size(), scroll + VISIBLE_ROWS); i++) {
                if (inside(event.x(), event.y(), leftPos + LIST_X + 1, rowTop(i), rowWidth(), ROW_H)) {
                    if (menu.hasPullable()) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.buttonFor(entries.get(i).enchantment()));
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.nexium.grindstone.item"), leftPos + INPUT_X, topPos + INPUT_Y - 10, MUTED, false);
        g.text(font, Component.translatable("screen.nexium.grindstone.book"), leftPos + BOOK_X, topPos + BOOK_Y - 10, MUTED, false);
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y, listH = VISIBLE_ROWS * ROW_H + 2;
        g.fill(lx, ly, lx + LIST_W, ly + listH, palette().outline());
        g.fill(lx + 1, ly + 1, lx + LIST_W - 1, ly + listH - 1, WELL);

        List<EnchantmentPulling.Entry> entries = menu.entries();
        boolean usable = menu.hasBook() && menu.hasPullable();
        if (entries.isEmpty() || !menu.hasPullable()) {
            message(g, lx, ly, entries.isEmpty() ? "screen.nexium.grindstone.empty" : "screen.nexium.grindstone.single");
            return;
        }
        setScroll(scroll);
        int rowW = rowWidth();
        for (int i = scroll; i < Math.min(entries.size(), scroll + VISIBLE_ROWS); i++) {
            EnchantmentPulling.Entry e = entries.get(i);
            int ry = rowTop(i);
            boolean hot = usable && inside(mouseX, mouseY, lx + 1, ry, rowW, ROW_H);
            if (hot) {
                g.fill(lx + 1, ry, lx + 1 + rowW, ry + ROW_H, ROW_EDGE);
                g.fill(lx + 2, ry + 1, lx + rowW, ry + ROW_H - 1, ROW_HOT);
            } else {
                g.fill(lx + 1, ry, lx + 1 + rowW, ry + ROW_H, ROW);
                g.fill(lx + 1, ry + ROW_H - 1, lx + 1 + rowW, ry + ROW_H, WELL);
            }
            Enchantment enchantment = e.enchantment().value();
            int colour = e.curse() ? (usable ? CURSE : CURSE_DIM) : (usable ? TEXT : DIM);
            String level = enchantment.getMaxLevel() == 1 ? "" : Component.translatableWithFallback("enchantment.level." + e.level(), String.valueOf(e.level())).getString();
            int levelW = level.isEmpty() ? 0 : font.width(level) + 4;
            g.text(font, font.plainSubstrByWidth(enchantment.description().getString(), rowW - 8 - levelW), lx + 4, ry + 3, colour, false);
            if (!level.isEmpty()) g.text(font, level, lx + 1 + rowW - font.width(level) - 3, ry + 3, usable ? LEVEL : DIM, false);
        }
        if (scrolls()) {
            int bx = lx + LIST_W - 4, top = ly + 1, height = VISIBLE_ROWS * ROW_H;
            g.fill(bx, top, bx + 3, top + height, SCROLLBAR_BACK);
            int thumb = Math.max(8, height * VISIBLE_ROWS / entries.size());
            int thumbY = top + (maxScroll() == 0 ? 0 : (height - thumb) * scroll / maxScroll());
            g.fill(bx, thumbY, bx + 3, thumbY + thumb, SCROLLBAR);
        }
    }

    /** A line or two of help in the empty list. */
    private void message(GuiGraphicsExtractor g, int lx, int ly, String key) {
        List<FormattedCharSequence> lines = font.split(Component.translatable(key), LIST_W - 12);
        int y = ly + 8;
        for (FormattedCharSequence line : lines) {
            g.text(font, line, lx + 6, y, MUTED, false);
            y += 11;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // A hint over the row under the mouse (what a click will do), or why it will do nothing
        if (menu.entries().isEmpty() || !menu.hasPullable()) return;
        List<EnchantmentPulling.Entry> entries = menu.entries();
        for (int i = scroll; i < Math.min(entries.size(), scroll + VISIBLE_ROWS); i++) {
            if (inside(mouseX, mouseY, leftPos + LIST_X + 1, rowTop(i), rowWidth(), ROW_H)) {
                Component tip = Component.translatable(menu.hasBook() ? "screen.nexium.grindstone.pull" : "screen.nexium.grindstone.need_book");
                g.setTooltipForNextFrame(font, font.split(tip, 150), mouseX, mouseY);
                return;
            }
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
