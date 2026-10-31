package net.storagenexus.drawer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Works out compacting chains from the world's crafting recipes, so modded
 * nuggets/ingots/blocks work too. A chain step is only accepted if it is reversible
 * (9 nuggets -> 1 ingot AND 1 ingot -> 9 nuggets).
 */
public final class CompactingRecipes {
    public record Tier(Item item, int factorToNextSmaller) {}

    private record Step(Item item, int factor) {}

    /** Largest form first. factorToNextSmaller is 1 for the smallest tier. */
    public static List<Tier> chain(ServerLevel level, Item start) {
        List<Item> items = new ArrayList<>();
        List<Integer> factors = new ArrayList<>(); // factor between items[i] and items[i+1]
        items.add(start);

        Optional<Step> up = findUp(level, start);
        up.ifPresent(s -> { items.add(0, s.item()); factors.add(0, s.factor()); });
        Optional<Step> down = findDown(level, start);
        down.ifPresent(s -> { items.add(s.item()); factors.add(s.factor()); });

        if (items.size() < 3 && up.isPresent()) {
            findUp(level, items.get(0)).ifPresent(s -> { items.add(0, s.item()); factors.add(0, s.factor()); });
        }
        if (items.size() < 3 && down.isPresent()) {
            findDown(level, items.get(items.size() - 1)).ifPresent(s -> { items.add(s.item()); factors.add(s.factor()); });
        }

        List<Tier> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            out.add(new Tier(items.get(i), i < factors.size() ? factors.get(i) : 1));
        }
        return out;
    }

    /** 3x3 (or 2x2) of item crafts into one bigger item that uncrafts back. */
    private static Optional<Step> findUp(ServerLevel level, Item item) {
        for (int size : new int[]{3, 2}) {
            ItemStack result = craft(level, size, item);
            if (result.isEmpty() || result.getCount() != 1 || result.is(item)) continue;
            ItemStack back = craft(level, 1, result.getItem());
            if (back.is(item) && back.getCount() == size * size) return Optional.of(new Step(result.getItem(), size * size));
        }
        return Optional.empty();
    }

    /** One item uncrafts into 9 (or 4) smaller items that craft back. */
    private static Optional<Step> findDown(ServerLevel level, Item item) {
        ItemStack result = craft(level, 1, item);
        if (result.isEmpty() || result.is(item)) return Optional.empty();
        int n = result.getCount();
        if (n != 9 && n != 4) return Optional.empty();
        int size = n == 9 ? 3 : 2;
        ItemStack back = craft(level, size, result.getItem());
        if (back.is(item) && back.getCount() == 1) return Optional.of(new Step(result.getItem(), n));
        return Optional.empty();
    }

    private static ItemStack craft(ServerLevel level, int size, Item item) {
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(size * size, new ItemStack(item)));
        CraftingInput input = CraftingInput.of(size, size, grid);
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        return recipe.map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private CompactingRecipes() {}
}
