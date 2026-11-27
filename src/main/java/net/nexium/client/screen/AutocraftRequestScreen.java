package net.nexium.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.nexium.autocraft.AutocraftPayloads;
import net.nexium.util.Fmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The craft request window: everything the network has a pattern for, a search, an amount, and the plan the network makes for it
 * (what is crafted, what is taken from storage, what is missing). Start sends the request to the network's Autocrafters.
 */
public class AutocraftRequestScreen extends Screen {
    private static final int W = 230, COLS = 11, ROWS = 4, CELL = 18;
    private static final int PANEL = 0xE0101016, EDGE = 0xFF50505F, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98, GOLD = 0xFFFFBE3C,
            RED = 0xFFFF5555, GREEN = 0xFF55D86B, HOVER = 0x50FFFFFF, SEL = 0x60FFBE3C;

    private final BlockPos anchor;
    private final List<ItemStack> all;
    private List<ItemStack> shown;
    private EditBox search, amount;
    private Button start;
    private int left, top, scroll, selected = -1;
    private AutocraftPayloads.PlanResult plan;
    private int sincePlan = -1;

    public AutocraftRequestScreen(AutocraftPayloads.OpenRequest data) {
        super(Component.translatable("screen.nexium.autocraft.title"));
        this.anchor = data.anchor();
        this.all = data.craftables();
        this.shown = all;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - 190) / 2;
        search = addRenderableWidget(new EditBox(font, left + 8, top + 20, 110, 14, Component.translatable("screen.nexium.search")));
        search.setResponder(t -> { filter(t); });
        amount = addRenderableWidget(new EditBox(font, left + 160, top + 20, 60, 14, Component.translatable("screen.nexium.autocraft.amount")));
        amount.setValue("1");
        amount.setMaxLength(7);
        amount.setResponder(t -> sincePlan = 0);
        start = addRenderableWidget(Button.builder(Component.translatable("screen.nexium.autocraft.start"), b -> send(true))
                .bounds(left + W - 78, top + 164, 70, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.autocraft.jobs"), b -> {
            if (minecraft != null) minecraft.gui.setScreen(new AutocraftMonitorScreen(anchor));
        }).bounds(left + 8, top + 164, 60, 18).build());
        start.active = false;
        setInitialFocus(search);
    }

    private void filter(String text) {
        String q = text.toLowerCase(Locale.ROOT).trim();
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : all) if (q.isEmpty() || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) out.add(s);
        shown = out;
        scroll = 0;
        selected = -1;
        plan = null;
        start.active = false;
    }

    private long wanted() {
        try { return Math.max(1, Long.parseLong(amount.getValue().trim())); } catch (NumberFormatException e) { return 1; }
    }

    private void send(boolean go) {
        if (selected < 0 || selected >= shown.size()) return;
        ClientPlayNetworking.send(new AutocraftPayloads.Plan(anchor, shown.get(selected).copyWithCount(1), wanted(), go));
    }

    /** Picks the item at {@code index} in the list as a click does (for the client test). */
    public void select(int index) {
        if (index < 0 || index >= shown.size()) return;
        selected = index;
        plan = null;
        start.active = false;
        send(false);
    }

    /** Presses Start (for the client test). */
    public void pressStart() {
        send(true);
    }

    public boolean planReady() {
        return plan != null && plan.ok();
    }

    public void onPlan(AutocraftPayloads.PlanResult result) {
        plan = result;
        start.active = result.ok();
        if (result.started() && minecraft != null) minecraft.gui.setScreen(new AutocraftMonitorScreen(anchor));
    }

    @Override
    public void tick() {
        super.tick();
        if (sincePlan >= 0 && ++sincePlan > 6) { sincePlan = -1; send(false); }
    }

    private int cellAt(double mx, double my) {
        int x = (int) (mx - (left + 8)), y = (int) (my - (top + 42));
        if (x < 0 || y < 0 || x >= COLS * CELL || y >= ROWS * CELL) return -1;
        int i = scroll * COLS + (y / CELL) * COLS + x / CELL;
        return i < shown.size() ? i : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int i = cellAt(event.x(), event.y());
        if (i >= 0) {
            selected = i;
            plan = null;
            start.active = false;
            send(false);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int maxScroll = Math.max(0, (shown.size() + COLS - 1) / COLS - ROWS);
        scroll = Mth.clamp(scroll - (int) Math.signum(sy), 0, maxScroll);
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + 190, EDGE);
        g.fill(left + 1, top + 1, left + W - 1, top + 189, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title, left + 8, top + 7, GOLD, false);
        g.text(font, Component.translatable("screen.nexium.autocraft.amount"), left + 125, top + 23, DIM, false);
        if (all.isEmpty()) g.text(font, Component.translatable("screen.nexium.autocraft.none"), left + 8, top + 50, RED, false);
        int hover = cellAt(mouseX, mouseY);
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            int i = (scroll + r) * COLS + c;
            if (i >= shown.size()) continue;
            int x = left + 8 + c * CELL, y = top + 42 + r * CELL;
            if (i == selected) g.fill(x, y, x + CELL, y + CELL, SEL);
            else if (i == hover) g.fill(x, y, x + CELL, y + CELL, HOVER);
            g.item(shown.get(i), x + 1, y + 1);
        }
        if (hover >= 0) g.setTooltipForNextFrame(font, shown.get(hover), mouseX, mouseY);
        int y = top + 118;
        if (plan != null) {
            if (plan.tooBig()) g.text(font, Component.translatable("screen.nexium.autocraft.too_big"), left + 8, y, RED, false);
            y = line(g, "screen.nexium.autocraft.craft", plan.craft(), left + 8, y, GREEN);
            y = line(g, "screen.nexium.autocraft.use", plan.use(), left + 8, y, TEXT);
            line(g, "screen.nexium.autocraft.missing", plan.missing(), left + 8, y, RED);
        } else if (selected >= 0) {
            g.text(font, Component.translatable("screen.nexium.autocraft.planning"), left + 8, y, DIM, false);
        }
    }

    private int line(GuiGraphicsExtractor g, String key, List<AutocraftPayloads.Entry> entries, int x, int y, int colour) {
        if (entries.isEmpty()) return y;
        StringBuilder sb = new StringBuilder();
        for (AutocraftPayloads.Entry e : entries) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(Fmt.compact(e.count())).append("x ").append(e.stack().getHoverName().getString());
        }
        Component text = Component.translatable(key, sb.toString());
        var lines = font.split(text, W - 16);
        for (int i = 0; i < Math.min(2, lines.size()); i++) { g.text(font, lines.get(i), x, y, colour, false); y += 10; }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
