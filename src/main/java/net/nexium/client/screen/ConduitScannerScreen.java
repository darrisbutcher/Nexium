package net.nexium.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.nexium.client.ScannerOutline;
import net.nexium.conduit.ConduitScan;
import net.nexium.conduit.ConduitScan.Link;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.ScanReportPayload;
import net.nexium.conduit.ScanRequestPayload;
import net.nexium.conduit.SideConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * The Conduit Scanner window: everything on one conduit network, one conduit type at a time (a tab each). Connections are grouped by
 * channel colour, with what takes things out of a block (Out) and what puts them in (In) under each colour, or listed one per row; a
 * button switches. Each row shows the block, where it is, its mode, priority, filter and redstone setting, and a red mark if something is
 * wrong (nothing sends or receives on its channel, redstone is holding it off, or it is paused). Click a row to outline it in the world.
 * The window updates once a second.
 */
public class ConduitScannerScreen extends Screen {
    private static final int PANEL_W = 400, ROW = 24;
    private static final int GOLD = 0xFFFFBE3C, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98, RED = 0xFFFF5555, ORANGE = 0xFFFFA23C, GREEN = 0xFF55D86B;
    private static final int PANEL = 0xE0101016, EDGE = 0xFF50505F, ROW_BG = 0x30FFFFFF, ROW_HOVER = 0x50FFFFFF, HEADER_BG = 0x50000000;

    /** One line of the list: a channel heading, or a connection (seen as taking out of its block or putting into it, in the grouped layout). */
    public record Row(boolean header, int channel, Link link, boolean takesOut) {}

    private static boolean lastGrouped = true;

    private final BlockPosHolder origin;
    private ConduitScan.Report report;
    private ConduitType wanted;
    private boolean grouped = lastGrouped;
    /** How many rows fit: as many as the screen's height allows (a small game window at a big GUI scale has few). */
    private int visible = 6;
    private int left, top, panelH, scroll, sinceRequest;
    private final Button[] tabs = new Button[ConduitType.VALUES.length];
    private Button layoutButton;
    private List<Row> rows = List.of();

    /** Just the position, so a report for another conduit is not mistaken for ours. */
    private record BlockPosHolder(net.minecraft.core.BlockPos pos) {}

    public ConduitScannerScreen(ScanReportPayload data) {
        super(Component.translatable("item.nexium.conduit_scanner"));
        this.origin = new BlockPosHolder(data.report().origin());
        this.report = data.report();
        this.wanted = data.report().type();
        rebuild();
    }

    // ---- state for tests -------------------------------------------------------------------------------------------

    public ConduitScan.Report report() {
        return report;
    }

    public ConduitType shownType() {
        return report.type();
    }

    public boolean grouped() {
        return grouped;
    }

    public List<Row> rows() {
        return rows;
    }

    /** Opens the tab for {@code type}, as a click on it would. */
    public void selectTab(ConduitType type) {
        if (!report.carries(type) || type == wanted) return;
        wanted = type;
        scroll = 0;
        ClientPlayNetworking.send(new ScanRequestPayload(origin.pos(), type.ordinal()));
    }

    /** Switches between the grouped and list layouts, as a click on the button would. */
    public void toggleLayout() {
        if (report.type() == ConduitType.ENERGY) return;
        grouped = !grouped;
        lastGrouped = grouped;
        scroll = 0;
        rebuild();
        refreshButtons();
    }

    /** The middle of a row, in GUI coordinates (for tests). */
    public int[] rowPoint(int index) {
        return new int[]{left + PANEL_W / 2, top + 44 + (index - scroll) * ROW + ROW / 2};
    }

    /** Clicks a row, which outlines it in the world (for tests). */
    public void clickRow(int index) {
        if (index >= 0 && index < rows.size() && !rows.get(index).header()) ScannerOutline.toggle(rows.get(index).link());
    }

    /** A new report from the server for this window's conduit. */
    public void update(ScanReportPayload data) {
        if (!data.report().origin().equals(origin.pos()) || data.report().type() != wanted) return;
        report = data.report();
        rebuild();
        refreshButtons();
    }

    // ---- the rows --------------------------------------------------------------------------------------------------

