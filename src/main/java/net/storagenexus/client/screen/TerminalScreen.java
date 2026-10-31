package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.terminal.TerminalActionPayload;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.terminal.TerminalSyncPayload;
import net.storagenexus.util.Fmt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Searchable, sortable view of everything in the network. Items and Fluids tabs.
 * Left-click takes a stack, right-click half, shift-click straight to your inventory;
 * click with items on the cursor to put them in. On the Fluids tab click with a bucket.
 * Search "@name" to filter by mod.
 */
public class TerminalScreen extends BaseScreen<TerminalMenu> {
    private enum Sort { COUNT, NAME, MOD }

    private EditBox search;
    private Button tabButton;
    private Button sortButton;
    private boolean fluidTab;
    private Sort sort = Sort.COUNT;
    private int scroll;
    private int seenRevision = -1;
    private String lastQuery = "";
    private final List<TerminalSyncPayload.ItemEntry> visibleItems = new ArrayList<>();
    private final List<TerminalSyncPayload.FluidEntry> visibleFluids = new ArrayList<>();

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TerminalMenu.WIDTH, TerminalMenu.imageHeight(menu.isCrafting()), TerminalMenu.inventoryTop(menu.isCrafting()));
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        search = new EditBox(font, leftPos + 100, topPos + 4, 54, 11, Component.translatable("screen.storagenexus.search"));
        search.setMaxLength(50);
        search.setBordered(true);
        addRenderableWidget(search);
        sortButton = addRenderableWidget(Button.builder(sortLabel(), b -> {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            b.setMessage(sortLabel());
            refilter();
        }).bounds(leftPos + 156, topPos + 3, 16, 13).build());
        tabButton = addRenderableWidget(Button.builder(tabLabel(), b -> {
            fluidTab = !fluidTab;
            scroll = 0;
            b.setMessage(tabLabel());
            refilter();
        }).bounds(leftPos + 174, topPos + 3, 16, 13).build());
    }

    private Component sortLabel() {
        return Component.literal(switch (sort) { case COUNT -> "#"; case NAME -> "A"; case MOD -> "@"; });
    }

    private Component tabLabel() {
        return Component.literal(fluidTab ? "F" : "I");
    }

    // ---- Filtering ----------------------------------------------------------------------------

    private void refilter() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        lastQuery = q;
        seenRevision = menu.clientRevision();
        visibleItems.clear();
        visibleFluids.clear();
        boolean mod = q.startsWith("@");
        String term = mod ? q.substring(1) : q;
        for (TerminalSyncPayload.ItemEntry e : menu.clientItems()) {
            String ns = BuiltInRegistries.ITEM.getKey(e.stack().getItem()).getNamespace();
            String text = mod ? ns : e.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (term.isEmpty() || text.contains(term)) visibleItems.add(e);
        }
        for (TerminalSyncPayload.FluidEntry e : menu.clientFluids()) {
            String ns = BuiltInRegistries.FLUID.getKey(e.fluid()).getNamespace();
            String text = mod ? ns : fluidName(e.fluid()).getString().toLowerCase(Locale.ROOT);
            if (term.isEmpty() || text.contains(term)) visibleFluids.add(e);
        }
        Comparator<TerminalSyncPayload.ItemEntry> ic = switch (sort) {
            case COUNT -> Comparator.comparingLong((TerminalSyncPayload.ItemEntry e) -> -e.count());
            case NAME -> Comparator.comparing(e -> e.stack().getHoverName().getString());
            case MOD -> Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.stack().getItem()).toString());
        };
        visibleItems.sort(ic);
        visibleFluids.sort(Comparator.comparingLong((TerminalSyncPayload.FluidEntry e) -> -e.amount()));
        scroll = Math.min(scroll, maxScroll());
    }

    private int entryCount() {
        return fluidTab ? visibleFluids.size() : visibleItems.size();
    }

    private int maxScroll() {
        int rows = (entryCount() + TerminalMenu.COLS - 1) / TerminalMenu.COLS;
        return Math.max(0, rows - TerminalMenu.ROWS);
    }

    private static Component fluidName(Fluid fluid) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        String q = search.getValue().toLowerCase(Locale.ROOT).trim();
        if (menu.clientRevision() != seenRevision || !q.equals(lastQuery)) refilter();
    }

    // ---- Rendering ------------------------------------------------------------------------------

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int gx = leftPos + TerminalMenu.GRID_X, gy = topPos + TerminalMenu.GRID_Y;
        for (int r = 0; r < TerminalMenu.ROWS; r++) {
            for (int c = 0; c < TerminalMenu.COLS; c++) slot(g, gx + c * 18, gy + r * 18);
        }
        // Scrollbar
        int sx = leftPos + 175, sy = gy, sh = TerminalMenu.ROWS * 18;
        g.fill(sx, sy, sx + 12, sy + sh, SLOT);
        int max = maxScroll();
        int knob = max == 0 ? 0 : (sh - 15) * scroll / max;
        g.fill(sx + 1, sy + knob, sx + 11, sy + knob + 15, max == 0 ? DARK : LIGHT);
        if (menu.isCrafting()) {
            g.text(font, "→", leftPos + 94, topPos + 154, TEXT, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int gx = leftPos + TerminalMenu.GRID_X, gy = topPos + TerminalMenu.GRID_Y;
        int start = scroll * TerminalMenu.COLS;
        int hovered = cellAt(mouseX, mouseY);
        for (int i = 0; i < TerminalMenu.COLS * TerminalMenu.ROWS; i++) {
            int idx = start + i;
            if (idx >= entryCount()) break;
            int x = gx + (i % TerminalMenu.COLS) * 18, y = gy + (i / TerminalMenu.COLS) * 18;
            if (fluidTab) {
                TerminalSyncPayload.FluidEntry e = visibleFluids.get(idx);
                renderFluidIcon(g, e.fluid(), x, y);
                String amt = Fmt.fluid(e.amount());
                g.text(font, amt, x + 17 - font.width(amt), y + 9, 0xFFFFFFFF, true);
            } else {
                TerminalSyncPayload.ItemEntry e = visibleItems.get(idx);
                g.item(e.stack(), x, y);
                g.itemDecorations(font, e.stack(), x, y, Fmt.compact(e.count()));
            }
            if (i == hovered) g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
        if (!menu.clientOnline()) {
            Component off = Component.translatable("message.storagenexus.network_offline").withStyle(ChatFormatting.RED);
            g.text(font, off, gx + 81 - font.width(off) / 2, gy + 50, 0xFFFF5555, true);
        }
        if (hovered >= 0 && start + hovered < entryCount() && menu.getCarried().isEmpty()) {
            int idx = start + hovered;
            if (fluidTab) {
                TerminalSyncPayload.FluidEntry e = visibleFluids.get(idx);
                g.setComponentTooltipForNextFrame(font, List.of(fluidName(e.fluid()),
                        Component.literal(Fmt.toMb(e.amount()) + " mB").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            } else {
                g.setTooltipForNextFrame(font, visibleItems.get(idx).stack(), mouseX, mouseY);
            }
        }
    }

    private void renderFluidIcon(GuiGraphicsExtractor g, Fluid fluid, int x, int y) {
        if (fluid.getBucket() != Items.AIR) {
            g.item(new ItemStack(fluid.getBucket()), x, y);
        } else {
            int color = 0xFF000000 | FluidVariantRendering.getColor(FluidVariant.of(fluid));
            g.fill(x + 1, y + 1, x + 15, y + 15, color);
        }
    }

    // ---- Input ----------------------------------------------------------------------------------

    /** Index of the grid cell under the mouse (0..53), or -1. */
    private int cellAt(double mx, double my) {
        int gx = leftPos + TerminalMenu.GRID_X, gy = topPos + TerminalMenu.GRID_Y;
        if (mx < gx || my < gy) return -1;
        int c = (int) (mx - gx) / 18, r = (int) (my - gy) / 18;
        if (c >= TerminalMenu.COLS || r >= TerminalMenu.ROWS) return -1;
        return r * TerminalMenu.COLS + c;
    }

    private void send(int action, ItemStack item, Fluid fluid) {
        ClientPlayNetworking.send(new TerminalActionPayload(action, item, fluid));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cell = cellAt(event.x(), event.y());
        if (cell < 0) return super.mouseClicked(event, doubleClick);
        int idx = scroll * TerminalMenu.COLS + cell;
        ItemStack carried = menu.getCarried();
        if (fluidTab) {
            if (carried.is(Items.BUCKET) && idx < visibleFluids.size()) {
                send(TerminalActionPayload.FILL_BUCKET, ItemStack.EMPTY, visibleFluids.get(idx).fluid());
            } else if (carried.getItem() instanceof BucketItem) {
                send(TerminalActionPayload.EMPTY_BUCKET, ItemStack.EMPTY, Fluids.EMPTY);
            }
            return true;
        }
        if (!carried.isEmpty()) {
            send(event.button() == 1 ? TerminalActionPayload.INSERT_ONE : TerminalActionPayload.INSERT_CARRIED, ItemStack.EMPTY, Fluids.EMPTY);
            return true;
        }
        if (idx >= visibleItems.size()) return true;
        ItemStack stack = visibleItems.get(idx).stack();
        int action = event.hasShiftDown() ? TerminalActionPayload.TAKE_TO_INVENTORY
                : event.button() == 1 ? TerminalActionPayload.TAKE_HALF : TerminalActionPayload.TAKE_STACK;
        send(action, stack, Fluids.EMPTY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search.isFocused()) {
            if (event.key() == 256) {
                onClose();
                return true;
            }
            return search.keyPressed(event) || search.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }
}
