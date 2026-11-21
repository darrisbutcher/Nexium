package net.nexium.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.backpack.BackpackFeeding;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.essentials.backpack.BackpackModuleItem;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.client.NexiumEssentialsClient;

/**
 * A Backpack's screen: its 36 slots top left, the player's inventory below, and on the right its module slots, each with
 * a button that switches that module on or off. Torch counts above 99 show in full while the Torch Stack module is on.
 * The Backpack's own slot in the inventory is outlined when it was opened from there: it can't be moved while the
 * screen is open. The Open Backpack key closes the screen again.
 *
 * <p>With a Crafting module installed and on, a panel on the right of the screen holds a 3x3 crafting grid and its result slot, and the
 * screen is centred as a whole. The trash slot under the module column is always there; it is drawn as a small bin while it is empty.
 *
 * <p>The crafting panel has the vanilla recipe book's button (under its grid); the book opens at the left of the screen and slides the
 * screen to the right, as the Crafting Terminal's does, and the recipes it lists are those the Backpack's slots and your inventory can make.
 * When the screen is too narrow for the book beside it (the book, a small gap and the whole Backpack screen), the open book covers
 * the screen instead, as in vanilla.
 *
 * <p>With a Feeding module installed, a small panel under the module column holds its two settings: the hunger level it eats at
 * (a click steps it lower, shift-click higher) and whether it may waste food.
 */
public class BackpackScreen extends AbstractRecipeBookScreen<BackpackMenu> {
    /** RecipeBookComponent's own offset left of centre when there is room, and the gap kept between the book and the screen. */
    private static final int BOOK_X_OFFSET = 86, BOOK_GAP = 4;
    private static final int TOGGLE_X = BackpackMenu.MODULE_X + 20, TOGGLE_WIDTH = 48;
    /** The crafting panel's height: its title, the grid, and a row under it for the recipe book's button. */
    private static final int SIDE_H = BackpackMenu.CRAFT_Y + 3 * 18 + 4 + 18 + 6;
    /** The Feeding panel: its heading, then its level button and its waste button, under the last module row. */
    private static final int FEEDING_Y = BackpackMenu.MODULE_Y + BackpackModules.SLOTS * BackpackMenu.MODULE_ROW + 4, FEEDING_WIDTH = 68;
    private final Button[] toggles = new Button[BackpackModules.SLOTS];
    private Button feedingLevel, feedingWaste;
    private boolean hasFeeding;
    /** Whether the crafting panel is showing (the screen is centred with it, and re-centred when it comes or goes). */
    private boolean craftingShown;
    private final SideHints hints = new SideHints();
    private final CraftingRecipeBookComponent recipeBook;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        this(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    private BackpackScreen(BackpackMenu menu, CraftingRecipeBookComponent recipeBook, Inventory inventory, Component title) {
        super(menu, recipeBook, inventory, title);
        this.recipeBook = recipeBook;
        // Widened in nexium.accesswidener: the recipe-book screen fixes the size at 176x166
        this.imageWidth = BackpackMenu.WIDTH;
        this.imageHeight = BackpackMenu.HEIGHT;
        this.inventoryLabelY = BackpackMenu.INVENTORY_Y - 11;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    /** The colours this screen is drawn in: all mod screens match vanilla's dark inventory. */
    private Palette palette() {
        return Palette.DARK;
    }

    /** The whole screen's width: the main panel and, while the Crafting module is on, the crafting panel beside it. */
    private int totalWidth() {
        return BackpackMenu.WIDTH + (craftingShown ? BackpackMenu.SIDE_W : 0);
    }

    /** Where the open recipe book ends. Vanilla puts it left of where a 176-wide screen would be centred, so this is measured from the middle. */
    private int bookRight() {
        return (width - RecipeBookComponent.IMAGE_WIDTH) / 2 - BOOK_X_OFFSET + RecipeBookComponent.IMAGE_WIDTH;
    }

    /** Where the panel goes: a small gap right of the open recipe book, else centred (with the crafting panel). */
    private int panelLeft() {
        if (recipeBook.isVisible() && !widthTooNarrow) return bookRight() + BOOK_GAP;
        return (width - totalWidth()) / 2;
    }

    /** The vanilla recipe book button, while there is one. */
    private ImageButton recipeBookButton() {
        for (var child : children()) if (child instanceof ImageButton b) return b;
        return null;
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        // Under the crafting grid, at its left
        return new ScreenPosition(panelLeft() + BackpackMenu.CRAFT_X, topPos + BackpackMenu.CRAFT_Y + 3 * 18 + 4);
    }

    @Override
    protected void onRecipeBookButtonClick() {
        leftPos = panelLeft(); // the screen slides right when the book opens
        placeWidgets();
    }

    /** The recipe book's button is only there while the grid is: without a Crafting module the vanilla button is taken off. */
    private void dropRecipeBookButtonWithoutGrid() {
        if (craftingShown) return;
        for (var child : new java.util.ArrayList<>(children())) if (child instanceof ImageButton) removeWidget(child);
        if (recipeBook.isVisible()) recipeBook.toggleVisibility();
    }

    /** Puts the module and Feeding buttons where they go for the current {@code leftPos}. */
    private void placeWidgets() {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            toggles[i].setPosition(leftPos + TOGGLE_X, topPos + BackpackMenu.MODULE_Y + i * BackpackMenu.MODULE_ROW);
        }
        feedingLevel.setPosition(leftPos + BackpackMenu.MODULE_X, topPos + FEEDING_Y + 13);
        feedingWaste.setPosition(leftPos + BackpackMenu.MODULE_X, topPos + FEEDING_Y + 31);
    }

    @Override
    protected void init() {
        craftingShown = menu.craftingActive();
        super.init();
        // Vanilla's rule for "too narrow" is for a screen 176 wide: this one is wider, so the book covers it when the whole screen
        // would not fit to its right
        boolean tooNarrow = bookRight() + BOOK_GAP + totalWidth() > width;
        if (tooNarrow != widthTooNarrow) {
            widthTooNarrow = tooNarrow;
            recipeBook.init(width, height, minecraft, widthTooNarrow);
        }
        // Centred with the crafting panel, which sits to its right (or right of the open recipe book)
        leftPos = panelLeft();
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            final int slot = i;
            toggles[i] = addRenderableWidget(Button.builder(Component.empty(), b -> click(BackpackMenu.BTN_TOGGLE_MODULE + slot))
                    .bounds(0, 0, TOGGLE_WIDTH, 16).build());
        }
        feedingLevel = addRenderableWidget(Button.builder(Component.empty(),
                        b -> click(minecraft != null && minecraft.hasShiftDown() ? BackpackMenu.BTN_FEEDING_LEVEL_BACK : BackpackMenu.BTN_FEEDING_LEVEL))
                .bounds(0, 0, FEEDING_WIDTH, 16).build());
        feedingWaste = addRenderableWidget(Button.builder(Component.empty(), b -> click(BackpackMenu.BTN_FEEDING_WASTE))
                .bounds(0, 0, FEEDING_WIDTH, 16).build());
        placeWidgets();
        dropRecipeBookButtonWithoutGrid();
        refresh();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // The Crafting module came or went: the panel shows or hides, and the screen is centred again
        if (menu.craftingActive() != craftingShown) rebuildWidgets();
        refresh();
    }