    private void rebuild() {
        List<Row> out = new ArrayList<>();
        if (grouped && report.type() != ConduitType.ENERGY) {
            TreeSet<Integer> channels = new TreeSet<>();
            for (Link l : report.links()) {
                if (l.mode().extracts()) channels.add(l.outChannel());
                if (l.mode().inserts()) channels.add(l.inChannel());
            }
            for (int c : channels) {
                out.add(new Row(true, c, null, false));
                for (Link l : report.links()) if (l.mode().extracts() && l.outChannel() == c) out.add(new Row(false, c, l, true));
                for (Link l : report.links()) if (l.mode().inserts() && l.inChannel() == c) out.add(new Row(false, c, l, false));
            }
        } else {
            List<Link> sorted = new ArrayList<>(report.links());
            sorted.sort(Comparator.comparingInt((Link l) -> Math.max(l.inChannel(), l.outChannel())).thenComparing(l -> l.mode().ordinal()));
            for (Link l : sorted) out.add(new Row(false, l.mode().extracts() ? l.outChannel() : l.inChannel(), l, l.mode().extracts()));
        }
        rows = out;
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - visible));
    }

    // ---- widgets ---------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        visible = Mth.clamp((height - 8 - 62 - 30) / ROW, 3, 12);
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - visible));
        panelH = 62 + visible * ROW + 30;
        left = (width - PANEL_W) / 2;
        top = Math.max(4, (height - panelH) / 2);
        int x = left + 8;
        for (ConduitType t : ConduitType.VALUES) {
            final ConduitType type = t;
            tabs[t.ordinal()] = addRenderableWidget(Button.builder(Component.translatable("conduit.nexium." + t.key()), b -> selectTab(type))
                    .bounds(x, top + 22, 60, 16).build());
            x += 62;
        }
        layoutButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggleLayout())
                .bounds(left + PANEL_W - 78, top + 22, 70, 16)
                .tooltip(Tooltip.create(Component.translatable("screen.nexium.scanner.layout.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.scanner.clear"), b -> ScannerOutline.clear())
                .bounds(left + 8, top + panelH - 22, 90, 16)
                .tooltip(Tooltip.create(Component.translatable("screen.nexium.scanner.clear.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.config.done"), b -> onClose())
                .bounds(left + PANEL_W - 78, top + panelH - 22, 70, 16).build());
        refreshButtons();
    }

    private void refreshButtons() {
        for (ConduitType t : ConduitType.VALUES) {
            Button b = tabs[t.ordinal()];
            if (b == null) continue;
            b.visible = report.carries(t);
            b.active = t != report.type();
        }
        if (layoutButton != null) {
            boolean energy = report.type() == ConduitType.ENERGY;
            layoutButton.active = !energy;
            layoutButton.setMessage(Component.translatable(grouped && !energy ? "screen.nexium.scanner.layout.grouped" : "screen.nexium.scanner.layout.list"));
        }
    }

    @Override
    public void tick() {
        // A fresh report each second
        if (++sinceRequest >= 20) {
            sinceRequest = 0;
            ClientPlayNetworking.send(new ScanRequestPayload(origin.pos(), wanted.ordinal()));
        }
    }

    // ---- input -----------------------------------------------------------------------------------------------------

    private int rowAt(double mouseX, double mouseY) {
        int listTop = top + 44;
        if (mouseX < left + 6 || mouseX >= left + PANEL_W - 10 || mouseY < listTop || mouseY >= listTop + visible * ROW) return -1;
        int index = scroll + (int) ((mouseY - listTop) / ROW);
        return index < rows.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int index = rowAt(event.x(), event.y());
        if (index < 0) return false;
        clickRow(index);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rows.size() > visible) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, rows.size() - visible);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- drawing ---------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + PANEL_W, top + panelH, EDGE);
        g.fill(left + 1, top + 1, left + PANEL_W - 1, top + panelH - 1, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 8, GOLD, false);
        int warnings = 0;
        for (Link l : report.links()) if (l.flags() != 0) warnings++;
        Component summary = Component.translatable("screen.nexium.scanner.summary", report.conduits(), report.links().size());
        g.text(font, summary, left + PANEL_W - 8 - font.width(summary), top + 8, DIM, false);
        if (warnings > 0) {
            Component warn = Component.translatable("screen.nexium.scanner.warnings", warnings).withStyle(ChatFormatting.RED);
            g.text(font, warn, left + 8 + font.width(title.copy().withStyle(ChatFormatting.BOLD)) + 10, top + 8, RED, false);
        }

        int listTop = top + 44;
        int hovered = rowAt(mouseX, mouseY);
        if (rows.isEmpty()) {
            g.centeredText(font, Component.translatable("screen.nexium.scanner.empty"), left + PANEL_W / 2, listTop + visible * ROW / 2, DIM);
        }
        for (int i = 0; i < visible; i++) {
            int index = scroll + i;
            if (index >= rows.size()) break;
            int y = listTop + i * ROW;
            Row row = rows.get(index);
            if (row.header()) drawHeader(g, row, y);
            else drawLink(g, row, y, index == hovered);
        }
        if (rows.size() > visible) {
            int trackH = visible * ROW, thumb = Math.max(10, trackH * visible / rows.size());
            int thumbY = listTop + (trackH - thumb) * scroll / Math.max(1, rows.size() - visible);
            g.fill(left + PANEL_W - 8, listTop, left + PANEL_W - 5, listTop + trackH, 0xFF2A2A36);
            g.fill(left + PANEL_W - 8, thumbY, left + PANEL_W - 5, thumbY + thumb, 0xFF8C8CAA);
        }
        if (report.truncated()) {
            g.text(font, font.plainSubstrByWidth(Component.translatable("screen.nexium.scanner.truncated").getString(), 210), left + 104, top + panelH - 18, ORANGE, false);
        } else if (report.type() == ConduitType.ENERGY) {
            g.text(font, font.plainSubstrByWidth(Component.translatable("screen.nexium.scanner.no_channels").getString(), 210), left + 104, top + panelH - 18, DIM, false);
        }
        if (hovered >= 0 && !rows.get(hovered).header()) drawTooltip(g, rows.get(hovered), mouseX, mouseY);
    }

    private void drawHeader(GuiGraphicsExtractor g, Row row, int y) {
        g.fill(left + 6, y, left + PANEL_W - 10, y + ROW - 2, HEADER_BG);
        swatch(g, left + 10, y + 5, row.channel());
        Component name = channelName(row.channel());
        g.text(font, Component.translatable("screen.nexium.scanner.channel", name).copy().withStyle(ChatFormatting.BOLD), left + 28, y + 8, TEXT, false);
    }

    private void drawLink(GuiGraphicsExtractor g, Row row, int y, boolean hover) {
        Link l = row.link();
        g.fill(left + 6, y, left + PANEL_W - 10, y + ROW - 2, hover ? ROW_HOVER : ROW_BG);
        int x = left + 10 + (grouped && report.type() != ConduitType.ENERGY ? 8 : 0);
        Block block = BuiltInRegistries.BLOCK.getValue(l.block());
        ItemStack icon = block == Blocks.AIR ? ItemStack.EMPTY : new ItemStack(block);
        if (!icon.isEmpty() && icon.getItem() != Items.AIR) g.item(icon, x, y + 3);
        int nameX = x + 20;
        g.text(font, font.plainSubstrByWidth(block.getName().getString(), 150), nameX, y + 3, TEXT, false);
        String where = l.target().getX() + ", " + l.target().getY() + ", " + l.target().getZ();
        g.text(font, where + "  " + Component.translatable("direction.nexium." + l.side().getSerializedName()).getString(), nameX, y + 13, DIM, false);

        int bx = left + 196;
        boolean outRow = grouped && report.type() != ConduitType.ENERGY ? row.takesOut() : l.mode().extracts();
        Component badge;
        int badgeColor;
        if (grouped && report.type() != ConduitType.ENERGY) {
            badge = Component.translatable(outRow ? "screen.nexium.scanner.out" : "screen.nexium.scanner.in");
            badgeColor = outRow ? ORANGE : GREEN;
        } else {
            badge = Component.translatable("screen.nexium.scanner.mode." + l.mode().name().toLowerCase(Locale.ROOT));
            badgeColor = l.mode() == SideConfig.Mode.EXTRACT ? ORANGE : l.mode() == SideConfig.Mode.INSERT ? GREEN : GOLD;
        }
        g.text(font, badge, bx, y + 8, badgeColor, false);
        int cx = bx + 52;
        if (!(grouped && report.type() != ConduitType.ENERGY) && report.type() != ConduitType.ENERGY) {
            // The list layout has no colour headings: show both channels as swatches (in, then out)
            if (l.mode().inserts()) swatch(g, cx, y + 5, l.inChannel());
            if (l.mode().extracts()) swatch(g, cx + 16, y + 5, l.outChannel());
            cx += 36;
        }
        // Small tags: priority, filter, round robin, redstone, speed
        StringBuilder tags = new StringBuilder();
        if (l.priority() != 0) tags.append('P').append(l.priority()).append(' ');
        if (l.filtered()) tags.append(Component.translatable("screen.nexium.scanner.tag.filter").getString()).append(' ');
        if (l.roundRobin()) tags.append(Component.translatable("screen.nexium.scanner.tag.round_robin").getString()).append(' ');
        if (l.redstone() != SideConfig.RedstoneControl.ALWAYS) tags.append(Component.translatable("screen.nexium.scanner.tag.redstone." + l.redstone().name().toLowerCase(Locale.ROOT)).getString()).append(' ');
        if (l.speedUpgrades() > 0) tags.append('+').append(l.speedUpgrades()).append(' ');
        g.text(font, font.plainSubstrByWidth(tags.toString().trim(), left + PANEL_W - 34 - cx), cx, y + 8, DIM, false);
        if (flagsFor(row) != 0) g.text(font, "!", left + PANEL_W - 24, y + 8, RED, false);
    }

    /** The warnings that count for this row: in the grouped layout, a row that takes out cares about receivers, one that puts in about senders. */
    private int flagsFor(Row row) {
        int flags = row.link().flags();
        if (grouped && report.type() != ConduitType.ENERGY) {
            flags &= ~(row.takesOut() ? ConduitScan.NO_SENDER : ConduitScan.NO_RECEIVER);
        }
        return flags;
    }

    private void swatch(GuiGraphicsExtractor g, int x, int y, int channel) {
        if (channel < 0) return;
        g.fill(x, y, x + 12, y + 12, 0xFF000000);
        g.fill(x + 1, y + 1, x + 11, y + 11, ConduitConfigScreen.CHANNEL_COLORS[Math.floorMod(channel, 16)]);
    }

    private static Component channelName(int channel) {
        return Component.translatable("color.minecraft." + ConduitConfigScreen.COLOR_KEYS[Math.floorMod(channel, 16)]);
    }

    private void drawTooltip(GuiGraphicsExtractor g, Row row, int mouseX, int mouseY) {
        Link l = row.link();
        Block block = BuiltInRegistries.BLOCK.getValue(l.block());
        List<Component> lines = new ArrayList<>();
        lines.add(block.getName().copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(l.target().getX() + ", " + l.target().getY() + ", " + l.target().getZ()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.nexium.scanner.tip.side", Component.translatable("direction.nexium." + l.side().getSerializedName()),
                l.conduit().getX() + ", " + l.conduit().getY() + ", " + l.conduit().getZ()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.nexium.scanner.tip.mode", Component.translatable("screen.nexium.scanner.mode." + l.mode().name().toLowerCase(Locale.ROOT))));
        if (report.type() != ConduitType.ENERGY) {
            if (l.mode().extracts()) lines.add(Component.translatable("screen.nexium.scanner.tip.out", channelName(l.outChannel())).withStyle(ChatFormatting.GOLD));
            if (l.mode().inserts()) lines.add(Component.translatable("screen.nexium.scanner.tip.in", channelName(l.inChannel())).withStyle(ChatFormatting.GREEN));
        }
        if (l.priority() != 0) lines.add(Component.translatable("screen.nexium.scanner.tip.priority", l.priority()));
        if (l.filtered()) lines.add(Component.translatable("screen.nexium.scanner.tip.filter"));
        if (l.redstone() != SideConfig.RedstoneControl.ALWAYS) {
            lines.add(Component.translatable("screen.nexium.scanner.tip.redstone." + l.redstone().name().toLowerCase(Locale.ROOT)));
        }
        int flags = flagsFor(row);
        if ((flags & ConduitScan.NO_RECEIVER) != 0) lines.add(Component.translatable("screen.nexium.scanner.warn.no_receiver", channelName(l.outChannel())).withStyle(ChatFormatting.RED));
        if ((flags & ConduitScan.NO_SENDER) != 0) lines.add(Component.translatable("screen.nexium.scanner.warn.no_sender", channelName(l.inChannel())).withStyle(ChatFormatting.RED));
        if ((flags & ConduitScan.REDSTONE_OFF) != 0) lines.add(Component.translatable("screen.nexium.scanner.warn.redstone").withStyle(ChatFormatting.RED));
        if ((flags & ConduitScan.PAUSED) != 0) lines.add(Component.translatable("screen.nexium.scanner.warn.paused").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("screen.nexium.scanner.tip.click").withStyle(ChatFormatting.DARK_GRAY));
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(font.split(line, 230));
        g.setTooltipForNextFrame(font, wrapped, mouseX, mouseY);
    }
}
