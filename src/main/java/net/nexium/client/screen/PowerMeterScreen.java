package net.nexium.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.nexium.conduit.EnergyNetwork;
import net.nexium.conduit.EnergyNetwork.Endpoint;
import net.nexium.energy.PowerControlPayload;
import net.nexium.energy.PowerReportPayload;
import net.nexium.energy.PowerRequestPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Power Meter dashboard: what the Energy Conduit network it sits against makes and uses, how much is stored and how long
 * it lasts, a minute of history, and every block on the network with its rate. Each machine has a priority button (High,
 * Normal or Low: when power runs short the Low ones go without first) and an On/Off switch, which set the conduit sides that
 * feed it. The numbers update once a second.
 */
public class PowerMeterScreen extends Screen {
    private static final int ROW = 22, ROWS = 7, PANEL_W = 400;
    private static final int GREEN = 0xFF55D86B, ORANGE = 0xFFFFA23C, RED = 0xFFFF5555, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98, GOLD = 0xFFFFBE3C;
    private static final int BAR_BG = 0xFF1A1A22, EDGE = 0xFF50505F, PANEL = 0xE0101016;

    /** The tabs of the list, left to right: what makes power, the batteries, what uses it. */
    private static final EnergyNetwork.Role[] TAB_ROLE = {EnergyNetwork.Role.PRODUCER, EnergyNetwork.Role.BATTERY, EnergyNetwork.Role.CONSUMER};
    private static final String[] TAB_KEY = {"generation", "batteries", "consumers"};
    /** The tab last used, so the next dashboard opens on it. */
    private static int lastTab;

    private BlockPos meter;
    private EnergyNetwork.Report report;
    private int left, top, panelH, scroll, sinceRequest, tab = lastTab;
    private final Button[] tabButtons = new Button[TAB_ROLE.length];
    private final Button[] priority = new Button[ROWS], power = new Button[ROWS];
    private final Endpoint[] shown = new Endpoint[ROWS];

    public PowerMeterScreen(PowerReportPayload data) {
        super(Component.translatable("block.nexium.power_meter"));
        this.meter = data.meter();
        this.report = data.report();
    }

    public BlockPos meter() {
        return meter;
    }

    public EnergyNetwork.Report report() {
        return report;
    }

    /** A new report from the server for this meter. */
    public void update(PowerReportPayload data) {
        if (!data.meter().equals(meter)) return;
        report = data.report();
        refresh();
    }

    /** The tab shown: 0 generation, 1 batteries, 2 consumers. */
    public int tab() {
        return tab;
    }

    /** The middle of a tab's button in GUI pixels (for the client test). */
    public int[] tabCentre(int index) {
        return new int[]{tabButtons[index].getX() + tabButtons[index].getWidth() / 2, tabButtons[index].getY() + tabButtons[index].getHeight() / 2};
    }

    /** Switches to a tab, as a click on it would. */
    public void selectTab(int index) {
        tab = Mth.clamp(index, 0, TAB_ROLE.length - 1);
        lastTab = tab;
        scroll = 0;
        refresh();
    }

    /** The blocks of the tab shown, in report order. */
    private List<Endpoint> tabbed() {
        List<Endpoint> out = new ArrayList<>();
        if (report == null) return out;
        for (Endpoint e : report.endpoints()) if (e.role() == TAB_ROLE[tab]) out.add(e);
        return out;
    }

    /** The rows shown now, top first (for tests). */
    public List<Endpoint> rows() {
        List<Endpoint> out = new ArrayList<>();
        for (Endpoint e : shown) if (e != null) out.add(e);
        return out;
    }

    /** Presses a row's priority button or power switch, as a click would (for tests). */
    public void pressPriority(int row) {
        cyclePriority(shown[row]);
    }

    public void pressPower(int row) {
        togglePower(shown[row]);
    }