    /** A click in the crafting panel is not a click outside the screen (which would throw away what you carry). */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int xo, int yo) {
        if (craftingShown && mouseX >= xo + BackpackMenu.SIDE_X && mouseX < xo + BackpackMenu.SIDE_X + BackpackMenu.SIDE_W
                && mouseY >= yo && mouseY < yo + SIDE_H) return false;
        return super.hasClickedOutside(mouseX, mouseY, xo, yo);
    }

    /** Title and inventory labels in the palette's text colour. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, palette().text(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, palette().text(), false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        palette().panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) if (s.isActive()) palette().slot(g, leftPos + s.x, topPos + s.y);
        renderExtraBg(g, mouseX, mouseY);
    }

    /** Whether the recipe book is open (for tests). */
    public boolean recipeBookVisible() {
        return recipeBook.isVisible();
    }

    /** The centre of the recipe book's button, in screen coordinates (for tests), or null while there is none. */
    public int[] recipeBookButtonCentre() {
        ImageButton b = recipeBookButton();
        return b == null ? null : new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
    }

    /** Whether the open recipe book covers the screen (too narrow for it to sit beside it). */
    private boolean bookCoversScreen() {
        return widthTooNarrow && recipeBook.isVisible();
    }

    /**
     * Vanilla draws only the book while it covers the screen, and answers every click outside it itself, so the book's own button
     * could not be pressed to close it: it is drawn on top and let through here.
     */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (bookCoversScreen() && craftingShown) {
            ImageButton b = recipeBookButton();
            if (b != null && b.isMouseOver(event.x(), event.y()) && b.mouseClicked(event, doubleClick)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The centre of a slot of this menu, in screen coordinates (for tests, which click it with the real mouse). */
    public int[] slotCentre(net.minecraft.world.inventory.Slot slot) {
        return new int[]{leftPos + slot.x + 8, topPos + slot.y + 8};
    }

    /**
     * The crafting panel's area, {x, y, width, height} in screen coordinates, or null when it is not showing. It sits outside the
     * screen's own size, to its right, so JEI and REI are told about it (their item lists move aside).
     */
    public int[] craftingPanelBounds() {
        return craftingShown ? new int[]{leftPos + BackpackMenu.SIDE_X - 2, topPos, BackpackMenu.SIDE_W + 2, SIDE_H} : null;
    }

    /** Whether the crafting panel is showing (for tests). */
    public boolean craftingShown() {
        return craftingShown;
    }

    private void refresh() {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            Button button = toggles[i];
            ItemStack module = menu.getSlot(i).getItem();
            if (!(module.getItem() instanceof BackpackModuleItem item)) {
                button.active = false;
                button.setMessage(Component.translatable("screen.nexium.backpack.module_empty").withStyle(ChatFormatting.DARK_GRAY));
                hints.set(button, Component.translatable("screen.nexium.backpack.module_empty.tooltip"));
                continue;
            }
            boolean on = !module.has(net.nexium.registry.ModComponents.MODULE_OFF);
            button.active = true;
            button.setMessage(Component.translatable(on ? "screen.nexium.backpack.module_on" : "screen.nexium.backpack.module_off")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
            hints.set(button, Component.empty()
                    .append(Component.translatable("item.nexium." + item.type().id() + "_module.desc")).append("\n")
                    .append(Component.translatable("screen.nexium.backpack.module_click").withStyle(ChatFormatting.GRAY)));
        }
        refreshFeeding();
    }

    /** The Feeding panel shows while a Feeding module is installed, and reads its settings. */
    private void refreshFeeding() {
        ItemStack feeding = ItemStack.EMPTY;
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            ItemStack module = menu.getSlot(i).getItem();
            if (module.getItem() instanceof BackpackModuleItem item && item.type() == BackpackModuleType.FEEDING) feeding = module;
        }
        hasFeeding = !feeding.isEmpty();
        feedingLevel.visible = feedingWaste.visible = hasFeeding;
        feedingLevel.active = feedingWaste.active = hasFeeding;
        if (!hasFeeding) return;
        int level = BackpackFeeding.level(feeding);
        boolean waste = BackpackFeeding.allowsWaste(feeding);
        feedingLevel.setMessage(Component.translatable("screen.nexium.backpack.feeding_level", level));
        hints.set(feedingLevel, Component.empty()
                .append(Component.translatable("screen.nexium.backpack.feeding_level.tip", level, 20 - level)).append("\n")
                .append(Component.translatable("screen.nexium.backpack.feeding_level.click").withStyle(ChatFormatting.GRAY)));
        feedingWaste.setMessage(Component.translatable(waste ? "screen.nexium.backpack.feeding_waste.yes" : "screen.nexium.backpack.feeding_waste.no")
                .withStyle(waste ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        hints.set(feedingWaste, Component.empty()
                .append(Component.translatable(waste ? "screen.nexium.backpack.feeding_waste.yes.tip" : "screen.nexium.backpack.feeding_waste.no.tip")).append("\n")
                .append(Component.translatable("screen.nexium.backpack.feeding_waste.click").withStyle(ChatFormatting.GRAY)));
    }

    /** The centre of the Feeding level button, in screen coordinates (for tests, which click it with the real mouse). */
    public int[] feedingLevelCentre() {
        return new int[]{feedingLevel.getX() + feedingLevel.getWidth() / 2, feedingLevel.getY() + feedingLevel.getHeight() / 2};
    }

    /** The centre of the Feeding waste button, in screen coordinates (for tests). */
    public int[] feedingWasteCentre() {
        return new int[]{feedingWaste.getX() + feedingWaste.getWidth() / 2, feedingWaste.getY() + feedingWaste.getHeight() / 2};
    }

    /** Whether the Feeding panel is showing (for tests). */
    public boolean feedingPanelShown() {
        return hasFeeding && feedingLevel.visible && feedingWaste.visible;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (bookCoversScreen() && craftingShown) {
            ImageButton b = recipeBookButton();
            if (b != null) b.extractRenderState(g, mouseX, mouseY, partialTick);
        }
        hints.render(g, leftPos, leftPos + imageWidth + (craftingShown ? BackpackMenu.SIDE_W : 0), mouseX, mouseY);
        // The trash slot says what it is, empty or not
        if (hoveredSlot == menu.trashSlot() && menu.getCarried().isEmpty()) {
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable("screen.nexium.backpack.trash").getVisualOrderText());
            lines.addAll(font.split(Component.translatable("screen.nexium.backpack.trash.tip").withStyle(ChatFormatting.GRAY), 160));
            g.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.nexium.backpack.modules"), leftPos + BackpackMenu.MODULE_X, topPos + 6, palette().text(), false);
        if (hasFeeding) {
            g.text(font, Component.translatable("screen.nexium.backpack.feeding"), leftPos + BackpackMenu.MODULE_X, topPos + FEEDING_Y, palette().text(), false);
        }
        trashIcon(g);
        if (craftingShown) {
            // The crafting panel, joined to the right of the main one: its title, the grid (the slots draw themselves), an arrow and the result
            palette().panel(g, leftPos + BackpackMenu.SIDE_X - 2, topPos, BackpackMenu.SIDE_W + 2, SIDE_H);
            for (var slot : menu.getInputGridSlots()) palette().slot(g, leftPos + slot.x, topPos + slot.y);
            palette().slot(g, leftPos + menu.getResultSlot().x, topPos + menu.getResultSlot().y);
            g.text(font, Component.translatable("screen.nexium.backpack.crafting"), leftPos + BackpackMenu.SIDE_X + 8, topPos + 6, palette().text(), false);
            g.text(font, "\u2192", leftPos + BackpackMenu.SIDE_X + 66, topPos + BackpackMenu.CRAFT_Y + 22, palette().text(), false);
        }
        // Outline the slot the Backpack was opened from: it stays put while the screen is open
        int source = menu.source();
        if (source >= 0 && source < 36) {
            int x = leftPos + BackpackMenu.GRID_X + (source >= 9 ? (source - 9) % 9 : source) * 18;
            int y = topPos + BackpackMenu.INVENTORY_Y + (source >= 9 ? (source - 9) / 9 * 18 : 58);
            int c = 0xFFB04CFF;
            g.fill(x - 1, y - 1, x + 17, y, c);
            g.fill(x - 1, y + 16, x + 17, y + 17, c);
            g.fill(x - 1, y, x, y + 16, c);
            g.fill(x + 16, y, x + 17, y + 16, c);
        }
    }

    /** A small bin in the trash slot while it is empty, so you can tell what the slot is. */
    private void trashIcon(GuiGraphicsExtractor g) {
        var slot = menu.trashSlot();
        if (slot.hasItem()) return;
        int x = leftPos + slot.x, y = topPos + slot.y;
        int body = 0xFF8A8A98, line = 0xFF55555F, rim = 0xFFA6A6B4;
        g.fill(x + 6, y + 1, x + 10, y + 2, rim); // the handle
        g.fill(x + 2, y + 2, x + 14, y + 4, rim); // the lid
        g.fill(x + 3, y + 4, x + 13, y + 14, body); // the bin
        g.fill(x + 4, y + 14, x + 12, y + 15, body);
        for (int stripe : new int[]{6, 8, 10}) g.fill(x + stripe, y + 6, x + stripe + 1, y + 13, line);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // The same key closes it again, except while it is being typed into the recipe book's search
        EditBox bookSearch = recipeBook.searchBox;
        boolean typing = bookSearch != null && recipeBook.isVisible() && bookSearch.isFocused();
        if (!typing && NexiumEssentialsClient.OPEN_BACKPACK.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * A count of 1,000 or more is four digits, wider than a slot: it is drawn smaller so it stays inside, like the three-digit ones.
     * (Every other slot is drawn as usual.)
     */
    @Override
    protected void extractSlot(GuiGraphicsExtractor g, net.minecraft.world.inventory.Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        if (!menu.isStorageSlot(slot) || stack.getCount() < 1000) {
            super.extractSlot(g, slot, mouseX, mouseY);
            return;
        }
        int x = slot.x, y = slot.y;
        g.item(stack, x, y, x + y * imageWidth);
        g.itemDecorations(font, stack, x, y, "");
        String count = Integer.toString(stack.getCount());
        float scale = 0.7F;
        g.pose().pushMatrix();
        g.pose().translate(x + 17 - font.width(count) * scale, y + 10);
        g.pose().scale(scale, scale);
        g.text(font, Component.literal(count), 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }
}
