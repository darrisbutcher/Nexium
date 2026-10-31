package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
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

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        view.init(this::addRenderableWidget, leftPos, topPos);
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        // Left of the crafting grid, level with its middle row
        return new ScreenPosition(leftPos + 6, topPos + TerminalMenu.craftingTop(menu.rows()) + 18);
    }

    @Override
    protected void onRecipeBookButtonClick() {
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
        BaseScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) BaseScreen.slot(g, leftPos + s.x, topPos + s.y);
        view.extractBackground(g, leftPos, topPos);
        g.text(font, "→", leftPos + 94, topPos + TerminalMenu.craftingTop(menu.rows()) + 22, BaseScreen.TEXT, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractContents(g, mouseX, mouseY, partialTick);
        view.extractGrid(g, leftPos, topPos, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
