package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawers are made from planks and slabs of one wood, with no chest: every wood's 1x1, 1x2 and 2x2 come out of their grids
 * in the right numbers, and no other recipe (vanilla's barrel included) matches the same grid.
 */
public class DrawerRecipeGameTest {
    private static final String[][] PATTERNS = {{"PSP", "S S", "PSP"}, {"PSP", "PPP", "PSP"}, {"SPS", "PPP", "SPS"}};
    private static final int[] COUNTS = {1, 2, 4};

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }

    @GameTest
    public void everyWoodsDrawersAreMadeFromPlanksAndSlabs(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (DrawerWood wood : DrawerWood.values()) {
            Item planks = item(wood.planks()), slab = item(wood.id() + "_slab");
            for (int size = 0; size < 3; size++) {
                List<ItemStack> grid = new ArrayList<>();
                for (String row : PATTERNS[size]) {
                    for (char c : row.toCharArray()) grid.add(c == 'P' ? new ItemStack(planks) : c == 'S' ? new ItemStack(slab) : ItemStack.EMPTY);
                }
                CraftingInput input = CraftingInput.of(3, 3, grid);
                List<String> matches = new ArrayList<>();
                ItemStack made = ItemStack.EMPTY;
                for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
                    if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, h.getLevel())) {
                        matches.add(holder.id().identifier().toString());
                        made = recipe.assemble(input);
                    }
                }
                Item expected = ModBlocks.DRAWERS.get(wood)[size].asItem();
                String what = wood.id() + " " + new String[]{"1x1", "1x2", "2x2"}[size];
                if (matches.size() != 1) problems.add(what + " matches " + matches);
                else if (!made.is(expected) || made.getCount() != COUNTS[size]) problems.add(what + " makes " + made);
            }
        }
        h.assertTrue(problems.isEmpty(), "drawer recipes: " + problems);
        h.succeed();
    }
}
