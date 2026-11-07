package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.util.Fmt;

/**
 * Crafting Terminal: the network grid, a 3x3 crafting grid with the vanilla recipe book, and your
 * inventory. Recipes the network has ingredients for show as craftable, and clicking one pulls the
 * ingredients from the network. See {@link TerminalView} for the grid controls.
 */
public class CraftingTerminalScreen extends AbstractRecipeBookScreen<TerminalMenu> {
    /** Below this width the open recipe book covers the whole screen (same rule as vanilla). */
    private static final int NARROW_WIDTH = 379;
    /** RecipeBookComponent's own offset left of centre when there is room. */
    private static final int BOOK_X_OFFSET = 86;
    private static final int BOOK_GAP = 4;

    private final TerminalView view;
    private final CraftingRecipeBookComponent recipeBook;
    private Button clearButton;

    public CraftingTerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        this(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    private CraftingTerminalScreen(TerminalMenu menu, CraftingRecipeBookComponent recipeBook, Inventory inventory, Component title) {
        super(menu, recipeBook, inventory, title);
        this.recipeBook = recipeBook;
        // Widened in storagenexus.accesswidener: the recipe-book screen fixes the size at 176x166.
        this.imageWidth = TerminalMenu.WIDTH;
        this.imageHeight = TerminalMenu.imageHeight(true, menu.rows());
        this.inventoryLabelY = TerminalMenu.inventoryTop(true, menu.rows()) - 11;
        this.view = new TerminalView(menu, this.font);
    }

    /** Where the panel goes: a small gap right of the open recipe book, else centred. */
    private int panelLeft() {
        if (recipeBook.isVisible() && width >= NARROW_WIDTH) {
            // Vanilla's placement assumes a 176-wide screen; ours is wider, so measure from the book.
            int bookRight = (width - RecipeBookComponent.IMAGE_WIDTH) / 2 - BOOK_X_OFFSET + RecipeBookComponent.IMAGE_WIDTH;
            return bookRight + BOOK_GAP;
        }
        return (width - imageWidth) / 2;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = panelLeft();
        titleLabelY = 6;
        view.init(this::addRenderableWidget, leftPos, topPos);
        clearButton = Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TerminalMenu.CLEAR_GRID);
        }).bounds(leftPos + 8, topPos + TerminalMenu.craftingTop(menu.rows()), 16, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.clear_grid"))).build();
        addRenderableWidget(clearButton);
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        // Left of the crafting grid, level with its middle row
        return new ScreenPosition(panelLeft() + 6, topPos + TerminalMenu.craftingTop(menu.rows()) + 18);
    }

    @Override
    protected void onRecipeBookButtonClick() {
        leftPos = panelLeft();
        view.reposition(leftPos, topPos); // the screen slides right when the book opens
        clearButton.setPosition(leftPos + 8, topPos + TerminalMenu.craftingTop(menu.rows()));
    }

    /** The terminal's search text (for the client test). */
    public String searchText() {
        return view.searchText();
    }

    /** Types into the terminal's search (for the client test). */
    public void setSearchText(String text) {
        view.setSearchText(text);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        view.tick();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Palette.DARK.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) Palette.DARK.slot(g, leftPos + s.x, topPos + s.y);
        view.extractBackground(g, leftPos, topPos);
        g.text(font, "→", leftPos + 94, topPos + TerminalMenu.craftingTop(menu.rows()) + 22, Palette.DARK.text(), false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, Palette.DARK.text(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Palette.DARK.text(), false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractContents(g, mouseX, mouseY, partialTick);
        view.extractGrid(g, leftPos, topPos, mouseX, mouseY);
        extractUsage(g, mouseX, mouseY);
    }

    // ---- Capacity: items above the result slot, fluids below it --------------------------------

    private static final float USAGE_SCALE = 0.75f;

    private void extractUsage(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StorageNetwork.Usage u = menu.clientUsage();
        int x = leftPos + 98, right = leftPos + imageWidth - 8;
        int top = topPos + TerminalMenu.craftingTop(menu.rows());
        long bucket = FluidConstants.BUCKET;
        usageBlock(g, mouseX, mouseY, x, right, top + 1, "screen.storagenexus.usage_items",
                Fmt.compact(u.items()) + "/" + Fmt.compact(u.itemCapacity()), u.items(), u.itemCapacity(),
                Component.translatable("screen.storagenexus.usage_items_tip", exact(u.items()), exact(u.itemCapacity()), percent(u.items(), u.itemCapacity())));
        usageBlock(g, mouseX, mouseY, x, right, top + 39, "screen.storagenexus.usage_fluids",
                Fmt.compact(u.fluid() / bucket) + "/" + Fmt.compact(u.fluidCapacity() / bucket) + " B", u.fluid(), u.fluidCapacity(),
                Component.translatable("screen.storagenexus.usage_fluids_tip", exact(u.fluid() / bucket), exact(u.fluidCapacity() / bucket),
                        percent(u.fluid(), u.fluidCapacity())));
    }

    /** A small label with used/total and a fill bar under it; hovering shows the exact numbers. */
    private void usageBlock(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int right, int y, String labelKey,
                            String numbers, long used, long capacity, Component tooltip) {
        Component text = Component.translatable(labelKey).append(" " + numbers);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(USAGE_SCALE, USAGE_SCALE);
        g.text(font, text, 0, 0, Palette.DARK.text(), false);
        g.pose().popMatrix();
        int barY = y + 8, width = right - x;
        g.fill(x, barY, right, barY + 4, Palette.DARK.slotDark());
        double fraction = capacity <= 0 ? 0 : Math.min(1.0, (double) used / capacity);
        int fill = (int) Math.round(width * fraction);
        int color = fraction < 0.75 ? 0xFF4CAF50 : fraction < 0.9 ? 0xFFE0C040 : 0xFFE05050;
        if (fill > 0) g.fill(x, barY, x + fill, barY + 4, color);
        if (mouseX >= x && mouseX < right && mouseY >= y && mouseY < barY + 4) {
            g.setComponentTooltipForNextFrame(font, java.util.List.of(tooltip), mouseX, mouseY);
        }
    }

    private static String exact(long n) {
        return String.format(java.util.Locale.ROOT, "%,d", n);
    }

    private static String percent(long used, long capacity) {
        return capacity <= 0 ? "0" : Long.toString(Math.min(100, used * 100 / capacity));
    }

    /**
     * Same sequence as AbstractRecipeBookScreen, which calls super.extractContents directly and so
     * would skip the network grid drawn in our extractContents.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (recipeBook.isVisible() && width < NARROW_WIDTH) {
            extractBackground(g, mouseX, mouseY, partialTick);
        } else {
            extractContents(g, mouseX, mouseY, partialTick);
        }
        g.nextStratum();
        recipeBook.extractRenderState(g, mouseX, mouseY, partialTick);
        g.nextStratum();
        extractCarriedItem(g, mouseX, mouseY);
        extractTooltip(g, mouseX, mouseY);
        recipeBook.extractTooltip(g, mouseX, mouseY, hoveredSlot);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (view.blurSearchIfClickedOutside(event)) setFocused(null);
        boolean bookCoversScreen = recipeBook.isVisible() && width < NARROW_WIDTH;
        if (!bookCoversScreen && view.mouseClicked(event, leftPos, topPos)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return view.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return view.mouseDragged() || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return view.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return view.searchConsumesKey(event) || super.keyPressed(event);
    }
}
