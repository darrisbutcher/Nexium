package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.freezer.FreezerBlock;
import net.storagenexus.freezer.FreezerBlockEntity;
import net.storagenexus.freezer.FreezerMode;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/** The Freezer: water to ice, packed ice and blue ice, its tank and its dye colour; and the ice and soul sand recipes. */
public class FreezerGameTest {
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final long BUCKET = FluidConstants.BUCKET;

    private static FreezerBlockEntity freezer(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.FREEZER);
        return h.getBlockEntity(POS, FreezerBlockEntity.class);
    }

    private static void tick(GameTestHelper h, FreezerBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) FreezerBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
    }

    private static int count(FreezerBlockEntity f, Item item) {
        int n = 0;
        for (int i = 0; i < FreezerBlockEntity.SLOTS; i++) if (f.getItem(i).is(item)) n += f.getItem(i).getCount();
        return n;
    }

    private static ItemStack craft(GameTestHelper h, List<ItemStack> items) {
        List<ItemStack> grid = new ArrayList<>(items);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest
    public void waterBecomesIceUsingOneBucket(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        f.setWater(3 * BUCKET);
        int ticks = NexusConfig.get().freezerIceTicks;
        tick(h, f, ticks - 1);
        h.assertValueEqual(count(f, Items.ICE), 0, "not yet");
        tick(h, f, 1);
        h.assertValueEqual(count(f, Items.ICE), 1, "one ice");
        h.assertValueEqual(f.water(), 2 * BUCKET, "one bucket used");
        tick(h, f, ticks * 2);
        h.assertValueEqual(count(f, Items.ICE), 3, "three ice from three buckets");
        h.assertValueEqual(f.water(), 0L, "the tank is empty");
        h.succeed();
    }

    @GameTest
    public void packedAndBlueIceUseTheirWaterSteadilyAndWaitWhenDry(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        f.setMode(FreezerMode.PACKED_ICE);
        f.setWater(FreezerBlockEntity.CAPACITY);
        int ticks = FreezerMode.PACKED_ICE.ticks();
        tick(h, f, ticks / 2);
        h.assertValueEqual(count(f, Items.PACKED_ICE), 0, "half way");
        h.assertTrue(f.water() < FreezerBlockEntity.CAPACITY, "water is used up as it works");
        // 9 buckets for one packed ice, and the tank only holds 8: it runs dry and waits
        tick(h, f, ticks);
        h.assertTrue(f.isDry(), "dry");
        h.assertValueEqual(count(f, Items.PACKED_ICE), 0, "no packed ice without enough water");
        f.setWater(FreezerBlockEntity.CAPACITY);
        tick(h, f, ticks);
        h.assertValueEqual(count(f, Items.PACKED_ICE), 1, "refilled, it finishes");
        // Blue ice: 90 seconds by default
        h.assertValueEqual(FreezerMode.BLUE_ICE.ticks(), 1800, "blue ice takes 90 seconds");
        h.assertValueEqual(FreezerMode.BLUE_ICE.buckets(), 81, "from 81 buckets");
        h.succeed();
    }

    @GameTest
    public void modeButtonCyclesAndRestartsTheProgress(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        f.setWater(2 * BUCKET);
        tick(h, f, 50);
        h.assertTrue(f.progress() > 0, "working");
        h.assertTrue(f.cycleMode() == FreezerMode.PACKED_ICE, "ice, then packed ice");
        h.assertValueEqual(f.progress(), 0, "a new mode starts over");
        h.assertTrue(f.cycleMode() == FreezerMode.BLUE_ICE, "then blue ice");
        h.assertTrue(f.cycleMode() == FreezerMode.ICE, "and back to ice");
        h.succeed();
    }

    @GameTest
    public void tankTakesOnlyWaterAndNoOneTakesItBack(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(f.tank().insert(FluidVariant.of(Fluids.LAVA), BUCKET, tx), 0L, "lava is refused");
            h.assertValueEqual(f.tank().insert(FluidVariant.of(Fluids.WATER), 10 * BUCKET, tx), 8 * BUCKET, "water fills it to 8 buckets");
            tx.commit();
        }
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(f.tank().extract(FluidVariant.of(Fluids.WATER), BUCKET, tx), 0L, "pipes can't empty it");
        }
        h.assertValueEqual(f.water(), 8 * BUCKET, "still full");
        h.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void aWaterBucketFillsTheTank(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        net.minecraft.server.level.ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        BlockPos abs = h.absolutePos(POS);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
        var result = h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        h.assertTrue(result.consumesAction(), "the bucket is used on the Freezer");
        h.assertValueEqual(f.water(), BUCKET, "one bucket went in");
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "and the player holds an empty bucket");
        h.succeed();
    }

    @GameTest
    public void redstoneAndAFullOutputPauseIt(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        f.setWater(BUCKET);
        h.setBlock(POS, ModBlocks.FREEZER.defaultBlockState().setValue(FreezerBlock.POWERED, true));
        FreezerBlockEntity powered = h.getBlockEntity(POS, FreezerBlockEntity.class);
        tick(h, powered, 30);
        h.assertValueEqual(powered.progress(), 0, "a redstone signal pauses it");
        h.assertValueEqual(powered.water(), BUCKET, "and uses no water");
        h.setBlock(POS, ModBlocks.FREEZER);
        FreezerBlockEntity full = h.getBlockEntity(POS, FreezerBlockEntity.class);
        full.setWater(BUCKET);
        for (int i = 0; i < FreezerBlockEntity.SLOTS; i++) full.setItem(i, new ItemStack(Items.ICE, 64));
        tick(h, full, 30);
        h.assertValueEqual(full.progress(), 0, "a full output waits");
        h.assertTrue(full.isFull(), "and says so");
        h.succeed();
    }

    @GameTest
    public void hoppersTakeTheIceFromBelow(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        h.assertTrue(f.getSlotsForFace(Direction.DOWN).length == FreezerBlockEntity.SLOTS, "the output below");
        h.assertTrue(f.getSlotsForFace(Direction.UP).length == 0, "nothing above");
        h.assertTrue(!f.canPlaceItemThroughFace(0, new ItemStack(Items.ICE), Direction.UP), "nothing goes in");
        h.succeed();
    }

    @GameTest
    public void dyeColourAndWaterStayInTheItem(GameTestHelper h) {
        FreezerBlockEntity f = freezer(h);
        h.assertTrue(!f.isDyed(), "an undyed Freezer");
        h.assertValueEqual(f.color(), FreezerBlockEntity.UNDYED, "keeps the frame colour");
        ItemStack item = new ItemStack(ModBlocks.FREEZER);
        item.set(DataComponents.DYED_COLOR, new DyedItemColor(0xB02E26));
        item.set(ModComponents.FREEZER_WATER, 2 * BUCKET);
        item.set(ModComponents.FREEZER_MODE, 2);
        f.applyComponentsFromItemStack(item);
        h.assertTrue(f.isDyed() && f.color() == 0xB02E26, "the colour came from the item");
        h.assertValueEqual(f.water(), 2 * BUCKET, "and the water");
        h.assertTrue(f.mode() == FreezerMode.BLUE_ICE, "and the mode");
        var back = f.collectComponents();
        h.assertTrue(back.get(DataComponents.DYED_COLOR) != null && back.get(DataComponents.DYED_COLOR).rgb() == 0xB02E26, "the dropped item keeps the colour");
        h.assertValueEqual(back.get(ModComponents.FREEZER_WATER), 2 * BUCKET, "and the water");
        h.succeed();
    }

    @GameTest
    public void dyesColourAFreezerAndMixLikeLeather(GameTestHelper h) {
        ItemStack red = craft(h, List.of(new ItemStack(ModBlocks.FREEZER), new ItemStack(Items.DYE.red())));
        h.assertTrue(red.is(ModBlocks.FREEZER.asItem()), "a dyed Freezer");
        DyedItemColor color = red.get(DataComponents.DYED_COLOR);
        h.assertTrue(color != null, "it has a dye colour");
        h.assertValueEqual(color.rgb() & 0xFFFFFF, DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF, "red");
        ItemStack mixed = craft(h, List.of(red, new ItemStack(Items.DYE.blue())));
        DyedItemColor second = mixed.get(DataComponents.DYED_COLOR);
        h.assertTrue(second != null && (second.rgb() & 0xFFFFFF) != (color.rgb() & 0xFFFFFF), "a second dye mixes the colour");
        h.succeed();
    }

    @GameTest
    public void freezerRecipeAndIceRecipes(GameTestHelper h) {
        ItemStack freezer = craft(h, List.of(new ItemStack(Items.PACKED_ICE), new ItemStack(Items.PACKED_ICE), new ItemStack(Items.PACKED_ICE),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.CAULDRON), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)));
        h.assertTrue(freezer.is(ModBlocks.FREEZER.asItem()), "the Freezer recipe");
        // Forwards (vanilla) and backwards (ours)
        h.assertValueEqual(craft(h, repeat(Items.ICE, 9)).getItem(), Items.PACKED_ICE, "9 ice make packed ice");
        h.assertValueEqual(craft(h, repeat(Items.PACKED_ICE, 9)).getItem(), Items.BLUE_ICE, "9 packed ice make blue ice");
        ItemStack ice = craft(h, List.of(new ItemStack(Items.PACKED_ICE)));
        h.assertTrue(ice.is(Items.ICE) && ice.getCount() == 9, "packed ice makes 9 ice");
        ItemStack packed = craft(h, List.of(new ItemStack(Items.BLUE_ICE)));
        h.assertTrue(packed.is(Items.PACKED_ICE) && packed.getCount() == 9, "blue ice makes 9 packed ice");
        h.succeed();
    }

    private static List<ItemStack> repeat(Item item, int n) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(new ItemStack(item));
        return list;
    }

    @GameTest
    public void soulSandSmeltsToSandInABlastFurnaceWithNoXp(GameTestHelper h) {
        var recipe = h.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(r -> r.id().identifier().toString().equals("storagenexus:sand_from_soul_sand")).findFirst();
        h.assertTrue(recipe.isPresent(), "the recipe exists");
        var value = (net.minecraft.world.item.crafting.BlastingRecipe) recipe.get().value();
        h.assertValueEqual(value.experience(), 0.0F, "no experience");
        h.assertValueEqual(value.cookingTime(), 100, "an ore's speed in a blast furnace");
        h.assertTrue(value.input().test(new ItemStack(Items.SOUL_SAND)), "soul sand goes in");
        h.assertTrue(!value.input().test(new ItemStack(Items.SAND)), "not sand");
        h.succeed();
    }
}
