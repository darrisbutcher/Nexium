package net.storagenexus.client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.guide.GuideData;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a recipe card: the crafting (or smithing) grid, an arrow, the result, and the recipe's name beside the result. An
 * ingredient that is a tag (any planks, any drawer) cycles through its members once a second.
 */
final class RecipePainter {
    private static final int CELL = 17;
    /** The grid, arrow and result, before the name. */
    static final int GRID_WIDTH = 3 * CELL + 3 + 12;
    /** Space one card takes down the page. */
    static final int CARD_HEIGHT = 63;
    private static final int FRAME = 0xFF373737, SLOT = 0xFF8B8B8B;

    /**
     * Draws one card with its top-left corner at x, y; the name wraps in {@code nameWidth} beside the result. Returns the
     * item under the mouse (an ingredient or the result), or empty. Its tooltip is already queued.
     */
    static ItemStack card(GuiGraphicsExtractor g, Font font, GuideData.Recipe recipe, int x, int y, int nameWidth,
                          int ink, int noteInk, int mouseX, int mouseY) {
        int rows = recipe.rows();
        int gridTop = y + (3 - rows) * CELL / 2;
        g.fill(x - 1, gridTop - 1, x + 3 * CELL + 1, gridTop + rows * CELL + 1, FRAME);
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < recipe.cells().size(); i++) {
            int cellX = x + (i % 3) * CELL, cellY = gridTop + (i / 3) * CELL;
            g.fill(cellX, cellY, cellX + 16, cellY + 16, SLOT);
            String ingredient = recipe.cells().get(i);
            if (ingredient == null) continue;
            ItemStack stack = stackOf(ingredient, 1);
            g.item(stack, cellX, cellY);
            if (over(mouseX, mouseY, cellX, cellY)) hovered = stack;
        }

        // The arrow, level with the middle of the grid
        int arrowX = x + 3 * CELL + 3;
        int middle = gridTop + rows * CELL / 2;
        g.fill(arrowX, middle - 1, arrowX + 5, middle + 1, FRAME | 0xFF888888);
        g.fill(arrowX + 5, middle - 3, arrowX + 6, middle + 3, FRAME | 0xFF888888);
        g.fill(arrowX + 6, middle - 2, arrowX + 7, middle + 2, FRAME | 0xFF888888);
        g.fill(arrowX + 7, middle - 1, arrowX + 8, middle + 1, FRAME | 0xFF888888);

        int resultX = arrowX + 12, resultY = gridTop + (rows - 1) * CELL / 2;
        g.fill(resultX - 1, resultY - 1, resultX + 17, resultY + 17, FRAME);
        g.fill(resultX, resultY, resultX + 16, resultY + 16, SLOT);
        ItemStack result = stackOf(recipe.result(), recipe.count());
        g.item(result, resultX, resultY);
        g.itemDecorations(font, result, resultX, resultY);
        if (over(mouseX, mouseY, resultX, resultY)) hovered = result;

        // The name beside the result, the note under the grid
        int nameX = resultX + 22, nameY = y + 2;
        for (FormattedCharSequence line : font.split(Component.translatable(recipe.nameKey()), nameWidth)) {
            g.text(font, line, nameX, nameY, ink, true);
            nameY += 10;
        }
        if (recipe.note() != null) {
            g.text(font, Component.translatable(recipe.noteKey()), x, gridTop + rows * CELL + 3, noteInk, false);
        }
        if (!hovered.isEmpty()) g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        return hovered;
    }

    private static boolean over(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    /** The item for an id, or for a tag the member whose turn it is. A missing item shows as a barrier. */
    static ItemStack stackOf(String ingredient, int count) {
        if (ingredient.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(ingredient.substring(1)));
            List<Holder<Item>> members = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(members::add);
            if (members.isEmpty()) return new ItemStack(Items.BARRIER);
            return new ItemStack(members.get((int) (Util.getMillis() / 1000 % members.size())), count);
        }
        Identifier id = Identifier.parse(ingredient);
        return BuiltInRegistries.ITEM.containsKey(id) ? new ItemStack(BuiltInRegistries.ITEM.getValue(id), count) : new ItemStack(Items.BARRIER);
    }

    private RecipePainter() {}
}
