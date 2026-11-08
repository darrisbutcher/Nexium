package net.storagenexus.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.assembler.AssemblyTarget;
import net.storagenexus.essentials.assembler.FarmAssemblerMenu;
import net.storagenexus.essentials.assembler.Requirement;

import java.util.ArrayList;
import java.util.List;

import static net.storagenexus.essentials.assembler.FarmAssemblerMenu.*;

/**
 * The Farm Assembler: what it can make down the left, what the chosen one needs on the right (how much it holds, a bar,
 * and a tick when a line is done), a button that moves what it needs out of your inventory, and Build.
 */
public class FarmAssemblerScreen extends AbstractContainerScreen<FarmAssemblerMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF313138, HIGHLIGHT = 0xFF5A5A68, SHADOW = 0xFF14141A;
    private static final int WELL = 0xFF18181E, ROW = 0xFF1C1C22, SELECTED = 0xFF464682, SELECTED_EDGE = 0xFF9696E6;
    private static final int SLOT_FILL = 0xFF202026, SLOT_DARK = 0xFF0E0E12, SLOT_LIGHT = 0xFF3C3C46;
    private static final int TEXT = 0xFFF0F0F0, MUTED = 0xFFAAAAAA, DIM = 0xFF787878, GOLD = 0xFFFFBE3C;
    private static final int DONE = 0xFF6EE66E, NOT_DONE = 0xFFFFFF55, BAR_DONE = 0xFF50C85A, BAR_NOT_DONE = 0xFFDCBE3C, BAR_BACK = 0xFF0C0C10;
    private static final int BUTTON = 0xFF484854, BUTTON_HOT = 0xFF5A5A82, BUTTON_OFF = 0xFF28282E, BUTTON_TOP = 0xFF78788C, BUTTON_BOTTOM = 0xFF282830;

    /** Rows of the list showing at once; with more targets than that, it scrolls (mouse wheel). */
    public static final int VISIBLE_ROWS = 7;
    private static final int SCROLLBAR = 0xFF5A5A82, SCROLLBAR_BACK = 0xFF0C0C10;
    /** Index of the first row showing. */
    private int listScroll;
    private AssemblyTarget lastChosen;

    private static int targets() {
        return AssemblyTarget.values().length;
    }

    private int maxScroll() {
        return Math.max(0, targets() - VISIBLE_ROWS);
    }

    private boolean scrolls() {
        return targets() > VISIBLE_ROWS;
    }

    /** Whether this target's row is showing right now. */
    private boolean rowShowing(AssemblyTarget t) {
        return t.ordinal() >= listScroll && t.ordinal() < listScroll + VISIBLE_ROWS;
    }

    /** The top of this target's row (when it is showing). */
    private int rowTop(AssemblyTarget t) {
        return topPos + LIST_Y + 1 + (t.ordinal() - listScroll) * LIST_ROW;
    }

    /** Where the rows' text may reach: a scroll bar takes a few pixels on the right. */
    private int rowWidth() {
        return LIST_W - 2 - (scrolls() ? 4 : 0);
    }

    /** The list follows a changed choice: the chosen row is always in view. */
    private void keepChosenInView() {
        AssemblyTarget chosen = menu.target();
        if (chosen == lastChosen) return;
        lastChosen = chosen;
        if (chosen.ordinal() < listScroll) listScroll = chosen.ordinal();
        if (chosen.ordinal() >= listScroll + VISIBLE_ROWS) listScroll = chosen.ordinal() - VISIBLE_ROWS + 1;
        listScroll = Math.max(0, Math.min(maxScroll(), listScroll));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrolls() && scrollY != 0 && inside(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_W, VISIBLE_ROWS * LIST_ROW + 2)) {
            listScroll = Math.max(0, Math.min(maxScroll(), listScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** The first row showing (for the client test). */
    public int listScroll() {
        return listScroll;
    }

    public void setListScroll(int rows) {
        listScroll = Math.max(0, Math.min(maxScroll(), rows));
    }

    public FarmAssemblerScreen(FarmAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        inventoryLabelX = INVENTORY_X;
        inventoryLabelY = INVENTORY_Y - 10;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, GOLD, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BODY);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, HIGHLIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, HIGHLIGHT);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, SHADOW);
        for (Slot s : menu.slots) slot(g, leftPos + s.x, topPos + s.y);

        AssemblyTarget chosen = menu.target();

        // What it can make
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        AssemblyTarget[] all = AssemblyTarget.values();
        keepChosenInView();
        int shown = Math.min(all.length, VISIBLE_ROWS);
        int rowW = rowWidth();
        g.fill(lx, ly, lx + LIST_W, ly + shown * LIST_ROW + 2, OUTLINE);
        g.fill(lx + 1, ly + 1, lx + LIST_W - 1, ly + shown * LIST_ROW + 1, WELL);
        for (AssemblyTarget t : all) {
            if (!rowShowing(t)) continue;
            int ry = rowTop(t);
            boolean sel = t == chosen, hot = !sel && inside(mouseX, mouseY, lx + 1, ry, rowW, LIST_ROW);
            if (sel) {
                g.fill(lx + 1, ry, lx + 1 + rowW, ry + LIST_ROW, SELECTED_EDGE);
                g.fill(lx + 2, ry + 1, lx + rowW, ry + LIST_ROW - 1, SELECTED);
            } else if (hot) {
                g.fill(lx + 1, ry, lx + 1 + rowW, ry + LIST_ROW, ROW);
            }
            g.item(t.icon(), lx + 2, ry);
            g.text(font, font.plainSubstrByWidth(t.shortName().getString(), rowW - 21), lx + 21, ry + 4, sel ? TEXT : MUTED, sel);
        }
        if (scrolls()) {
            // A scroll bar down the right edge of the list
            int bx = lx + LIST_W - 4, top = ly + 1, height = shown * LIST_ROW;
            g.fill(bx, top, bx + 3, top + height, SCROLLBAR_BACK);
            int thumb = Math.max(8, height * VISIBLE_ROWS / all.length);
            int thumbY = top + (maxScroll() == 0 ? 0 : (height - thumb) * listScroll / maxScroll());
            g.fill(bx, thumbY, bx + 3, thumbY + thumb, SCROLLBAR);
        }
        button(g, leftPos + LIST_X, topPos + ADD_Y, LIST_W, Component.translatable("screen.nexusessentials.assembler.add"), true,
                inside(mouseX, mouseY, leftPos + LIST_X, topPos + ADD_Y, LIST_W, BUTTON_H));

        // The chosen one: what it is, and what it needs
        int rx = leftPos + RIGHT_X, rw = RIGHT_W;
        g.item(chosen.icon(), rx, topPos + HEADER_Y);
        g.text(font, chosen.displayName(), rx + 20, topPos + HEADER_Y + 4, TEXT, true);
        g.text(font, font.plainSubstrByWidth(chosen.makes().getString(), rw), rx, topPos + MAKES_Y, MUTED, false);
        boolean canBuild = menu.complete() && !menu.outputFull();
        button(g, leftPos + BUILD_X, topPos + HEADER_Y + 1, BUILD_W, Component.translatable("screen.nexusessentials.assembler.build"), canBuild,
                canBuild && inside(mouseX, mouseY, leftPos + BUILD_X, topPos + HEADER_Y + 1, BUILD_W, BUTTON_H));

        List<Requirement> reqs = chosen.requirements();
        for (int i = 0; i < reqs.size(); i++) {
            int ry = topPos + ROWS_Y + i * ROW_H;
            int have = menu.count(i), need = menu.needed(i);
            boolean done = have >= need;
            g.fill(rx, ry, rx + rw, ry + ROW_H, ROW);
            g.pose().pushMatrix();
            g.pose().translate(rx + 1, ry);
            g.pose().scale(0.6875F, 0.6875F);
            g.item(new ItemStack(reqs.get(i).icon()), 0, 0);
            g.pose().popMatrix();
            g.text(font, reqs.get(i).name(), rx + 14, ry + 1, TEXT, false);
            String amount = String.format("%,d / %,d", have, need);
            int ax = rx + rw - 2 - (done ? 9 : 0) - font.width(amount);
            g.text(font, amount, ax, ry + 1, done ? DONE : NOT_DONE, false);
            if (done) tick(g, rx + rw - 9, ry + 2);
            int bx0 = rx + 14, bx1 = rx + rw - 2, by = ry + ROW_H - 2;
            g.fill(bx0, by, bx1, by + 1, BAR_BACK);
            int filled = need <= 0 ? bx1 - bx0 : (int) ((long) (bx1 - bx0) * Math.min(have, need) / need);
            if (filled > 0) g.fill(bx0, by, bx0 + filled, by + 1, done ? BAR_DONE : BAR_NOT_DONE);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        AssemblyTarget chosen = menu.target();
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        for (AssemblyTarget t : AssemblyTarget.values()) {
            if (rowShowing(t) && inside(mouseX, mouseY, lx + 1, rowTop(t), rowWidth(), LIST_ROW)) {
                List<Component> lines = new ArrayList<>(List.of(t.displayName().copy().withStyle(ChatFormatting.WHITE), t.makes().copy().withStyle(ChatFormatting.GRAY)));
                if (t != chosen) lines.add(Component.translatable("screen.nexusessentials.assembler.switch").withStyle(ChatFormatting.DARK_GRAY));
                g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
                return;
            }
        }
        if (inside(mouseX, mouseY, lx, topPos + ADD_Y, LIST_W, BUTTON_H)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.nexusessentials.assembler.add_tip").withStyle(ChatFormatting.GRAY),
                    Component.translatable("screen.nexusessentials.assembler.add_tip2").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, leftPos + BUILD_X, topPos + HEADER_Y + 1, BUILD_W, BUTTON_H)) {
            g.setComponentTooltipForNextFrame(font, buildTip(chosen), mouseX, mouseY);
            return;
        }
        List<Requirement> reqs = chosen.requirements();
        for (int i = 0; i < reqs.size(); i++) {
            if (inside(mouseX, mouseY, leftPos + RIGHT_X, topPos + ROWS_Y + i * ROW_H, RIGHT_W, ROW_H)) {
                List<Component> lines = new ArrayList<>();
                lines.add(reqs.get(i).name().copy().withStyle(ChatFormatting.WHITE));
                if (reqs.get(i).mob()) lines.add(Component.translatable("screen.nexusessentials.assembler.mob").withStyle(ChatFormatting.GRAY));
                int missing = menu.needed(i) - menu.count(i);
                lines.add(missing > 0 ? Component.translatable("screen.nexusessentials.assembler.still", String.format("%,d", missing)).withStyle(ChatFormatting.YELLOW)
                        : Component.translatable("screen.nexusessentials.assembler.done").withStyle(ChatFormatting.GREEN));
                g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
                return;
            }
        }
    }

    private List<Component> buildTip(AssemblyTarget chosen) {
        List<Component> lines = new ArrayList<>();
        if (menu.outputFull()) {
            lines.add(Component.translatable("screen.nexusessentials.assembler.take_first").withStyle(ChatFormatting.RED));
        } else if (menu.complete()) {
            lines.add(Component.translatable("screen.nexusessentials.assembler.ready", chosen.displayName()).withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.translatable("screen.nexusessentials.assembler.needs").withStyle(ChatFormatting.YELLOW));
            List<Requirement> reqs = chosen.requirements();
            for (int i = 0; i < reqs.size(); i++) {
                int missing = menu.needed(i) - menu.count(i);
                if (missing > 0) lines.add(Component.literal(String.format("%,d ", missing)).append(reqs.get(i).name()).withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && minecraft != null && minecraft.gameMode != null) {
            double mx = event.x(), my = event.y();
            int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
            for (AssemblyTarget t : AssemblyTarget.values()) {
                if (t != menu.target() && rowShowing(t) && inside(mx, my, lx + 1, rowTop(t), rowWidth(), LIST_ROW)) {
                    press(t.ordinal());
                    return true;
                }
            }
            if (inside(mx, my, lx, topPos + ADD_Y, LIST_W, BUTTON_H)) {
                press(BUTTON_ADD);
                return true;
            }
            if (menu.complete() && !menu.outputFull() && inside(mx, my, leftPos + BUILD_X, topPos + HEADER_Y + 1, BUILD_W, BUTTON_H)) {
                press(BUTTON_BUILD);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, Component label, boolean enabled, boolean hot) {
        g.fill(x, y, x + w, y + BUTTON_H, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + BUTTON_H - 1, !enabled ? BUTTON_OFF : hot ? BUTTON_HOT : BUTTON);
        if (enabled) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, BUTTON_TOP);
            g.fill(x + 1, y + BUTTON_H - 2, x + w - 1, y + BUTTON_H - 1, BUTTON_BOTTOM);
        }
        g.text(font, label, x + (w - font.width(label)) / 2, y + (BUTTON_H - 8) / 2, enabled ? TEXT : DIM, enabled);
    }

    private static void tick(GuiGraphicsExtractor g, int x, int y) {
        // a small check mark, 7 wide and 6 tall
        int[][] px = {{0, 3}, {1, 4}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {6, 1}, {6, 0}};
        for (int[] p : px) g.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 2, DONE);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