    @Override
    protected void init() {
        panelH = 40 + ROWS * ROW + 12;
        left = (width - PANEL_W) / 2;
        top = Math.max(4, (height - panelH) / 2);
        int listX = left + 150, tabW = 78, tabGap = 3;
        for (int t = 0; t < TAB_ROLE.length; t++) {
            final int index = t;
            tabButtons[t] = addRenderableWidget(Button.builder(Component.translatable("screen.nexium.power.tab." + TAB_KEY[t]), b -> selectTab(index))
                    .bounds(listX + t * (tabW + tabGap), top + 22, tabW, 14).build());
        }
        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            int y = top + 40 + i * ROW;
            priority[i] = addRenderableWidget(Button.builder(Component.empty(), b -> cyclePriority(shown[row]))
                    .bounds(left + PANEL_W - 104, y, 52, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.nexium.power.priority.tip"))).build());
            power[i] = addRenderableWidget(Button.builder(Component.empty(), b -> togglePower(shown[row]))
                    .bounds(left + PANEL_W - 50, y, 40, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.nexium.power.switch.tip"))).build());
        }
        refresh();
    }

    private void cyclePriority(Endpoint e) {
        if (e == null || e.role() != EnergyNetwork.Role.CONSUMER || e.direct()) return;
        int next = e.priority() == 0 ? 5 : e.priority() > 0 ? -5 : 0;
        ClientPlayNetworking.send(new PowerControlPayload(meter, e.pos(), next, e.paused()));
    }

    private void togglePower(Endpoint e) {
        if (e == null || e.direct()) return;
        ClientPlayNetworking.send(new PowerControlPayload(meter, e.pos(), e.priority(), !e.paused()));
    }

    private void refresh() {
        List<Endpoint> all = tabbed();
        scroll = Mth.clamp(scroll, 0, Math.max(0, all.size() - ROWS));
        for (int t = 0; t < tabButtons.length; t++) {
            Component name = Component.translatable("screen.nexium.power.tab." + TAB_KEY[t]);
            tabButtons[t].setMessage(t == tab ? name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : name.copy().withStyle(ChatFormatting.WHITE));
        }
        for (int i = 0; i < ROWS; i++) {
            Endpoint e = scroll + i < all.size() ? all.get(scroll + i) : null;
            shown[i] = e;
            power[i].visible = e != null;
            // Priority only means something for what uses power
            priority[i].visible = e != null && TAB_ROLE[tab] == EnergyNetwork.Role.CONSUMER;
            if (e == null) continue;
            priority[i].active = e.role() == EnergyNetwork.Role.CONSUMER && !e.direct();
            power[i].active = !e.direct();
            priority[i].setMessage(Component.translatable(e.direct() ? "screen.nexium.power.priority.battery"
                    : e.role() != EnergyNetwork.Role.CONSUMER ? "screen.nexium.power.priority.none"
                    : e.priority() > 0 ? "screen.nexium.power.priority.high"
                    : e.priority() < 0 ? "screen.nexium.power.priority.low" : "screen.nexium.power.priority.normal"));
            power[i].setMessage(Component.translatable(e.paused() ? "screen.nexium.power.off" : "screen.nexium.power.on")
                    .withStyle(e.paused() ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
    }

    @Override
    public void tick() {
        // A fresh report each second
        if (++sinceRequest >= 20) {
            sinceRequest = 0;
            ClientPlayNetworking.send(new PowerRequestPayload(meter));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int count = tabbed().size();
        if (count > ROWS) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, count - ROWS);
            refresh();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + PANEL_W, top + panelH, EDGE);
        g.fill(left + 1, top + 1, left + PANEL_W - 1, top + panelH - 1, PANEL);
        g.fill(left + 146, top + 24, left + 147, top + panelH - 6, EDGE);
        // A bar under the tab in use, and a line under all of them
        int listX = left + 150;
        g.fill(listX, top + 37, left + PANEL_W - 10, top + 38, EDGE);
        g.fill(listX + tab * 81, top + 36, listX + tab * 81 + 78, top + 38, GOLD);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 8, GOLD, false);
        if (report == null) {
            g.centeredText(font, Component.translatable("screen.nexium.power.none"), left + PANEL_W / 2, top + panelH / 2, DIM);
            return;
        }
        EnergyNetwork.Report r = report;
        int x = left + 8, y = top + 28;
        // Status
        Component status = r.deficit() ? Component.translatable("screen.nexium.power.deficit").withStyle(ChatFormatting.RED)
                : r.produced() >= r.used() ? Component.translatable("screen.nexium.power.ok").withStyle(ChatFormatting.GREEN)
                : Component.translatable("screen.nexium.power.draining").withStyle(ChatFormatting.GOLD);
        g.text(font, status, x, y, TEXT, false);
        y += 14;
        g.text(font, Component.translatable("screen.nexium.power.making", rate(r.produced())), x, y, GREEN, false);
        y += 11;
        g.text(font, Component.translatable("screen.nexium.power.using", rate(r.used())), x, y, ORANGE, false);
        y += 11;
        float net = r.produced() - r.used();
        g.text(font, Component.translatable("screen.nexium.power.net", (net >= 0 ? "+" : "-") + rate(Math.abs(net))), x, y, net >= 0 ? GREEN : RED, false);
        y += 16;
        // Stored: the pool and the batteries together
        long stored = r.stored() + r.batteryStored(), capacity = Math.max(1, r.capacity() + r.batteryCapacity());
        g.text(font, Component.translatable("screen.nexium.power.stored", compact(stored), compact(capacity)), x, y, TEXT, false);
        y += 11;
        g.fill(x, y, x + 130, y + 8, BAR_BG);
        g.fill(x, y, x + (int) (130L * stored / capacity), y + 8, 0xFF3FB8FF);
        y += 12;
        Component left_ = r.secondsLeft() >= 0 ? Component.translatable("screen.nexium.power.lasts", duration(r.secondsLeft()))
                : net > 0.01f ? Component.translatable("screen.nexium.power.charging") : Component.translatable("screen.nexium.power.steady");
        g.text(font, left_, x, y, DIM, false);
        y += 16;
        // A minute of history: green is made, orange is used
        g.text(font, Component.translatable("screen.nexium.power.history"), x, y, DIM, false);
        int keyX = x + font.width(Component.translatable("screen.nexium.power.history")) + 6;
        g.text(font, Component.translatable("screen.nexium.power.history.made"), keyX, y, GREEN, false);
        g.text(font, Component.translatable("screen.nexium.power.history.used"), keyX + font.width(Component.translatable("screen.nexium.power.history.made")) + 4, y, ORANGE, false);
        y += 11;
        drawGraph(g, x, y, 130, 46, r);

        // Column heads of the list
        int listX = left + 150;
        List<Endpoint> tabbed = tabbed();
        if (tabbed.isEmpty()) {
            g.text(font, Component.translatable("screen.nexium.power.empty." + TAB_KEY[tab]), listX, top + 46, DIM, false);
        }
        for (int i = 0; i < ROWS; i++) {
            Endpoint e = shown[i];
            if (e == null) continue;
            int ry = top + 40 + i * ROW;
            var level = minecraft.level;
            Block block = level == null ? null : level.getBlockState(e.pos()).getBlock();
            if (block != null) g.item(new ItemStack(block), listX, ry + 1);
            Component name = block == null ? Component.literal("?") : block.getName();
            int nameColor = e.paused() ? DIM : TEXT;
            g.text(font, font.plainSubstrByWidth(name.getString(), 122), listX + 20, ry + 2, nameColor, false);
            String rateText = switch (e.role()) {
                case PRODUCER -> "+" + rate(e.perTick());
                case CONSUMER -> "-" + rate(e.perTick());
                case BATTERY -> (e.perTick() >= 0 ? "+" : "-") + rate(Math.abs(e.perTick()));
            };
            int color = e.paused() ? DIM : e.role() == EnergyNetwork.Role.PRODUCER ? GREEN : e.role() == EnergyNetwork.Role.CONSUMER ? ORANGE : 0xFF3FB8FF;
            g.text(font, rateText, listX + 20, ry + 11, color, false);
            // The tab already says what it is; only a machine fed straight by a Battery needs a note
            if (e.direct()) g.text(font, Component.translatable("screen.nexium.power.role.direct"), listX + 20 + font.width(rateText) + 6, ry + 11, DIM, false);
        }
        if (tabbed.size() > ROWS) {
            int trackH = ROWS * ROW, thumb = Math.max(10, trackH * ROWS / tabbed.size());
            int thumbY = top + 40 + (trackH - thumb) * scroll / Math.max(1, tabbed.size() - ROWS);
            g.fill(left + PANEL_W - 8, top + 40, left + PANEL_W - 5, top + 40 + trackH, 0xFF2A2A36);
            g.fill(left + PANEL_W - 8, thumbY, left + PANEL_W - 5, thumbY + thumb, 0xFF8C8CAA);
        }
    }

    private void drawGraph(GuiGraphicsExtractor g, int x, int y, int w, int h, EnergyNetwork.Report r) {
        g.fill(x, y, x + w, y + h, BAR_BG);
        float[] made = r.producedHistory(), used = r.usedHistory();
        int n = made.length;
        float max = 1;
        for (int i = 0; i < n; i++) max = Math.max(max, Math.max(made[i], used[i]));
        int colW = Math.max(1, w / Math.max(1, n));
        for (int i = 0; i < n; i++) {
            int cx = x + w - (n - i) * colW;
            int mh = Math.round(h * made[i] / max), uh = Math.round(h * used[i] / max);
            if (mh > 0) g.fill(cx, y + h - mh, cx + colW, y + h, 0xAA55D86B);
            if (uh > 0) g.fill(cx, y + h - uh, cx + colW, y + h - uh + 1, ORANGE);
        }
        g.text(font, compact(Math.round(max)), x + 2, y + 2, DIM, false);
    }

    private static String rate(float perTick) {
        if (perTick >= 100) return compact(Math.round(perTick)) + " NE/t";
        if (perTick >= 10) return Math.round(perTick) + " NE/t";
        return String.format(Locale.ROOT, "%.1f NE/t", perTick);
    }

    static String compact(long v) {
        if (v >= 10_000_000) return (v / 1_000_000) + "M";
        if (v >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", v / 1_000_000.0);
        if (v >= 10_000) return (v / 1000) + "k";
        if (v >= 1000) return String.format(Locale.ROOT, "%.1fk", v / 1000.0);
        return Long.toString(v);
    }

    static String duration(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + (seconds % 3600 / 60) + "m";
        if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
