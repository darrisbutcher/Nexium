package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.nexium.autocraft.AutocrafterBlockEntity;
import net.nexium.autocraft.AutocraftingService;
import net.nexium.autocraft.Pattern;
import net.nexium.autocraft.PatternEncoderBlockEntity;
import net.nexium.autocraft.PatternEncoderMenu;
import net.nexium.autocraft.PatternItem;
import net.nexium.autocraft.Planner;
import net.nexium.network.StorageNetwork;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Autocrafting: the planner, an Autocrafter making a chain of recipes, processing patterns into a machine, and encoding patterns. */
public class AutocraftGameTest {
    private static final BlockPos TERMINAL = new BlockPos(1, 1, 1), CRAFTER = new BlockPos(1, 1, 2), DRIVE = new BlockPos(2, 1, 1),
            CHEST = new BlockPos(1, 1, 3);

    private static Pattern crafting(Item[] grid, Item result, int count) {
        List<ItemStack> inputs = new ArrayList<>();
        for (Item item : grid) inputs.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        return new Pattern(false, inputs, List.of(new ItemStack(result, count)));
    }

    private static Pattern planksFromLog() {
        return crafting(new Item[] {Items.OAK_LOG, null, null, null, null, null, null, null, null}, Items.OAK_PLANKS, 4);
    }

    private static Pattern sticksFromPlanks() {
        return crafting(new Item[] {Items.OAK_PLANKS, null, null, Items.OAK_PLANKS, null, null, null, null, null}, Items.STICK, 4);
    }

    private static ItemStack patternItem(Pattern pattern) {
        ItemStack stack = new ItemStack(ModItems.PATTERN);
        stack.set(ModComponents.PATTERN, pattern);
        return stack;
    }

    @GameTest
    public void thePlannerCraftsIngredientsFirst(GameTestHelper h) {
        BlockPos at = new BlockPos(0, 0, 0);
        List<Planner.PatternRef> refs = List.of(new Planner.PatternRef(at, 0, sticksFromPlanks()), new Planner.PatternRef(at, 1, planksFromLog()));
        Planner.Plan plan = Planner.plan(Map.of(ItemVariant.of(Items.OAK_LOG), 1L), refs, ItemVariant.of(Items.STICK), 4);
        h.assertTrue(plan.ok(), "one log is enough for four sticks");
        h.assertValueEqual(plan.steps().size(), 2, "planks, then sticks");
        h.assertValueEqual(plan.steps().get(0).ref().pattern().primary().getItem(), Items.OAK_PLANKS, "planks first");
        h.assertValueEqual(plan.steps().get(1).ref().pattern().primary().getItem(), Items.STICK, "sticks second");
        Planner.Plan missing = Planner.plan(Map.of(), refs, ItemVariant.of(Items.STICK), 4);
        h.assertTrue(!missing.ok(), "no log: not possible");
        h.assertTrue(missing.missing().containsKey(ItemVariant.of(Items.OAK_LOG)), "the log is what is missing");
        Planner.Plan none = Planner.plan(Map.of(ItemVariant.of(Items.STICK), 8L), List.of(), ItemVariant.of(Items.STICK), 4);
        h.assertTrue(none.steps().isEmpty() && none.missing().isEmpty(), "what is already stored needs no crafting");
        h.assertTrue(!none.ok(), "and there is nothing to start");
        h.succeed();
    }

