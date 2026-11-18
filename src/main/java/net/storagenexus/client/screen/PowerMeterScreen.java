package net.storagenexus.client.screen;

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
import net.storagenexus.conduit.EnergyNetwork;
import net.storagenexus.conduit.EnergyNetwork.Endpoint;
import net.storagenexus.energy.PowerControlPayload;
import net.storagenexus.energy.PowerReportPayload;
import net.storagenexus.energy.PowerRequestPayload;

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

    private BlockPos meter;
    private EnergyNetwork.Report report;
    private int left, top, panelH, scroll, sinceRequest;
    private final Button[] priority = new Button[ROWS], power = new Button[ROWS];
    private final Endpoint[] shown = new Endpoint[ROWS];

    public PowerMeterScreen(PowerReportPayload data) {
        super(Component.translatable("block.storagenexus.power_meter"));
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
        int listX = left + 150;
        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            int y = top + 40 + i * ROW;
            priority[i] = addRenderableWidget(Button.builder(Component.empty(), b -> cyclePriority(shown[row]))
                    .bounds(left + PANEL_W - 104, y, 52, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.power.priority.tip"))).build());
            power[i] = addRenderableWidget(Button.builder(Component.empty(), b -> togglePower(shown[row]))
                    .bounds(left + PANEL_W - 50, y, 40, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.power.switch.tip"))).build());
        }
        refresh();
    }

    private void cyclePriority(Endpoint e) {
        if (e == null || e.role() != EnergyNetwork.Role.CONSUMER) return;
        int next = e.priority() == 0 ? 5 : e.priority() > 0 ? -5 : 0;
        ClientPlayNetworking.send(new PowerControlPayload(meter, e.pos(), next, e.paused()));
    }

    private void togglePower(Endpoint e) {
        if (e == null) return;
        ClientPlayNetworking.send(new PowerControlPayload(meter, e.pos(), e.priority(), !e.paused()));
    }

    private void refresh() {
        List<Endpoint> all = report == null ? List.of() : report.endpoints();
        scroll = Mth.clamp(scroll, 0, Math.max(0, all.size() - ROWS));
        for (int i = 0; i < ROWS; i++) {
            Endpoint e = scroll + i < all.size() ? all.get(scroll + i) : null;
            shown[i] = e;
            priority[i].visible = power[i].visible = e != null;
            if (e == null) continue;
            priority[i].active = e.role() == EnergyNetwork.Role.CONSUMER;
            priority[i].setMessage(Component.translatable(e.role() != EnergyNetwork.Role.CONSUMER ? "screen.storagenexus.power.priority.none"
                    : e.priority() > 0 ? "screen.storagenexus.power.priority.high"
                    : e.priority() < 0 ? "screen.storagenexus.power.priority.low" : "screen.storagenexus.power.priority.normal"));
            power[i].setMessage(Component.translatable(e.paused() ? "screen.storagenexus.power.off" : "screen.storagenexus.power.on")
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
        if (report != null && report.endpoints().size() > ROWS) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, report.endpoints().size() - ROWS);
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
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 8, GOLD, false);
        if (report == null) {
            g.centeredText(font, Component.translatable("screen.storagenexus.power.none"), left + PANEL_W / 2, top + panelH / 2, DIM);
            return;
        }
        EnergyNetwork.Report r = report;
        int x = left + 8, y = top + 28;
        // Status
        Component status = r.deficit() ? Component.translatable("screen.storagenexus.power.deficit").withStyle(ChatFormatting.RED)
                : r.produced() >= r.used() ? Component.translatable("screen.storagenexus.power.ok").withStyle(ChatFormatting.GREEN)
                : Component.translatable("screen.storagenexus.power.draining").withStyle(ChatFormatting.GOLD);
        g.text(font, status, x, y, TEXT, false);
        y += 14;
        g.text(font, Component.translatable("screen.storagenexus.power.making", rate(r.produced())), x, y, GREEN, false);
        y += 11;
        g.text(font, Component.translatable("screen.storagenexus.power.using", rate(r.used())), x, y, ORANGE, false);
        y += 11;
        float net = r.produced() - r.used();
        g.text(font, Component.translatable("screen.storagenexus.power.net", (net >= 0 ? "+" : "-") + rate(Math.abs(net))), x, y, net >= 0 ? GREEN : RED, false);
        y += 16;
        // Stored: the pool and the batteries together
        long stored = r.stored() + r.batteryStored(), capacity = Math.max(1, r.capacity() + r.batteryCapacity());
        g.text(font, Component.translatable("screen.storagenexus.power.stored", compact(stored), compact(capacity)), x, y, TEXT, false);
        y += 11;
        g.fill(x, y, x + 130, y + 8, BAR_BG);
        g.fill(x, y, x + (int) (130L * stored / capacity), y + 8, 0xFF3FB8FF);
        y += 12;
        Component left_ = r.secondsLeft() >= 0 ? Component.translatable("screen.storagenexus.power.lasts", duration(r.secondsLeft()))
                : net > 0.01f ? Component.translatable("screen.storagenexus.power.charging") : Component.translatable("screen.storagenexus.power.steady");
        g.text(font, left_, x, y, DIM, false);
        y += 16;
        // A minute of history: green is made, orange is used
        g.text(font, Component.translatable("screen.storagenexus.power.history"), x, y, DIM, false);
        int keyX = x + font.width(Component.translatable("screen.storagenexus.power.history")) + 6;
        g.text(font, Component.translatable("screen.storagenexus.power.history.made"), keyX, y, GREEN, false);
        g.text(font, Component.translatable("screen.storagenexus.power.history.used"), keyX + font.width(Component.translatable("screen.storagenexus.power.history.made")) + 4, y, ORANGE, false);
        y += 11;
        drawGraph(g, x, y, 130, 46, r);

        // Column heads of the list
        int listX = left + 150;
        g.text(font, Component.translatable("screen.storagenexus.power.blocks", r.endpoints().size()), listX, top + 28, DIM, false);
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
            Component roleName = Component.translatable("screen.storagenexus.power.role." + e.role().name().toLowerCase(Locale.ROOT));
            g.text(font, roleName, listX + 20 + font.width(rateText) + 6, ry + 11, DIM, false);
        }
        if (r.endpoints().size() > ROWS) {
            int trackH = ROWS * ROW, thumb = Math.max(10, trackH * ROWS / r.endpoints().size());
            int thumbY = top + 40 + (trackH - thumb) * scroll / Math.max(1, r.endpoints().size() - ROWS);
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
