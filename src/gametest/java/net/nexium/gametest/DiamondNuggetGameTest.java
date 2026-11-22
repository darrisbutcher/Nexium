package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.nexium.energy.NexusFurnaceBlockEntity;
import net.nexium.registry.ModItems;

import java.util.Collections;

/**
 * Diamond Nuggets: nine make a diamond and a diamond makes nine; smelting diamond tools, armor and the Diamond AIOT gives nuggets, as many
 * as a whole one is worth scaled by what is left of its durability (never fewer than one, never more than a stack).
 */
public class DiamondNuggetGameTest {
    private static ItemStack worn(ItemStack stack, double fractionLeft) {
        int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        stack.set(DataComponents.DAMAGE, max - (int) Math.round(max * fractionLeft));
        return stack;
    }

    private static int smelted(GameTestHelper h, ItemStack stack) {
        var recipe = NexusFurnaceBlockEntity.recipeFor(h.getLevel(), stack);
        h.assertTrue(recipe.isPresent(), stack.getItem() + " has a smelting recipe");
        ItemStack out = recipe.get().assemble(new SingleRecipeInput(stack));
        h.assertTrue(out.is(ModItems.DIAMOND_NUGGET), "it smelts to diamond nuggets");
        return out.getCount();
    }

    @GameTest
    public void aWholeToolGivesWhatItsDiamondsAreWorth(GameTestHelper h) {
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_PICKAXE)), 27, "a pickaxe, three diamonds");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_SHOVEL)), 9, "a shovel, one diamond");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_SWORD)), 18, "a sword, two");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_SPEAR)), 9, "a spear, one");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_BOOTS)), 36, "boots, four");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_HELMET)), 45, "a helmet, five");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_LEGGINGS)), 63, "leggings, seven");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_CHESTPLATE)), 64, "a chestplate, eight diamonds, held to one stack");
        h.assertValueEqual(smelted(h, new ItemStack(Items.DIAMOND_HORSE_ARMOR)), 63, "horse armor, seven");
        h.assertValueEqual(smelted(h, new ItemStack(ModItems.AIOTS.get(net.nexium.aiot.AiotTier.DIAMOND))), 64, "the Diamond AIOT, held to one stack");
        h.succeed();
    }

    @GameTest
    public void wearTakesItsShareOffAndOneIsTheLeast(GameTestHelper h) {
        h.assertValueEqual(smelted(h, worn(new ItemStack(Items.DIAMOND_PICKAXE), 0.5)), 14, "half left gives about half");
        h.assertValueEqual(smelted(h, worn(new ItemStack(Items.DIAMOND_PICKAXE), 0.25)), 7, "a quarter left");
        ItemStack almost = new ItemStack(Items.DIAMOND_PICKAXE);
        almost.set(DataComponents.DAMAGE, almost.getOrDefault(DataComponents.MAX_DAMAGE, 0) - 1);
        h.assertValueEqual(smelted(h, almost), 1, "one point left still gives a nugget");
        h.assertValueEqual(smelted(h, worn(new ItemStack(Items.DIAMOND_SHOVEL), 0.0)), 1, "a shovel with nothing left still gives one");
        h.succeed();
    }

    @GameTest(maxTicks = 300)
    public void aBlastFurnaceSmeltsAWornToolIntoNuggets(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, Blocks.BLAST_FURNACE);
        BlastFurnaceBlockEntity furnace = h.getBlockEntity(pos, BlastFurnaceBlockEntity.class);
        furnace.setItem(0, worn(new ItemStack(Items.DIAMOND_AXE), 0.5));
        furnace.setItem(1, new ItemStack(Items.COAL));
        h.succeedWhen(() -> {
            ItemStack out = furnace.getItem(2);
            h.assertTrue(out.is(ModItems.DIAMOND_NUGGET), "the furnace has made nuggets");
            h.assertValueEqual(out.getCount(), 14, "half of the 27 an axe is worth");
        });
    }

    @GameTest
    public void nineNuggetsMakeADiamondAndBack(GameTestHelper h) {
        var level = h.getLevel();
        CraftingInput nuggets = CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(ModItems.DIAMOND_NUGGET)));
        var toDiamond = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, nuggets, level);
        h.assertTrue(toDiamond.isPresent() && toDiamond.get().value().assemble(nuggets).is(Items.DIAMOND), "nine nuggets make a diamond");
        CraftingInput diamond = CraftingInput.of(1, 1, java.util.List.of(new ItemStack(Items.DIAMOND)));
        var back = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, diamond, level);
        ItemStack out = back.isPresent() ? back.get().value().assemble(diamond) : ItemStack.EMPTY;
        h.assertTrue(out.is(ModItems.DIAMOND_NUGGET) && out.getCount() == 9, "a diamond makes nine nuggets");
        h.succeed();
    }
}