    @GameTest(maxTicks = 120)
    public void anAutocrafterMakesAChainOfRecipes(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CRAFTER, ModBlocks.AUTOCRAFTER);
        h.setBlock(DRIVE, ModBlocks.DISK_DRIVE);
        var drive = h.getBlockEntity(DRIVE, net.nexium.disk.DiskDriveBlockEntity.class);
        drive.disks().setItem(0, new ItemStack(ModItems.STORAGE_DISKS.get(net.nexium.disk.DiskTier.K16)));
        AutocrafterBlockEntity crafter = h.getBlockEntity(CRAFTER, AutocrafterBlockEntity.class);
        crafter.patterns().setItem(0, patternItem(sticksFromPlanks()));
        crafter.patterns().setItem(1, patternItem(planksFromLog()));
        crafter.energy().set(AutocrafterBlockEntity.BUFFER);
        StorageNetwork.clearCache();
        StorageNetwork net = StorageNetwork.find(h.getLevel(), h.absolutePos(TERMINAL));
        try (Transaction tx = Transaction.openOuter()) {
            net.insertItem(ItemVariant.of(Items.OAK_LOG), 1, tx);
            tx.commit();
        }
        ServerLevel level = h.getLevel();
        List<ItemStack> craftables = AutocraftingService.craftables(level, net);
        h.assertValueEqual(craftables.size(), 2, "the network can make planks and sticks");
        Planner.Plan plan = AutocraftingService.plan(level, net, ItemVariant.of(Items.STICK), 4);
        h.assertTrue(plan.ok(), "the plan works");
        AutocraftingService.start(level, h.absolutePos(TERMINAL), null, ItemVariant.of(Items.STICK), 4, plan);
        h.succeedWhen(() -> {
            h.assertTrue(net.countItem(ItemVariant.of(Items.STICK)) >= 4, "four sticks arrive");
            h.assertValueEqual(net.countItem(ItemVariant.of(Items.OAK_LOG)), 0L, "the log was used");
            h.assertTrue(crafter.energy().stored() < AutocrafterBlockEntity.BUFFER, "crafting used energy");
        });
    }

    @GameTest
    public void withoutEnergyNothingIsCrafted(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CRAFTER, ModBlocks.AUTOCRAFTER);
        h.setBlock(DRIVE, ModBlocks.DISK_DRIVE);
        h.getBlockEntity(DRIVE, net.nexium.disk.DiskDriveBlockEntity.class).disks().setItem(0, new ItemStack(ModItems.STORAGE_DISKS.get(net.nexium.disk.DiskTier.K16)));
        AutocrafterBlockEntity crafter = h.getBlockEntity(CRAFTER, AutocrafterBlockEntity.class);
        crafter.patterns().setItem(0, patternItem(planksFromLog()));
        StorageNetwork.clearCache();
        StorageNetwork net = StorageNetwork.find(h.getLevel(), h.absolutePos(TERMINAL));
        try (Transaction tx = Transaction.openOuter()) {
            net.insertItem(ItemVariant.of(Items.OAK_LOG), 1, tx);
            tx.commit();
        }
        AutocrafterBlockEntity.Start start = crafter.startCrafting(h.getLevel(), net, crafter.pattern(0), 1, UUID.randomUUID());
        h.assertValueEqual(start, AutocrafterBlockEntity.Start.NO_ENERGY, "an empty Autocrafter waits for energy");
        h.assertValueEqual(net.countItem(ItemVariant.of(Items.OAK_LOG)), 1L, "the log stays in storage");
        h.succeed();
    }

    @GameTest
    public void aProcessingPatternPushesIntoAMachine(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CRAFTER, ModBlocks.AUTOCRAFTER);
        h.setBlock(CHEST, Blocks.CHEST);
        h.setBlock(DRIVE, ModBlocks.DISK_DRIVE);
        h.getBlockEntity(DRIVE, net.nexium.disk.DiskDriveBlockEntity.class).disks().setItem(0, new ItemStack(ModItems.STORAGE_DISKS.get(net.nexium.disk.DiskTier.K16)));
        AutocrafterBlockEntity crafter = h.getBlockEntity(CRAFTER, AutocrafterBlockEntity.class);
        Pattern pattern = new Pattern(true, List.of(new ItemStack(Items.COBBLESTONE, 2)), List.of(new ItemStack(Items.STONE)));
        crafter.patterns().setItem(0, patternItem(pattern));
        crafter.energy().set(AutocrafterBlockEntity.BUFFER);
        StorageNetwork.clearCache();
        StorageNetwork net = StorageNetwork.find(h.getLevel(), h.absolutePos(TERMINAL));
        try (Transaction tx = Transaction.openOuter()) {
            net.insertItem(ItemVariant.of(Items.COBBLESTONE), 2, tx);
            tx.commit();
        }
        h.assertValueEqual(crafter.push(h.getLevel(), net, pattern, 1), AutocrafterBlockEntity.Start.STARTED, "pushed");
        ChestBlockEntity chest = h.getBlockEntity(CHEST, ChestBlockEntity.class);
        h.assertValueEqual(chest.countItem(Items.COBBLESTONE), 2, "the machine got the inputs");
        h.assertValueEqual(net.countItem(ItemVariant.of(Items.COBBLESTONE)), 0L, "and the network gave them up");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void aBatteryTouchingAnAutocrafterPowersIt(GameTestHelper h) {
        h.setBlock(CRAFTER, ModBlocks.AUTOCRAFTER);
        BlockPos battery = CRAFTER.north();
        h.setBlock(battery, ModBlocks.IRON_BATTERY);
        h.getBlockEntity(battery, net.nexium.energy.BatteryBlockEntity.class).energy().set(100_000);
        AutocrafterBlockEntity crafter = h.getBlockEntity(CRAFTER, AutocrafterBlockEntity.class);
        h.succeedWhen(() -> h.assertTrue(crafter.energy().stored() > 0, "the Autocrafter has been given energy"));
    }

    private static net.minecraft.world.item.crafting.RecipeHolder<?> recipe(GameTestHelper h, String id) {
        return h.getLevel().recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.withDefaultNamespace(id))).orElseThrow();
    }

    @GameTest
    public void aRecipeFromJeiFillsTheEncoder(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, ModBlocks.PATTERN_ENCODER);
        PatternEncoderBlockEntity be = h.getBlockEntity(at, PatternEncoderBlockEntity.class);
        var player = h.makeMockServerPlayerInLevel();
        PatternEncoderMenu menu = new PatternEncoderMenu(1, player.getInventory(), be);
        // Crafting mode: the sticks recipe is laid out and its result follows
        h.assertTrue(menu.fillFrom(recipe(h, "stick")), "a crafting recipe fills the grid");
        h.assertValueEqual(be.slots().getItem(0).getItem().toString().contains("planks"), true, "planks in the top left: " + be.slots().getItem(0));
        h.assertValueEqual(be.slots().getItem(3).getItem().toString().contains("planks"), true, "and below it: " + be.slots().getItem(3));
        h.assertValueEqual(be.slots().getItem(PatternEncoderBlockEntity.INPUTS).getItem(), Items.STICK, "the result is sticks");
        h.assertValueEqual(be.slots().getItem(PatternEncoderBlockEntity.INPUTS).getCount(), 4, "four of them");
        // A smelting recipe needs processing mode
        h.assertTrue(!menu.fillFrom(recipe(h, "iron_ingot_from_smelting_raw_iron")), "a smelting recipe does not fill a crafting template");
        be.setProcessing(true);
        h.assertTrue(menu.fillFrom(recipe(h, "iron_ingot_from_smelting_raw_iron")), "but fills a processing template");
        h.assertValueEqual(be.slots().getItem(0).getItem(), Items.RAW_IRON, "raw iron in");
        h.assertValueEqual(be.slots().getItem(PatternEncoderBlockEntity.INPUTS).getItem(), Items.IRON_INGOT, "an iron ingot out");
        // A crafting recipe in processing mode adds identical ingredients up: the sticks need two planks
        h.assertTrue(menu.fillFrom(recipe(h, "stick")), "a crafting recipe fills a processing template too");
        h.assertValueEqual(be.slots().getItem(0).getCount(), 2, "two planks as one input");
        h.assertTrue(be.slots().getItem(1).isEmpty(), "in one slot");
        h.succeed();
    }

    @GameTest
    public void theEncoderWritesPatterns(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, ModBlocks.PATTERN_ENCODER);
        PatternEncoderBlockEntity be = h.getBlockEntity(at, PatternEncoderBlockEntity.class);
        var player = h.makeMockServerPlayerInLevel();
        PatternEncoderMenu menu = new PatternEncoderMenu(1, player.getInventory(), be);
        be.slots().setItem(PatternEncoderBlockEntity.BLANK, new ItemStack(ModItems.BLANK_PATTERN, 2));
        h.assertTrue(!menu.encode(), "an empty recipe cannot be encoded");
        be.slots().setItem(0, new ItemStack(Items.OAK_LOG));
        be.slots().setItem(PatternEncoderBlockEntity.INPUTS, new ItemStack(Items.OAK_PLANKS, 4));
        h.assertTrue(menu.encode(), "a recipe encodes");
        ItemStack made = be.slots().getItem(PatternEncoderBlockEntity.ENCODED);
        h.assertTrue(PatternItem.isEncoded(made), "the pattern is encoded");
        h.assertValueEqual(be.slots().getItem(PatternEncoderBlockEntity.BLANK).getCount(), 1, "one blank was used");
        h.assertTrue(made.get(ModComponents.PATTERN).valid(), "and it is valid");
        h.succeed();
    }
}
