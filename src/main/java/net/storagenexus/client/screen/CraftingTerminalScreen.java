package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.terminal.TerminalMenu;

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return view.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return view.searchConsumesKey(event) || super.keyPressed(event);
    }
}
