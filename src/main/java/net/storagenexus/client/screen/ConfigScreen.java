package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.storagenexus.essentials.config.ConfigSchema;
import net.storagenexus.essentials.config.ConfigSchema.Entry;
import net.storagenexus.essentials.config.NexusConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nexium settings screen: every setting in {@code config/nexusessentials.json}, grouped by feature. A switch is a
 * button, a number is a box (clamped to its limits when you leave it), and each setting has its own Reset beside it;
 * "Reset page" and "Reset all" put a whole group or everything back. Changes are saved as they are made. Hover a setting
 * for what it does, its default and whether it is for your own game or for the world you host.
 */
public class ConfigScreen extends Screen {
    private static final int PAD = 8, SIDE = 112, ROW = 24, TOP = 30, BOTTOM_NOTE = 36, CONTROL = 60, RESET = 40;
    private static final int PANEL = 0xD8101016, EDGE = 0xFF50505F, GOLD = 0xFFFFBE3C, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98;

    /** The group shown last, so the screen opens where it was left. */
    private static String lastGroup = ConfigSchema.GROUPS.getFirst();

    private final Screen parent;
    private String group;
    private int firstRow;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> groupButtons = new ArrayList<>();
    private Button resetPage;
    private int listLeft, listRight, listTop, listBottom;

    /** One setting on the page: its label's place and the widgets that edit it. */
    private static final class Row {
        final Entry entry;
        Button toggle;
        EditBox box;
        Button reset;

        Row(Entry entry) {
            this.entry = entry;
        }
    }

    public ConfigScreen(Screen parent) {
        super(Component.translatable("screen.storagenexus.config"));
        this.parent = parent;
        this.group = lastGroup;
    }

    // ---- state for tests -------------------------------------------------------------------------------------------

    public String group() {
        return group;
    }

    public int rowCount() {
        return rows.size();
    }

    /** The settings on the current page, in order. */
    public List<Entry> page() {
        return rows.stream().map(r -> r.entry).toList();
    }

    public void showGroup(String id) {
        if (!ConfigSchema.GROUPS.contains(id)) return;
        group = id;
        lastGroup = id;
        firstRow = 0;
        rebuildRows();
    }

    // ---- widgets ---------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        listLeft = PAD + SIDE + 10;
        listRight = width - PAD - 10;
        listTop = TOP + 4;
        listBottom = height - BOTTOM_NOTE - PAD;

