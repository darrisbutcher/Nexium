package net.nexium.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.filter.FilterData;
import net.nexium.filter.FilterTagsPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Chooses the item tags of an Advanced Filter: every item tag the game knows, in a list you can search, scroll and click. A click
 * switches a tag on or off, and each change goes to the server at once, so the filter in hand always has what is shown. Done (or Esc)
 * goes back to the filter.
 */
public class TagPickerScreen extends Screen {
    private static final int PANEL_W = 240, PANEL_H = 204, ROW = 16, ROWS = 8, PAD = 8;
    private static final int PANEL = 0xE0101016, EDGE = 0xFF50505F, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98, GREEN = 0xFF55D86B,
            ROW_BG = 0x30FFFFFF, ROW_HOVER = 0x60FFFFFF, CHOSEN_BG = 0x40305A38;

    /** One item tag: its id, how many items it holds, and one of them to draw. */
    public record Entry(Identifier id, int count, Item icon) {}

    private final FilterScreen parent;
    private final LinkedHashSet<Identifier> selected;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> shown = List.of();
    private EditBox search;
    private Button selectedOnlyButton;
    private boolean selectedOnly;
    private int left, top, scroll;

    public TagPickerScreen(FilterScreen parent, LinkedHashSet<Identifier> selected) {
        super(Component.translatable("screen.nexium.tag_picker"));
        this.parent = parent;
        this.selected = selected;
        for (HolderSet.Named<Item> named : BuiltInRegistries.ITEM.getTags().toList()) {
            Item icon = named.stream().findFirst().map(Holder::value).orElse(Items.AIR);
            all.add(new Entry(named.key().location(), named.size(), icon));
        }
        all.sort(Comparator.comparing(e -> e.id().toString()));
        refilter();
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = Math.max(4, (height - PANEL_H) / 2);
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left + PAD, top + 22, PANEL_W - PAD * 2, 14, Component.translatable("screen.nexium.tag_picker.search"));
        search.setHint(Component.translatable("screen.nexium.tag_picker.search"));
        search.setMaxLength(60);
        search.setValue(query);
        search.setResponder(text -> refilter());
        addRenderableWidget(search);
        setInitialFocus(search);
        int by = top + PANEL_H - PAD - 16;
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.tag_picker.clear"), b -> clear())
                .bounds(left + PAD, by, 56, 16).build());
        selectedOnlyButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            selectedOnly = !selectedOnly;
            refilter();
        }).bounds(left + PAD + 60, by, 88, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.tag_picker.done"), b -> onClose())
                .bounds(left + PANEL_W - PAD - 56, by, 56, 16).build());
        refilter();
    }

    // ---- state, also for tests -----------------------------------------------------------------------------------------

    public List<Identifier> selected() {
        return List.copyOf(selected);
    }

    public List<Entry> shown() {
        return shown;
    }

    public void setSearch(String text) {
        search.setValue(text);
        refilter();
    }

    /** The centre of the row at {@code index} among the tags shown (counted from the first one visible), for tests. */
    public int[] rowCentre(int index) {
        return new int[]{left + PANEL_W / 2, top + 42 + index * ROW + ROW / 2};
    }

    /** The centre of the Done button, in screen coordinates (for tests). */
    public int[] doneCentre() {
        return new int[]{left + PANEL_W - PAD - 28, top + PANEL_H - PAD - 8};
    }

    public void toggleRow(int index) {
        int i = scroll + index;
        if (i >= 0 && i < shown.size()) toggle(shown.get(i).id());
    }

    private void refilter() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        if (q.startsWith("#")) q = q.substring(1);
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (selectedOnly && !selected.contains(e.id())) continue;
            if (!q.isEmpty() && !e.id().toString().contains(q)) continue;
            out.add(e);
        }
        shown = out;
        scroll = Mth.clamp(scroll, 0, Math.max(0, shown.size() - ROWS));
        if (selectedOnlyButton != null) {
            selectedOnlyButton.setMessage(Component.translatable(selectedOnly ? "screen.nexium.tag_picker.all" : "screen.nexium.tag_picker.selected_only"));
        }
    }

    private void toggle(Identifier id) {
        if (!selected.remove(id)) {
            if (selected.size() >= FilterData.MAX_TAGS) return;
            selected.add(id);
        }
        send();
        if (selectedOnly) refilter();
    }

    private void clear() {
        selected.clear();
        send();
        refilter();
    }

    private void send() {
        ClientPlayNetworking.send(new FilterTagsPayload(new ArrayList<>(selected)));
    }

    // ---- input ------------------------------------------------------------------------------------------------------------

    private int rowAt(double x, double y) {
        if (x < left + PAD || x >= left + PANEL_W - PAD - 6 || y < top + 42 || y >= top + 42 + ROWS * ROW) return -1;
        int row = (int) ((y - (top + 42)) / ROW);
        return scroll + row < shown.size() ? row : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int row = rowAt(event.x(), event.y());
        if (row < 0 || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        toggleRow(row);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * 2, 0, Math.max(0, shown.size() - ROWS));
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // The menu behind this screen was closed (the filter left the hand, say): close this too
        if (minecraft != null && minecraft.player != null && minecraft.player.containerMenu != parent.filterMenu()) {
            minecraft.gui.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        parent.pickerClosed(selected.size());
        minecraft.gui.setScreen(parent);
    }

    // ---- drawing -----------------------------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, EDGE);
        g.fill(left + 1, top + 1, left + PANEL_W - 1, top + PANEL_H - 1, PANEL);
        g.text(font, title, left + PAD, top + 8, 0xFFFFBE3C, false);
        Component count = Component.translatable("screen.nexium.tag_picker.count", selected.size(), FilterData.MAX_TAGS);
        g.text(font, count, left + PANEL_W - PAD - font.width(count), top + 8, selected.isEmpty() ? DIM : GREEN, false);
        // The widgets (search, buttons) are drawn by super above the panel fill: draw them again on top
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) w.extractRenderState(g, mouseX, mouseY, partialTick);
        }
        int hovered = rowAt(mouseX, mouseY);
        for (int i = 0; i < ROWS && scroll + i < shown.size(); i++) {
            Entry e = shown.get(scroll + i);
            boolean on = selected.contains(e.id());
            int y = top + 42 + i * ROW;
            g.fill(left + PAD, y, left + PANEL_W - PAD - 6, y + ROW - 1, on ? CHOSEN_BG : i == hovered ? ROW_HOVER : ROW_BG);
            // A tick box
            g.fill(left + PAD + 3, y + 3, left + PAD + 12, y + 12, on ? GREEN : 0xFF000000);
            g.fill(left + PAD + 4, y + 4, left + PAD + 11, y + 11, on ? 0xFF2E8B45 : 0xFF202028);
            if (e.icon() != Items.AIR) g.item(new ItemStack(e.icon()), left + PAD + 16, y);
            String id = font.plainSubstrByWidth("#" + e.id(), PANEL_W - PAD * 2 - 6 - 40 - 28);
            g.text(font, id, left + PAD + 36, y + 4, on ? GREEN : TEXT, false);
            String n = Integer.toString(e.count());
            g.text(font, n, left + PANEL_W - PAD - 10 - font.width(n), y + 4, DIM, false);
        }
        if (shown.isEmpty()) {
            g.centeredText(font, Component.translatable("screen.nexium.tag_picker.none_found"), left + PANEL_W / 2, top + 42 + ROWS * ROW / 2, DIM);
        }
        if (shown.size() > ROWS) {
            int trackH = ROWS * ROW, thumb = Math.max(10, trackH * ROWS / shown.size());
            int thumbY = top + 42 + (trackH - thumb) * scroll / Math.max(1, shown.size() - ROWS);
            g.fill(left + PANEL_W - PAD - 4, top + 42, left + PANEL_W - PAD - 1, top + 42 + trackH, 0xFF2A2A36);
            g.fill(left + PANEL_W - PAD - 4, thumbY, left + PANEL_W - PAD - 1, thumbY + thumb, 0xFF8C8CAA);
        }
        if (hovered >= 0) {
            Entry e = shown.get(scroll + hovered);
            boolean on = selected.contains(e.id());
            net.nexium.client.Tooltips.show(g, font, List.of(
                    Component.literal("#" + e.id()),
                    Component.translatable("screen.nexium.tag_picker.items", e.count()).withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.translatable(on ? "screen.nexium.tag_picker.chosen" : "screen.nexium.tag_picker.choose")
                            .withStyle(on ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
