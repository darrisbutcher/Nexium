package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.terminal.TerminalActionPayload;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.terminal.TerminalSyncPayload;
import net.storagenexus.util.Fmt;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The network grid shared by the Storage and Crafting Terminal screens: search, sort, scrolling,
 * and fluids (first) and items in one list.
 *
 * Clicks: left takes a stack, right takes one, shift-click goes straight to your inventory.
 * Holding items: left puts the whole stack in, right puts one in. An empty bucket on a fluid fills
 * it; a filled bucket is poured in with left-click, or stored as an item with right-click.
 * Search "@name" to filter by mod.
 */
final class TerminalView {
    private enum Sort { COUNT, NAME, MOD }

    /** One grid cell: a fluid or an item. */
    private record Entry(TerminalSyncPayload.@Nullable FluidEntry fluid, TerminalSyncPayload.@Nullable ItemEntry item) {}

    private final TerminalMenu menu;
    private final Font font;
    private EditBox search;
    private Button sortButton;
    private Sort sort = Sort.COUNT;
    private int scroll;
    private int seenRevision = -1;
    private String lastQuery = "";
    private final List<Entry> visible = new ArrayList<>();

    TerminalView(TerminalMenu menu, Font font) {
        this.menu = menu;
        this.font = font;
    }

    private int rows() { return menu.rows(); }

