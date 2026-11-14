package net.storagenexus.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The crafting tweaks the mod brings (bigger stair, trapdoor, wood and brick batches, slabs and stairs back to blocks,
 * dyeing, the odd craftable block and the Armadillo Crate), and the two loot tweaks: double shulker shells and Silk Touch
 * budding amethyst.
 */
public class RecipeTweaksGameTest {
    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }

    private static ItemStack stack(String path, int count) {
        return new ItemStack(item(path), count);
    }

    /** Every crafting recipe that matches the grid, as the stacks they make. */
    private static List<ItemStack> craft(GameTestHelper h, int width, int height, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        List<ItemStack> made = new ArrayList<>();
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, h.getLevel())) made.add(recipe.assemble(input));
        }
        return made;
    }

    private static boolean makes(List<ItemStack> made, String result, int count) {
        for (ItemStack s : made) if (s.is(item(result)) && s.getCount() == count) return true;
        return false;
    }

    private static List<ItemStack> repeat(String path, int n) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(stack(path, 1));
        return out;
    }

    @GameTest
    public void tweakRecipesLoad(GameTestHelper h) {
        long ours = h.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(r -> r.id().identifier().getNamespace().equals("nexusessentials") && r.id().identifier().getPath().startsWith("tweaks/")).count();
        h.assertValueEqual(ours, 410L, "recipe tweaks loaded");
        h.succeed();
    }

    @GameTest
    public void stairsTrapdoorsWoodAndBricksMakeMore(GameTestHelper h) {
        ItemStack planks = stack("oak_planks", 1), none = ItemStack.EMPTY;
        h.assertTrue(makes(craft(h, 3, 3, List.of(planks, none, none, planks, planks, none, planks, planks, planks)), "oak_stairs", 8), "8 stairs from 6 planks");
        h.assertTrue(makes(craft(h, 3, 2, List.of(planks, planks, planks, planks, planks, planks)), "oak_trapdoor", 12), "12 trapdoors from 6 planks");
        ItemStack log = stack("oak_log", 1);
        h.assertTrue(makes(craft(h, 2, 2, List.of(log, log, log, log)), "oak_wood", 4), "4 wood from 4 logs");
        ItemStack brick = stack("brick", 1);
        h.assertTrue(makes(craft(h, 2, 2, List.of(brick, brick, brick, brick)), "bricks", 4), "4 brick blocks from 4 bricks");
        // Chiseled and cracked stone bricks make stairs as well
        ItemStack chiseled = stack("chiseled_stone_bricks", 1), cracked = stack("cracked_stone_bricks", 1), stone = stack("stone_bricks", 1);
        h.assertTrue(makes(craft(h, 3, 3, List.of(chiseled, none, none, cracked, stone, none, stone, chiseled, cracked)), "stone_brick_stairs", 8),
                "mixed stone bricks make 8 stone brick stairs");
        h.succeed();
    }

    @GameTest
    public void slabsAndStairsGoBackToBlocksAndBlocksToSlabs(GameTestHelper h) {
        h.assertTrue(makes(craft(h, 2, 1, repeat("oak_slab", 2)), "oak_planks", 1), "2 slabs make a block");
        h.assertTrue(makes(craft(h, 2, 2, repeat("oak_stairs", 4)), "oak_planks", 3), "4 stairs make 3 blocks");
        h.assertTrue(makes(craft(h, 1, 1, List.of(stack("cobblestone", 1))), "cobblestone_slab", 2), "a block makes 2 slabs");
        h.assertTrue(makes(craft(h, 2, 2, repeat("smooth_stone_slab", 4)), "smooth_stone", 2), "4 smooth stone slabs make 2 smooth stone");
        // No loop: 3 blocks to 6 slabs back to 3 blocks, 6 blocks to 8 stairs back to 6 blocks
        h.succeed();
    }

    @GameTest
    public void dyeingAndTheSmallRecipes(GameTestHelper h) {
        List<ItemStack> candles = new ArrayList<>();
        String[] colours = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray"};
        for (String c : colours) candles.add(stack(c + "_candle", 1));
        candles.add(4, stack("ink_sac", 1));
        h.assertTrue(makes(craft(h, 3, 3, candles), "black_candle", 8), "eight candles and an ink sac make 8 black candles");
        List<ItemStack> glass = new ArrayList<>();
        for (int i = 0; i < 8; i++) glass.add(stack(colours[i] + "_stained_glass", 1));
        glass.add(4, stack("ice", 1));
        h.assertTrue(makes(craft(h, 3, 3, glass), "glass", 8), "ice washes stained glass clear");
        h.assertTrue(makes(craft(h, 2, 2, repeat("flint", 4)), "gravel", 1), "flint makes gravel");
        h.assertTrue(makes(craft(h, 1, 1, List.of(stack("coal", 1))), "black_dye", 1), "coal makes black dye");
        h.assertTrue(makes(craft(h, 1, 1, List.of(stack("packed_ice", 1))), "ice", 9), "packed ice makes 9 ice");
        h.assertTrue(makes(craft(h, 1, 1, List.of(stack("nether_wart_block", 1))), "nether_wart", 9), "a nether wart block makes 9 nether wart");
        // The Armadillo Crate is a shulker box named for its scutes
        ItemStack scute = stack("armadillo_scute", 1), chest = stack("chest", 1);
        List<ItemStack> crate = craft(h, 3, 3, List.of(scute, scute, scute, scute, chest, scute, scute, scute, scute));
        h.assertTrue(crate.stream().anyMatch(s -> s.is(Items.SHULKER_BOX) && s.getHoverName().getString().equals("Armadillo Crate")), "the Armadillo Crate");
        h.succeed();
    }

    @GameTest
    public void aQuartzBlockCutsBackToFourQuartz(GameTestHelper h) {
        var input = new SingleRecipeInput(stack("quartz_block", 1));
        var recipe = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.STONECUTTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "the stonecutter takes a quartz block");
        // The stonecutter offers every recipe for the block: look for the one that gives quartz
        boolean found = false;
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe cut && cut.matches(input, h.getLevel())) {
                ItemStack made = cut.assemble(input);
                if (made.is(Items.QUARTZ) && made.getCount() == 4) found = true;
            }
        }
        h.assertTrue(found, "a quartz block makes 4 quartz in the stonecutter");
        h.succeed();
    }

    @GameTest
    public void smeltingTweaks(GameTestHelper h) {
        var manager = h.getLevel().getServer().getRecipeManager();
        var leather = manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack("rotten_flesh", 1)), h.getLevel());
        h.assertTrue(leather.isPresent() && leather.get().value().assemble(new SingleRecipeInput(stack("rotten_flesh", 1))).is(Items.LEATHER), "rotten flesh smelts to leather");
        var string = manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack("white_wool", 1)), h.getLevel());
        h.assertTrue(string.isPresent() && string.get().value().assemble(new SingleRecipeInput(stack("white_wool", 1))).getCount() == 4, "wool smelts to 4 string");
        h.succeed();
    }

    @GameTest
    public void shulkersDropTwoShellsAndBuddingAmethystNeedsSilkTouch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Entity shulker = EntityTypes.SHULKER.create(level, EntitySpawnReason.SPAWNER);
        var table = level.getServer().reloadableRegistries().getLootTable(shulker.getLootTable().get());
        var killer = FakePlayer.get(level);
        int dropped = 0, empty = 0;
        for (int i = 0; i < 200; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, shulker)
                    .withParameter(LootContextParams.ORIGIN, shulker.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                    .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                    .create(LootContextParamSets.ENTITY);
            int shells = 0;
            for (ItemStack s : table.getRandomItems(params)) if (s.is(Items.SHULKER_SHELL)) shells += s.getCount();
            if (shells == 0) empty++; else { h.assertValueEqual(shells, 2, "two shells a drop"); dropped++; }
        }
        h.assertTrue(dropped > 40 && empty > 40, "about half the shulkers drop shells: " + dropped + " drops, " + empty + " without");

        var budding = Blocks.BUDDING_AMETHYST.defaultBlockState();
        var buddingTable = level.getServer().reloadableRegistries().getLootTable(Blocks.BUDDING_AMETHYST.getLootTable().get());
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        LootParams with = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.BLOCK_STATE, budding).withParameter(LootContextParams.TOOL, silk).create(LootContextParamSets.BLOCK);
        LootParams without = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.BLOCK_STATE, budding).withParameter(LootContextParams.TOOL, new ItemStack(Items.DIAMOND_PICKAXE)).create(LootContextParamSets.BLOCK);
        h.assertTrue(buddingTable.getRandomItems(with).stream().anyMatch(s -> s.is(Items.BUDDING_AMETHYST)), "Silk Touch collects budding amethyst");
        h.assertTrue(buddingTable.getRandomItems(without).isEmpty(), "and without it nothing drops");
        h.succeed();
    }

    private static boolean makesItem(List<ItemStack> made, Item result, int count) {
        for (ItemStack s : made) if (s.is(result) && s.getCount() == count) return true;
        return false;
    }

    /** The grid, three across, from a pattern whose letters are given in the map's order: " " is an empty cell. */
    private static List<ItemStack> grid(String pattern, java.util.Map<Character, Item> key) {
        List<ItemStack> grid = new ArrayList<>();
        for (char c : pattern.toCharArray()) grid.add(c == ' ' ? ItemStack.EMPTY : new ItemStack(key.get(c)));
        return grid;
    }

    /** 1.21.11: the Tier 1 item and fluid conduits make 8, the Infinite Lava Source takes an eye of ender, the Elevator chiseled deepslate. */
    @GameTest
    public void conduitLavaSourceAndElevatorRecipes(GameTestHelper h) {
        var nugget = Items.IRON_NUGGET;
        var item = grid("NNNGHGNNN", java.util.Map.of('N', nugget, 'G', Items.GLASS, 'H', Items.HOPPER));
        h.assertTrue(makesItem(craft(h, 3, 3, item), net.storagenexus.registry.ModItems.ITEM_CONDUIT, 8), "the Item Conduit makes 8");
        var fluid = grid("NNNGBGNNN", java.util.Map.of('N', nugget, 'G', Items.GLASS, 'B', Items.BUCKET));
        h.assertTrue(makesItem(craft(h, 3, 3, fluid), net.storagenexus.registry.ModItems.FLUID_CONDUIT, 8), "the Fluid Conduit makes 8");

        var lava = java.util.Map.<Character, Item>of('O', Items.OBSIDIAN, 'L', Items.LAVA_BUCKET, 'E', Items.ENDER_EYE, 'S', Items.NETHER_STAR);
        h.assertTrue(makesItem(craft(h, 3, 3, grid("OLOLELOLO", lava)), net.storagenexus.registry.ModBlocks.LAVA_SPRING.asItem(), 1),
                "obsidian, lava buckets and an eye of ender make the Infinite Lava Source");
        h.assertTrue(craft(h, 3, 3, grid("OLOLSLOLO", lava)).isEmpty(), "a nether star no longer does");

        var elevator = java.util.Map.<Character, Item>of('B', Items.CHISELED_DEEPSLATE, 'I', Items.IRON_INGOT, 'R', Items.REDSTONE_BLOCK,
                'P', Items.POLISHED_BLACKSTONE);
        h.assertTrue(makesItem(craft(h, 3, 3, grid("BBBIRII I", elevator)), net.storagenexus.registry.ModBlocks.ELEVATOR.asItem(), 2),
                "chiseled deepslate, iron and a redstone block make 2 Elevators");
        h.assertTrue(craft(h, 3, 3, grid("PPPIRII I", elevator)).isEmpty(), "polished blackstone no longer does");
        h.succeed();
    }
}
