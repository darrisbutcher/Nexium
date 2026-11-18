package net.nexium.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.drawer.CompactingRecipes;
import net.nexium.essentials.compressed.CompressedBlockItem;
import net.nexium.essentials.compressed.CompressedBlocks;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Compressed blocks: 29 bulk blocks times 3 levels. Every one is registered, named, tagged and mined like the block it stands
 * for, drops itself, crafts from nine of the one below and back into nine, and says what it stands for. (That the Compacting
 * module packs them is in BackpackGameTest.)
 */
public class CompressedBlocksGameTest {
    private static final String[] LEVEL_NAMES = {"compressed_", "double_compressed_", "triple_compressed_"};

    private static Item previous(CompressedBlocks.Base base, int level) {
        return level == 1 ? base.block().asItem() : CompressedBlocks.item(base, level - 1);
    }

    @GameTest
    public void allEightySevenAreThereWithTheirNames(GameTestHelper h) {
        JsonObject lang = JsonParser.parseReader(new InputStreamReader(
                CompressedBlocksGameTest.class.getResourceAsStream("/assets/nexium/lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse("nexium:compressed_blocks"));
        int count = 0;
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) {
            for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                String name = LEVEL_NAMES[level - 1] + base.id();
                Identifier id = Identifier.parse("nexium:" + name);
                h.assertTrue(BuiltInRegistries.BLOCK.getValue(id) == CompressedBlocks.block(base, level), name + " is a registered block");
                h.assertTrue(BuiltInRegistries.ITEM.getValue(id) == CompressedBlocks.item(base, level), name + " is a registered item");
                h.assertTrue(CompressedBlocks.block(base, level).asItem() == CompressedBlocks.item(base, level), name + "'s block gives its item");
                h.assertTrue(CompressedBlocks.item(base, level).builtInRegistryHolder().is(tag), name + " is in the compressed_blocks tag");
                h.assertTrue(lang.has("block.nexium." + name) && !lang.get("block.nexium." + name).getAsString().isBlank(), name + " has a name");
                count++;
            }
        }
        h.assertValueEqual(count, 87, "twenty-nine blocks, three levels each");
        h.assertValueEqual(CompressedBlocks.items().size(), 87, "and the list has all of them");
        h.assertValueEqual(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator().estimateSize() > 0, true, "the tag has items");
        h.succeed();
    }

    @GameTest
    public void eachIsMinedLikeTheBlockItStandsFor(GameTestHelper h) {
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) {
            BlockState plain = base.block().defaultBlockState();
            for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                Block block = CompressedBlocks.block(base, level);
                BlockState state = block.defaultBlockState();
                String name = LEVEL_NAMES[level - 1] + base.id();
                h.assertTrue(state.getDestroySpeed(h.getLevel(), BlockPos.ZERO) == plain.getDestroySpeed(h.getLevel(), BlockPos.ZERO), name + " has the hardness of " + base.id());
                h.assertTrue(block.getExplosionResistance() == base.block().getExplosionResistance(), name + " has the blast resistance of " + base.id());
                h.assertTrue(state.getSoundType() == plain.getSoundType(), name + " sounds like " + base.id());
                h.assertTrue(state.requiresCorrectToolForDrops() == !base.shovel(), name + " needs a pickaxe for drops exactly when " + base.id() + "'s family does");
                h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE) == !base.shovel(), name + " is mined with a pickaxe: " + !base.shovel());
                h.assertTrue(state.is(BlockTags.MINEABLE_WITH_SHOVEL) == base.shovel(), name + " is mined with a shovel: " + base.shovel());
            }
        }
        h.succeed();
    }

    @GameTest
    public void eachDropsItself(GameTestHelper h) {
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) {
            for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                BlockState state = CompressedBlocks.block(base, level).defaultBlockState();
                List<ItemStack> drops = Block.getDrops(state, h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)), null);
                h.assertTrue(drops.size() == 1 && drops.get(0).is(CompressedBlocks.item(base, level)) && drops.get(0).getCount() == 1,
                        LEVEL_NAMES[level - 1] + base.id() + " drops itself, drops: " + drops);
            }
        }
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, List<ItemStack> grid, int width, int height) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        Optional<RecipeHolder<CraftingRecipe>> recipe = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        return recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest
    public void nineMakeOneAndOneMakesNineAgain(GameTestHelper h) {
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) {
            for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                String name = LEVEL_NAMES[level - 1] + base.id();
                Item below = previous(base, level);
                Item here = CompressedBlocks.item(base, level);
                List<ItemStack> nine = new ArrayList<>();
                for (int i = 0; i < 9; i++) nine.add(new ItemStack(below));
                ItemStack made = craft(h, nine, 3, 3);
                h.assertTrue(made.is(here) && made.getCount() == 1, "nine of the block below craft " + name + ", got " + made);
                ItemStack back = craft(h, List.of(new ItemStack(here)), 1, 1);
                h.assertTrue(back.is(below) && back.getCount() == 9, name + " crafts back into nine of the block below, got " + back);
            }
        }
        h.succeed();
    }

    @GameTest
    public void aStackTellsWhatItStandsFor(GameTestHelper h) {
        for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
            ItemStack stack = new ItemStack(CompressedBlocks.item(CompressedBlocks.Base.COBBLED_DEEPSLATE, level));
            List<Component> lines = new ArrayList<>();
            ((CompressedBlockItem) stack.getItem()).appendHoverText(stack, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
            String expected = level == 1 ? "9" : level == 2 ? "81" : "729";
            h.assertTrue(lines.stream().anyMatch(l -> l.getContents() instanceof TranslatableContents t && t.getKey().equals("tooltip.nexium.compressed")
                            && t.getArgs()[0].equals(expected)), "level " + level + " says it stands for " + expected + ": " + lines);
            h.assertValueEqual(((CompressedBlockItem) stack.getItem()).standsFor(), Integer.parseInt(expected), "and standsFor() agrees");
        }
        h.succeed();
    }

    @GameTest
    public void aCompactingDrawerFindsTheTiers(GameTestHelper h) {
        List<CompactingRecipes.Tier> chain = CompactingRecipes.chain(h.getLevel(), Items.COBBLESTONE);
        h.assertTrue(chain.size() >= 2, "cobblestone is no longer a one-tier item: " + chain.size());
        h.assertTrue(chain.stream().anyMatch(t -> t.item() == Items.COBBLESTONE), "its chain includes cobblestone");
        h.assertTrue(chain.stream().anyMatch(t -> t.item() == CompressedBlocks.item(CompressedBlocks.Base.COBBLESTONE, 1)), "and the Compressed block");
        // Level 1 down to the plain block, and level 2 on top of it
        h.assertTrue(chain.stream().anyMatch(t -> t.item() == CompressedBlocks.item(CompressedBlocks.Base.COBBLESTONE, 2)), "and the Double Compressed one");
        h.succeed();
    }

    @GameTest
    public void theNewBulkBlocksAreAllThere(GameTestHelper h) {
        List<String> expected = List.of("calcite", "dripstone_block", "smooth_basalt", "terracotta", "sandstone", "red_sandstone", "obsidian",
                "magma_block", "mud", "clay", "soul_sand", "soul_soil", "snow_block");
        List<String> have = new ArrayList<>();
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) have.add(base.id());
        for (String id : expected) h.assertTrue(have.contains(id), id + " has compressed versions");
        // Soft ones take a shovel, the stony ones a pickaxe
        for (String id : List.of("mud", "clay", "soul_sand", "soul_soil", "snow_block")) {
            h.assertTrue(CompressedBlocks.Base.valueOf(id.toUpperCase(java.util.Locale.ROOT)).shovel(), id + " is dug with a shovel");
        }
        for (String id : List.of("calcite", "dripstone_block", "smooth_basalt", "terracotta", "sandstone", "red_sandstone", "obsidian", "magma_block")) {
            h.assertTrue(!CompressedBlocks.Base.valueOf(id.toUpperCase(java.util.Locale.ROOT)).shovel(), id + " is mined with a pickaxe");
        }
        h.succeed();
    }

    @GameTest
    public void eachGlowsLikeTheBlockItStandsFor(GameTestHelper h) {
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) {
            int light = base.block().defaultBlockState().getLightEmission();
            for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                h.assertValueEqual(CompressedBlocks.block(base, level).defaultBlockState().getLightEmission(), light,
                        LEVEL_NAMES[level - 1] + base.id() + " gives off the light of " + base.id());
            }
        }
        h.assertTrue(CompressedBlocks.block(CompressedBlocks.Base.MAGMA_BLOCK, 1).defaultBlockState().getLightEmission() > 0, "a Compressed Magma Block glows");
        h.assertValueEqual(CompressedBlocks.block(CompressedBlocks.Base.CALCITE, 1).defaultBlockState().getLightEmission(), 0, "a Compressed Calcite does not");
        h.succeed();
    }

    @GameTest
    public void compressedObsidianNeedsADiamondPickaxe(GameTestHelper h) {
        for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
            BlockState state = CompressedBlocks.block(CompressedBlocks.Base.OBSIDIAN, level).defaultBlockState();
            h.assertTrue(state.is(BlockTags.NEEDS_DIAMOND_TOOL), LEVEL_NAMES[level - 1] + "obsidian takes a diamond (or better) pickaxe");
            h.assertTrue(!state.is(BlockTags.NEEDS_IRON_TOOL), "and is not left at the iron tier");
        }
        h.assertTrue(CompressedBlocks.block(CompressedBlocks.Base.CALCITE, 1).defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL) == false, "calcite does not need a diamond pickaxe");
        h.assertTrue(Blocks.OBSIDIAN.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL), "which is what plain obsidian takes");
        h.succeed();
    }

    /** What each way-back recipe turns a block into: the block, what it gives back and how many. */
    private record WayBack(Item block, Item back, int count, Item pack) {}

    private static final List<WayBack> WAY_BACK = List.of(
            new WayBack(Items.SNOW_BLOCK, Items.SNOWBALL, 4, Items.SNOWBALL),
            new WayBack(Items.CLAY, Items.CLAY_BALL, 4, Items.CLAY_BALL),
            new WayBack(Items.AMETHYST_BLOCK, Items.AMETHYST_SHARD, 4, Items.AMETHYST_SHARD),
            new WayBack(Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4, Items.GLOWSTONE_DUST),
            new WayBack(Items.HONEYCOMB_BLOCK, Items.HONEYCOMB, 4, Items.HONEYCOMB),
            new WayBack(Items.MAGMA_BLOCK, Items.MAGMA_CREAM, 4, Items.MAGMA_CREAM),
            new WayBack(Items.MELON, Items.MELON_SLICE, 9, Items.MELON_SLICE),
            new WayBack(Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, 4, Items.POINTED_DRIPSTONE),
            new WayBack(Items.RESIN_BRICKS, Items.RESIN_BRICK, 4, Items.RESIN_BRICK));

    @GameTest
    public void aBlockCraftsBackIntoExactlyWhatMadeIt(GameTestHelper h) {
        for (WayBack w : WAY_BACK) {
            String name = BuiltInRegistries.ITEM.getKey(w.block()).getPath();
            ItemStack back = craft(h, List.of(new ItemStack(w.block())), 1, 1);
            h.assertTrue(back.is(w.back()) && back.getCount() == w.count(), name + " crafts back into " + w.count() + " " + BuiltInRegistries.ITEM.getKey(w.back()).getPath() + ", got " + back);
            // And the way there is what it always was: the same count of the item makes the block
            int side = w.count() == 9 ? 3 : 2;
            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < side * side; i++) grid.add(new ItemStack(w.back()));
            ItemStack made = craft(h, grid, side, side);
            h.assertTrue(made.is(w.block()) && made.getCount() == 1, w.count() + " " + BuiltInRegistries.ITEM.getKey(w.back()).getPath() + " make " + name + ", got " + made);
        }
        h.succeed();
    }

    @GameTest
    public void compactingKnowsEveryNewPack(GameTestHelper h) {
        for (WayBack w : WAY_BACK) {
            var pack = CompactingRecipes.packInto(h.getLevel(), w.pack());
            h.assertTrue(pack.isPresent() && pack.get().into() == w.block() && pack.get().count() == w.count(),
                    BuiltInRegistries.ITEM.getKey(w.pack()).getPath() + " packs into " + BuiltInRegistries.ITEM.getKey(w.block()).getPath() + ", " + pack);
        }
        // The new bulk blocks pack 9 to 1 into their Compressed block
        for (CompressedBlocks.Base base : List.of(CompressedBlocks.Base.CALCITE, CompressedBlocks.Base.DRIPSTONE_BLOCK, CompressedBlocks.Base.OBSIDIAN,
                CompressedBlocks.Base.MAGMA_BLOCK, CompressedBlocks.Base.MUD, CompressedBlocks.Base.SOUL_SAND, CompressedBlocks.Base.SNOW_BLOCK,
                CompressedBlocks.Base.CLAY, CompressedBlocks.Base.SANDSTONE, CompressedBlocks.Base.TERRACOTTA)) {
            var pack = CompactingRecipes.packInto(h.getLevel(), base.block().asItem());
            h.assertTrue(pack.isPresent() && pack.get().into() == CompressedBlocks.item(base, 1) && pack.get().count() == 9, base.id() + " packs 9 into its Compressed block: " + pack);
        }
        // Ice already had its way: ice, packed ice, blue ice, each 9 to 1 and each back again
        h.assertTrue(CompactingRecipes.packInto(h.getLevel(), Items.ICE).map(p -> p.into() == Items.PACKED_ICE && p.count() == 9).orElse(false), "9 ice make packed ice");
        h.assertTrue(CompactingRecipes.packInto(h.getLevel(), Items.PACKED_ICE).map(p -> p.into() == Items.BLUE_ICE && p.count() == 9).orElse(false), "9 packed ice make blue ice");
        h.succeed();
    }
}