        groupButtons.clear();
        int y = TOP;
        for (String id : ConfigSchema.GROUPS) {
            Button b = addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.config.group." + id),
                    button -> showGroup(id)).bounds(PAD, y, SIDE, 20).build());
            groupButtons.add(b);
            y += 22;
        }
        int bottom = height - PAD - 20;
        addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.config.done"), b -> onClose())
                .bounds(PAD, bottom, SIDE, 20).build());
        resetPage = addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.config.reset_page"), b -> resetPage())
                .bounds(PAD, bottom - 44, SIDE, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.config.reset_page.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.config.reset_all"), b -> resetAll())
                .bounds(PAD, bottom - 22, SIDE, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.config.reset_all.tip"))).build());
        rebuildRows();
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW);
    }

    private void rebuildRows() {
        for (Row row : rows) {
            if (row.toggle != null) removeWidget(row.toggle);
            if (row.box != null) removeWidget(row.box);
            removeWidget(row.reset);
        }
        rows.clear();
        NexusConfig config = NexusConfig.get();
        for (Entry entry : ConfigSchema.in(group)) {
            Row row = new Row(entry);
            if (entry.isSwitch()) {
                row.toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    entry.setBoolean(NexusConfig.get(), !entry.getBoolean(NexusConfig.get()));
                    changed();
                }).bounds(0, 0, CONTROL, 18).build());
            } else {
                row.box = new EditBox(font, 0, 0, CONTROL, 18, Component.translatable(entry.nameKey()));
                row.box.setMaxLength(9);
                row.box.setValue(Integer.toString(entry.getInt(config)));
                row.box.setResponder(text -> {
                    try {
                        int value = Integer.parseInt(text.trim());
                        entry.setInt(NexusConfig.get(), value);
                        NexusConfig.save();
                    } catch (NumberFormatException ignored) {
                        // Half-typed: keep the old value until the box is left
                    }
                });
                addRenderableWidget(row.box);
            }
            row.reset = addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.config.reset"), b -> {
                entry.reset(NexusConfig.get());
                changed();
            }).bounds(0, 0, RESET, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.config.reset.tip"))).build());
            rows.add(row);
        }
        firstRow = Mth.clamp(firstRow, 0, Math.max(0, rows.size() - visibleRows()));
        refresh();
    }

    /** After a setting changed: save, and update every widget to match. */
    private void changed() {
        NexusConfig.save();
        refresh();
    }

    private void resetPage() {
        for (Entry e : ConfigSchema.in(group)) e.reset(NexusConfig.get());
        changed();
    }

    private void resetAll() {
        for (Entry e : ConfigSchema.ENTRIES) e.reset(NexusConfig.get());
        changed();
    }

    /** Puts the widgets where the scroll position says, hides the ones off the page and shows the current values. */
    private void refresh() {
        NexusConfig config = NexusConfig.get();
        int controlX = listRight - RESET - 4 - CONTROL;
        int visible = visibleRows();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            boolean shown = i >= firstRow && i < firstRow + visible;
            int y = listTop + (i - firstRow) * ROW + 3;
            Entry e = row.entry;
            if (row.toggle != null) {
                row.toggle.setPosition(controlX, y);
                row.toggle.visible = shown;
                row.toggle.setMessage(Component.translatable(e.getBoolean(config) ? "screen.storagenexus.config.on" : "screen.storagenexus.config.off"));
            }
            if (row.box != null) {
                row.box.setPosition(controlX, y);
                row.box.visible = shown;
                if (!row.box.isFocused()) row.box.setValue(Integer.toString(e.getInt(config)));
            }
            row.reset.setPosition(listRight - RESET, y);
            row.reset.visible = shown;
            row.reset.active = !e.isDefault(config);
        }
        for (int i = 0; i < groupButtons.size(); i++) groupButtons.get(i).active = !ConfigSchema.GROUPS.get(i).equals(group);
        if (resetPage != null) resetPage.active = ConfigSchema.in(group).stream().anyMatch(e -> !e.isDefault(config));
    }

    @Override
    public void tick() {
        // A box that has been left shows the real (clamped) value again
        refresh();
    }

    // ---- input and drawing -----------------------------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listLeft - 4 && rows.size() > visibleRows()) {
            firstRow = Mth.clamp(firstRow - (int) Math.signum(scrollY), 0, rows.size() - visibleRows());
            refresh();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // A click outside the boxes leaves them, so the value is tidied
        boolean handled = super.mouseClicked(event, doubleClick);
        refresh();
        return handled;
    }

    @Override
    public void onClose() {
        NexusConfig.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(PAD - 2, 4, width - PAD + 2, height - 4, PANEL);
        g.fill(PAD - 2, 4, width - PAD + 2, 5, EDGE);
        g.fill(PAD - 2, height - 5, width - PAD + 2, height - 4, EDGE);
        g.fill(PAD + SIDE + 4, TOP, PAD + SIDE + 5, height - PAD - 4, EDGE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), PAD + 2, 12, GOLD, false);
        g.text(font, Component.translatable("screen.storagenexus.config.group." + group), listLeft, 12, TEXT, false);
        NexusConfig config = NexusConfig.get();
        int visible = visibleRows();
        int labelRight = listRight - RESET - 4 - CONTROL - 6;
        Row hovered = null;
        for (int i = firstRow; i < rows.size() && i < firstRow + visible; i++) {
            Row row = rows.get(i);
            int y = listTop + (i - firstRow) * ROW;
            Entry e = row.entry;
            Component name = Component.translatable(e.nameKey());
            int color = e.isDefault(config) ? TEXT : GOLD;
            // Long names are cut to the room there is
            String shown = font.plainSubstrByWidth(name.getString(), labelRight - listLeft);
            g.text(font, shown, listLeft, y + 8, color, false);
            if (mouseX >= listLeft && mouseX < listRight && mouseY >= y && mouseY < y + ROW) hovered = row;
        }
        if (rows.size() > visible) {
            int trackTop = listTop, trackHeight = visible * ROW;
            int thumb = Math.max(12, trackHeight * visible / rows.size());
            int thumbY = trackTop + (trackHeight - thumb) * firstRow / Math.max(1, rows.size() - visible);
            g.fill(width - PAD - 6, trackTop, width - PAD - 3, trackTop + trackHeight, 0xFF2A2A36);
            g.fill(width - PAD - 6, thumbY, width - PAD - 3, thumbY + thumb, 0xFF8C8CAA);
        }
        List<FormattedCharSequence> note = font.split(Component.translatable("screen.storagenexus.config.note"), listRight - listLeft);
        for (int i = 0; i < note.size() && i < 2; i++) g.text(font, note.get(i), listLeft, height - BOTTOM_NOTE + i * 10, DIM, false);
        if (hovered != null && !(hovered.reset.isHovered())) drawTooltip(g, hovered, mouseX, mouseY);
    }

    private void drawTooltip(GuiGraphicsExtractor g, Row row, int mouseX, int mouseY) {
        Entry e = row.entry;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(e.nameKey()).withStyle(net.minecraft.ChatFormatting.GOLD));
        lines.add(Component.translatable(e.tipKey()));
        NexusConfig defaults = new NexusConfig();
        Component applies = Component.translatable(e.clientSide() ? "screen.storagenexus.config.client" : "screen.storagenexus.config.server");
        if (e.isSwitch()) {
            lines.add(Component.translatable("screen.storagenexus.config.default",
                    Component.translatable(e.getBoolean(defaults) ? "screen.storagenexus.config.on" : "screen.storagenexus.config.off"))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("screen.storagenexus.config.range", e.min(), e.max(), e.getInt(defaults))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("screen.storagenexus.config.applies", applies).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        int wrap = Math.min(220, Math.max(120, width / 2));
        for (Component line : lines) wrapped.addAll(font.split(line, wrap));
        g.setTooltipForNextFrame(font, wrapped, mouseX, mouseY);
    }
}
