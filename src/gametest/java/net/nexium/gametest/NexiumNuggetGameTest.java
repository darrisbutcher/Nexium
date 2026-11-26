package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.nexium.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Nexium Nuggets: nine make an ingot and an ingot makes nine; a Redstone Fuel Rod now takes a nugget (not an ingot) in the middle. */
public class NexiumNuggetGameTest {
    @GameTest
    public void nineNuggetsMakeAnIngotAndBack(GameTestHelper h) {
        var level = h.getLevel();
        CraftingInput nuggets = CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(ModItems.NEXIUM_NUGGET)));
        var up = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, nuggets, level);
        h.assertTrue(up.isPresent() && up.get().value().assemble(nuggets).is(ModItems.NEXIUM_INGOT), "nine nuggets make a Nexium Ingot");
        CraftingInput ingot = CraftingInput.of(1, 1, List.of(new ItemStack(ModItems.NEXIUM_INGOT)));
        var down = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, ingot, level);
        ItemStack out = down.isPresent() ? down.get().value().assemble(ingot) : ItemStack.EMPTY;
        h.assertTrue(out.is(ModItems.NEXIUM_NUGGET) && out.getCount() == 9, "an ingot makes nine nuggets");
        h.succeed();
    }

    private static CraftingInput rod(ItemStack centre) {
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, new ItemStack(Items.REDSTONE_BLOCK)));
        grid.set(4, centre);
        return CraftingInput.of(3, 3, grid);
    }

    @GameTest
    public void aFuelRodNeedsANuggetNotAnIngot(GameTestHelper h) {
        var level = h.getLevel();
        CraftingInput withNugget = rod(new ItemStack(ModItems.NEXIUM_NUGGET));
        var made = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, withNugget, level);
        h.assertTrue(made.isPresent() && made.get().value().assemble(withNugget).is(ModItems.REDSTONE_FUEL_ROD), "eight redstone blocks around a nugget make a Fuel Rod");
        CraftingInput withIngot = rod(new ItemStack(ModItems.NEXIUM_INGOT));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, withIngot, level).isEmpty(), "an ingot no longer does");
        h.succeed();
    }
}
