package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.drawer.CompactingRecipes;
import net.nexium.essentials.sponge.BigSpongeBlock;
import net.nexium.essentials.sponge.BigSponges;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Combined sponges: Large (reach 14), Huge (32) and Giant (64), dry and wet. All six are registered, named and tagged; nine make one
 * and one makes nine again; each soaks up water as far as its reach and as much as its capacity; a wet one dries in a furnace and in
 * the Nether; and the Backpack's Compacting module packs sponges.
 */
public class BigSpongesGameTest {
    private static final BlockPos ORIGIN = new BlockPos(1, 2, 1);

    private static ItemStack craft(GameTestHelper h, List<ItemStack> grid, int width, int height) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        Optional<RecipeHolder<CraftingRecipe>> recipe = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        return recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static Item below(BigSponges.Size size) {
        return size == BigSponges.Size.LARGE ? Items.SPONGE : BigSponges.item(BigSponges.Size.values()[size.ordinal() - 1]);
    }

    @GameTest
    public void allSixAreThereNamedAndTagged(GameTestHelper h) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse("nexium:big_sponges"));
        com.google.gson.JsonObject lang = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(
                BigSpongesGameTest.class.getResourceAsStream("/assets/nexium/lang/en_us.json"), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        for (BigSponges.Size size : BigSponges.Size.values()) {
            for (boolean wet : new boolean[]{false, true}) {
                String name = (wet ? "wet_" : "") + size.id() + "_sponge";
                Identifier id = Identifier.parse("nexium:" + name);
                Block block = wet ? BigSponges.wetBlock(size) : BigSponges.block(size);
                Item item = wet ? BigSponges.wetItem(size) : BigSponges.item(size);
                h.assertTrue(BuiltInRegistries.BLOCK.getValue(id) == block, name + " is a registered block");
                h.assertTrue(BuiltInRegistries.ITEM.getValue(id) == item, name + " is a registered item");
                h.assertTrue(block.asItem() == item, name + "'s block gives its item");
                h.assertTrue(item.builtInRegistryHolder().is(tag), name + " is in the big_sponges tag");
                h.assertTrue(lang.has("block.nexium." + name) && !lang.get("block.nexium." + name).getAsString().isBlank(), name + " has a name");
                h.assertTrue(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_HOE), name + " is mined with a hoe, like a sponge");
            }
        }
        h.assertValueEqual(BigSponges.items().size(), 6, "six items");
        h.assertValueEqual(BigSponges.Size.LARGE.reach(), 14, "Large reaches 14");
        h.assertValueEqual(BigSponges.Size.HUGE.reach(), 32, "Huge reaches 32");
        h.assertValueEqual(BigSponges.Size.GIANT.reach(), 64, "Giant reaches 64");
        h.succeed();
    }

    @GameTest
    public void nineMakeOneAndOneMakesNineAgain(GameTestHelper h) {
        for (BigSponges.Size size : BigSponges.Size.values()) {
            Item lower = below(size);
            List<ItemStack> nine = new ArrayList<>();
            for (int i = 0; i < 9; i++) nine.add(new ItemStack(lower));
            ItemStack made = craft(h, nine, 3, 3);
            h.assertTrue(made.is(BigSponges.item(size)) && made.getCount() == 1, "nine of the one below make a " + size.id() + " sponge, got " + made);
            ItemStack back = craft(h, List.of(new ItemStack(BigSponges.item(size))), 1, 1);
            h.assertTrue(back.is(lower) && back.getCount() == 9, "a " + size.id() + " sponge crafts back into nine, got " + back);
        }
        h.succeed();
    }

    @GameTest
    public void eachDropsItselfDryOrWet(GameTestHelper h) {
        for (BigSponges.Size size : BigSponges.Size.values()) {
            for (boolean wet : new boolean[]{false, true}) {
                BlockState state = (wet ? BigSponges.wetBlock(size) : BigSponges.block(size)).defaultBlockState();
                List<ItemStack> drops = Block.getDrops(state, h.getLevel(), h.absolutePos(ORIGIN), null);
                Item item = wet ? BigSponges.wetItem(size) : BigSponges.item(size);
                h.assertTrue(drops.size() == 1 && drops.get(0).is(item) && drops.get(0).getCount() == 1, size.id() + (wet ? " wet" : " dry") + " drops itself, drops: " + drops);
            }
        }
        h.succeed();
    }

    /** A line of water source blocks east from the sponge's position, laid without letting it flow. */
    private static void waterLine(ServerLevel level, BlockPos sponge, int length) {
        for (int i = 1; i <= length; i++) level.setBlock(sponge.east(i), Blocks.WATER.defaultBlockState(), 2);
    }

    private static int waterOnLine(ServerLevel level, BlockPos sponge, int length) {
        int n = 0;
        for (int i = 1; i <= length; i++) if (level.getBlockState(sponge.east(i)).is(Blocks.WATER)) n++;
        return n;
    }

    @GameTest
    public void eachReachesAsFarAsItsReach(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // A plain sponge first, to know what reach means: how far along a line of water it takes water from
        BlockPos plain = h.absolutePos(ORIGIN);
        waterLine(level, plain, 20);
        level.setBlockAndUpdate(plain, Blocks.SPONGE.defaultBlockState());
        int plainReach = 20 - waterOnLine(level, plain, 20);
        h.assertTrue(plainReach >= 5 && plainReach <= 7, "a plain sponge takes the water along the first 6 blocks or so: " + plainReach);
        for (BigSponges.Size size : BigSponges.Size.values()) {
            BlockPos sponge = h.absolutePos(ORIGIN.offset(0, 10 * (size.ordinal() + 1), 0));
            int length = size.reach() + 10;
            waterLine(level, sponge, length);
            level.setBlockAndUpdate(sponge, BigSponges.block(size).defaultBlockState());
            int taken = length - waterOnLine(level, sponge, length);
            // Whatever the plain sponge's number was for 6, a big one's is the same measure of its own reach
            h.assertValueEqual(taken, size.reach() - 6 + plainReach, size.id() + " sponge takes water along " + size.reach() + " blocks of line (the plain one took " + plainReach + " for 6)");
            h.assertTrue(level.getBlockState(sponge.east(length)).is(Blocks.WATER), "the water at the far end of the line, past its reach, is left");
            h.assertTrue(level.getBlockState(sponge).is(BigSponges.wetBlock(size)), size.id() + " sponge turned wet");
        }
        h.succeed();
    }

    /** Fills a cube of water source blocks around a position (no flowing), 2 * half + 1 blocks on a side. */
    private static void waterCube(ServerLevel level, BlockPos center, int half) {
        for (int x = -half; x <= half; x++) for (int y = -half; y <= half; y++) for (int z = -half; z <= half; z++) {
            level.setBlock(center.offset(x, y, z), Blocks.WATER.defaultBlockState(), 2);
        }
    }

    private static int waterInCube(ServerLevel level, BlockPos center, int half) {
        int n = 0;
        for (int x = -half; x <= half; x++) for (int y = -half; y <= half; y++) for (int z = -half; z <= half; z++) {
            if (level.getBlockState(center.offset(x, y, z)).is(Blocks.WATER)) n++;
        }
        return n;
    }

    @GameTest
    public void eachSoaksAsMuchWaterAsItsCapacity(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // Sponge in the middle of a cube of water bigger than it can take: it takes exactly its capacity, and leaves the rest
        int[] halves = {8, 14, 20};
        BlockPos base = h.absolutePos(ORIGIN.offset(0, 60, 0));
        for (BigSponges.Size size : BigSponges.Size.values()) {
            int half = halves[size.ordinal()];
            BlockPos center = base.offset(0, size.ordinal() * 100, 0);
            waterCube(level, center, half);
            int before = waterInCube(level, center, half);
            long start = System.nanoTime();
            level.setBlockAndUpdate(center, BigSponges.block(size).defaultBlockState());
            long ms = (System.nanoTime() - start) / 1_000_000L;
            int left = waterInCube(level, center, half);
            int taken = before - 1 - left; // one water block was where the sponge went
            h.assertTrue(level.getBlockState(center).is(BigSponges.wetBlock(size)), size.id() + " sponge turned wet");
            h.assertValueEqual(taken, size.capacity(), size.id() + " took its capacity of water (cube had " + before + ")");
            System.out.println("[sponge] " + size.id() + ": took " + taken + " water blocks in " + ms + " ms");
            h.assertTrue(ms < 15_000, size.id() + " sponge soaked its water in a reasonable time: " + ms + " ms");
        }
        h.succeed();
    }

    @GameTest
    public void aWetOneDriesInAFurnace(GameTestHelper h) {
        for (BigSponges.Size size : BigSponges.Size.values()) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(BigSponges.wetItem(size)));
            var recipe = h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING, input, h.getLevel());
            h.assertTrue(recipe.isPresent(), "a wet " + size.id() + " sponge can be smelted");
            ItemStack dried = recipe.get().value().assemble(input);
            h.assertTrue(dried.is(BigSponges.item(size)) && dried.getCount() == 1, "into the dry " + size.id() + " sponge, got " + dried);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 300)
    public void aWetOneDriesAtOnceInTheNether(GameTestHelper h) {
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "the Nether is there");
        BlockPos pos = new BlockPos(5, 90, 5);
        h.startSequence()
                .thenExecute(() -> nether.setChunkForced(0, 0, true))
                .thenWaitUntil(() -> h.assertTrue(nether.getChunkSource().getChunkNow(0, 0) != null, "waiting for the Nether chunk"))
                .thenIdle(10)
                .thenExecute(() -> {
                    // A vanilla wet sponge first, as the control: if it does not dry here neither will ours, whatever it does
                    nether.setBlockAndUpdate(pos, Blocks.WET_SPONGE.defaultBlockState());
                    boolean vanillaDried = nether.getBlockState(pos).is(Blocks.SPONGE);
                    nether.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    h.assertTrue(vanillaDried, "a vanilla wet sponge dries in the Nether (the control)");
                    nether.setBlockAndUpdate(pos, BigSponges.wetBlock(BigSponges.Size.HUGE).defaultBlockState());
                    boolean ours = nether.getBlockState(pos).is(BigSponges.block(BigSponges.Size.HUGE));
                    nether.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    nether.setChunkForced(0, 0, false);
                    h.assertTrue(ours, "a wet Huge Sponge placed in the Nether is a dry one at once");
                    BlockPos here = h.absolutePos(ORIGIN);
                    h.getLevel().setBlockAndUpdate(here, BigSponges.wetBlock(BigSponges.Size.LARGE).defaultBlockState());
                    h.assertTrue(h.getLevel().getBlockState(here).is(BigSponges.wetBlock(BigSponges.Size.LARGE)), "and stays wet in the Overworld");
                })
                .thenSucceed();
    }

    @GameTest
    public void compactingPacksSponges(GameTestHelper h) {
        for (BigSponges.Size size : BigSponges.Size.values()) {
            var pack = CompactingRecipes.packInto(h.getLevel(), below(size));
            h.assertTrue(pack.isPresent() && pack.get().into() == BigSponges.item(size) && pack.get().count() == 9,
                    "nine of the one below pack into a " + size.id() + " sponge: " + pack);
        }
        h.succeed();
    }

    @GameTest
    public void aStackTellsItsReachAndCapacity(GameTestHelper h) {
        for (BigSponges.Size size : BigSponges.Size.values()) {
            ItemStack stack = new ItemStack(BigSponges.item(size));
            List<Component> lines = new ArrayList<>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
            boolean said = lines.stream().anyMatch(l -> l.getContents() instanceof TranslatableContents t && t.getKey().equals("tooltip.nexium.big_sponge")
                    && t.getArgs()[0].equals(size.reach()));
            h.assertTrue(said, size.id() + " says it reaches " + size.reach() + ": " + lines);
            h.assertValueEqual(((BigSpongeBlock) BigSponges.block(size)).reach(), size.reach(), "and the block agrees");
        }
        h.succeed();
    }
}