    void init(Consumer<AbstractWidget> add, int left, int top) {
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, 0, 0, 72, 11, Component.translatable("screen.storagenexus.search"));
        search.setMaxLength(50);
        search.setBordered(true);
        search.setValue(query);
        add.accept(search);
        sortButton = Button.builder(sortLabel(), b -> {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            b.setMessage(sortLabel());
            refilter();
        }).bounds(0, 0, 16, 13).build();
        add.accept(sortButton);
        reposition(left, top);
        refilter();
    }

    /** Called when the screen moves (e.g. the recipe book opens). */
    void reposition(int left, int top) {
        search.setPosition(left + 100, top + 4);
        sortButton.setPosition(left + 174, top + 3);
    }

    private Component sortLabel() {
        return Component.literal(switch (sort) { case COUNT -> "#"; case NAME -> "A"; case MOD -> "@"; });
    }

    // ---- Filtering ----------------------------------------------------------------------------

    private void refilter() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        lastQuery = q;
        seenRevision = menu.clientRevision();
        boolean mod = q.startsWith("@");
        String term = mod ? q.substring(1) : q;

        List<TerminalSyncPayload.FluidEntry> fluids = new ArrayList<>();
        for (TerminalSyncPayload.FluidEntry e : menu.clientFluids()) {
            String text = mod ? BuiltInRegistries.FLUID.getKey(e.fluid()).getNamespace()
                    : fluidName(e.fluid()).getString().toLowerCase(Locale.ROOT);
            if (term.isEmpty() || text.contains(term)) fluids.add(e);
        }
        List<TerminalSyncPayload.ItemEntry> items = new ArrayList<>();
        for (TerminalSyncPayload.ItemEntry e : menu.clientItems()) {
            String text = mod ? BuiltInRegistries.ITEM.getKey(e.stack().getItem()).getNamespace()
                    : e.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (term.isEmpty() || text.contains(term)) items.add(e);
        }
        fluids.sort(Comparator.comparingLong((TerminalSyncPayload.FluidEntry e) -> -e.amount()));
        items.sort(switch (sort) {
            case COUNT -> Comparator.comparingLong((TerminalSyncPayload.ItemEntry e) -> -e.count());
            case NAME -> Comparator.comparing(e -> e.stack().getHoverName().getString());
            case MOD -> Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.stack().getItem()).toString());
        });
        visible.clear();
        for (TerminalSyncPayload.FluidEntry f : fluids) visible.add(new Entry(f, null));
        for (TerminalSyncPayload.ItemEntry i : items) visible.add(new Entry(null, i));
        scroll = Math.min(scroll, maxScroll());
    }

    private int maxScroll() {
        int rowsNeeded = (visible.size() + TerminalMenu.COLS - 1) / TerminalMenu.COLS;
        return Math.max(0, rowsNeeded - rows());
    }

    private static Component fluidName(Fluid fluid) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }

    void tick() {
        String q = search.getValue().toLowerCase(Locale.ROOT).trim();
        if (menu.clientRevision() != seenRevision || !q.equals(lastQuery)) refilter();
    }

    // ---- Rendering ------------------------------------------------------------------------------

    /** Cell frames and the scrollbar. */
    void extractBackground(GuiGraphicsExtractor g, int left, int top) {
        int gx = left + TerminalMenu.GRID_X, gy = top + TerminalMenu.GRID_Y;
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < TerminalMenu.COLS; c++) BaseScreen.slot(g, gx + c * 18, gy + r * 18);
        }
        int sx = left + 175, sh = rows() * 18;
        g.fill(sx, gy, sx + 12, gy + sh, BaseScreen.SLOT);
        int max = maxScroll();
        int knob = max == 0 ? 0 : (sh - 15) * scroll / max;
        g.fill(sx + 1, gy + knob, sx + 11, gy + knob + 15, max == 0 ? BaseScreen.DARK : BaseScreen.LIGHT);
    }

    /** The grid's contents, hover highlight and tooltips. */
    void extractGrid(GuiGraphicsExtractor g, int left, int top, int mouseX, int mouseY) {
        int gx = left + TerminalMenu.GRID_X, gy = top + TerminalMenu.GRID_Y;
        int start = scroll * TerminalMenu.COLS;
        int hovered = cellAt(left, top, mouseX, mouseY);
        for (int i = 0; i < TerminalMenu.COLS * rows(); i++) {
            int idx = start + i;
            if (idx >= visible.size()) break;
            int x = gx + (i % TerminalMenu.COLS) * 18, y = gy + (i / TerminalMenu.COLS) * 18;
            Entry e = visible.get(idx);
            if (e.fluid() != null) {
                fluidIcon(g, e.fluid().fluid(), x, y);
                String amt = Fmt.fluid(e.fluid().amount());
                g.text(font, amt, x + 17 - font.width(amt), y + 9, 0xFFFFFFFF, true);
            } else if (e.item() != null) {
                g.item(e.item().stack(), x, y);
                g.itemDecorations(font, e.item().stack(), x, y, Fmt.compact(e.item().count()));
            }
            if (i == hovered) g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
        if (!menu.clientOnline()) {
            Component off = Component.translatable("message.storagenexus.network_offline").withStyle(ChatFormatting.RED);
            g.text(font, off, gx + 81 - font.width(off) / 2, gy + rows() * 9 - 4, 0xFFFF5555, true);
        }
        Entry e = hovered < 0 ? null : entryAt(start + hovered);
        if (e != null && menu.getCarried().isEmpty()) {
            if (e.fluid() != null) {
                g.setComponentTooltipForNextFrame(font, List.of(fluidName(e.fluid().fluid()),
                        Component.literal(Fmt.toMb(e.fluid().amount()) + " mB").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            } else if (e.item() != null) {
                g.setTooltipForNextFrame(font, e.item().stack(), mouseX, mouseY);
            }
        }
    }

    private void fluidIcon(GuiGraphicsExtractor g, Fluid fluid, int x, int y) {
        if (fluid.getBucket() != Items.AIR) {
            g.item(new ItemStack(fluid.getBucket()), x, y);
        } else {
            int color = 0xFF000000 | FluidVariantRendering.getColor(FluidVariant.of(fluid));
            g.fill(x + 1, y + 1, x + 15, y + 15, color);
        }
    }

    // ---- Input ----------------------------------------------------------------------------------

    private @Nullable Entry entryAt(int idx) {
        return idx >= 0 && idx < visible.size() ? visible.get(idx) : null;
    }

    /** Index of the grid cell under the mouse, or -1. */
    private int cellAt(int left, int top, double mx, double my) {
        int gx = left + TerminalMenu.GRID_X, gy = top + TerminalMenu.GRID_Y;
        if (mx < gx || my < gy) return -1;
        int c = (int) (mx - gx) / 18, r = (int) (my - gy) / 18;
        if (c >= TerminalMenu.COLS || r >= rows()) return -1;
        return r * TerminalMenu.COLS + c;
    }

    private static void send(int action, ItemStack item, Fluid fluid) {
        ClientPlayNetworking.send(new TerminalActionPayload(action, item, fluid));
    }

    /** True if the click was in the grid (and handled). */
    boolean mouseClicked(MouseButtonEvent event, int left, int top) {
        int cell = cellAt(left, top, event.x(), event.y());
        if (cell < 0) return false;
        Entry e = entryAt(scroll * TerminalMenu.COLS + cell);
        boolean right = event.button() == 1;
        ItemStack carried = menu.getCarried();

        if (!carried.isEmpty()) {
            if (carried.is(Items.BUCKET) && e != null && e.fluid() != null) {
                send(TerminalActionPayload.FILL_BUCKET, ItemStack.EMPTY, e.fluid().fluid());
            } else if (!right && carried.getItem() instanceof BucketItem && !carried.is(Items.BUCKET)) {
                send(TerminalActionPayload.EMPTY_BUCKET, ItemStack.EMPTY, Fluids.EMPTY);
            } else {
                send(right ? TerminalActionPayload.INSERT_ONE : TerminalActionPayload.INSERT_CARRIED, ItemStack.EMPTY, Fluids.EMPTY);
            }
            return true;
        }
        if (e == null || e.item() == null) return true;
        ItemStack stack = e.item().stack();
        int action = event.hasShiftDown() ? TerminalActionPayload.TAKE_TO_INVENTORY
                : right ? TerminalActionPayload.TAKE_ONE : TerminalActionPayload.TAKE_STACK;
        send(action, stack, Fluids.EMPTY);
        return true;
    }

    boolean mouseScrolled(double scrollY) {
        if (scrollY == 0) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    /** Lets the search box keep typed keys (e.g. "e") from closing the screen. */
    boolean searchConsumesKey(KeyEvent event) {
        if (!search.isFocused()) return false;
        if (event.key() == 256) return false; // Escape still closes
        return search.keyPressed(event) || search.canConsumeInput();
    }
}
