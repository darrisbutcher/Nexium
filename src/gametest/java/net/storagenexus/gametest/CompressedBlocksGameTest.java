package net.storagenexus.gametest;

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
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.drawer.CompactingRecipes;
import net.storagenexus.essentials.compressed.CompressedBlockItem;
import net.storagenexus.essentials.compressed.CompressedBlocks;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Compressed blocks: 16 bulk blocks times 3 levels. Every one is registered, named, tagged and mined like the block it stands
 * for, drops itself, crafts from nine of the one below and back into nine, and says what it stands for. (That the Compacting
 * module packs them is in BackpackGameTest.)
 */
public class CompressedBlocksGameTest {
    private static final String[] LEVEL_NAMES = {"compressed_", "double_compressed_", "triple_compressed_"};

    private static Item previous(CompressedBlocks.Base base, int level) {
        return level == 1 ? base.block().asItem() : CompressedBlocks.item(base, level - 1);
    }

    @GameTest
    public void allFortyEightAreThereWithTheirNames(GameTestHelper h) {
        JsonObject lang = JsonParser.parseReader(new InputStreamReader(
                CompressedBlocksGameTest.class.getResourceAsStream("/assets/nexusessentials/lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse("nexusessentials:compressed_blocks"));
        int count = 0;
        for (CompressedBlocks.Base base : CompressedBlocks.Base.values()) {
            for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                String name = LEVEL_NAMES[level - 1] + base.id();
                Identifier id = Identifier.parse("nexusessentials:" + name);
                h.assertTrue(BuiltInRegistries.BLOCK.getValue(id) == CompressedBlocks.block(base, level), name + " is a registered block");
                h.assertTrue(BuiltInRegistries.ITEM.getValue(id) == CompressedBlocks.item(base, level), name + " is a registered item");
                h.assertTrue(CompressedBlocks.block(base, level).asItem() == CompressedBlocks.item(base, level), name + "'s block gives its item");
                h.assertTrue(CompressedBlocks.item(base, level).builtInRegistryHolder().is(tag), name + " is in the compressed_blocks tag");
                h.assertTrue(lang.has("block.nexusessentials." + name) && !lang.get("block.nexusessentials." + name).getAsString().isBlank(), name + " has a name");
                count++;
            }
        }
        h.assertValueEqual(count, 48, "sixteen blocks, three levels each");
        h.assertValueEqual(CompressedBlocks.items().size(), 48, "and the list has all of them");
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
            h.assertTrue(lines.stream().anyMatch(l -> l.getContents() instanceof TranslatableContents t && t.getKey().equals("tooltip.nexusessentials.compressed")
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
}
