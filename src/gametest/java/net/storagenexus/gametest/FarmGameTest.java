package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.farm.FarmBlock;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.farm.FarmRecipe;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The other one-block farms: what each one makes. */
public class FarmGameTest {
    private static final BlockPos FARM = new BlockPos(4, 2, 4);

    // ---------------------------------------------------------------- helpers






    private static FarmBlockEntity farm(GameTestHelper h, FarmKind kind) {
        h.setBlock(FARM, ModBlocks.FARMS.get(kind));
        return h.getBlockEntity(FARM, FarmBlockEntity.class);
    }

    private static void tick(GameTestHelper h, FarmBlockEntity farm, int ticks) {
        for (int i = 0; i < ticks; i++) FarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
    }

    private static int count(FarmBlockEntity farm, Item item) {
        int n = 0;
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) if (farm.getItem(i).is(item)) n += farm.getItem(i).getCount();
        return n;
    }

    private static void cycles(GameTestHelper h, FarmBlockEntity farm, int n) {
        for (int i = 0; i < n; i++) farm.cycle(h.getLevel(), farm.kind(), h.getLevel().getRandom());
    }

    // ---------------------------------------------------------------- what they make

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void zombifiedPiglinMakesNuggetsAndRareIngotsButNoFlesh(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.ZOMBIFIED_PIGLIN);
        cycles(h, farm, 300);
        int nuggets = count(farm, Items.GOLD_NUGGET), ingots = count(farm, Items.GOLD_INGOT);
        // Looting V: 0-1 plus 0-5 nuggets (about 3 a kill) and a 7.5% ingot
        h.assertTrue(nuggets > 600 && nuggets < 1200, "about 3 nuggets a kill: " + nuggets);
        h.assertTrue(ingots >= 4 && ingots <= 50, "about 22 ingots in 300 kills: " + ingots);
        h.assertValueEqual(count(farm, Items.ROTTEN_FLESH), 0, "no rotten flesh");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void witherSkeletonMakesCoalBonesAndSkulls(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.WITHER_SKELETON);
        cycles(h, farm, 300);
        h.assertTrue(count(farm, Items.COAL) > 100 && count(farm, Items.COAL) < 200, "about half a coal a kill: " + count(farm, Items.COAL));
        h.assertTrue(count(farm, Items.BONE) > 240 && count(farm, Items.BONE) < 360, "about a bone a kill: " + count(farm, Items.BONE));
        h.assertTrue(count(farm, Items.WITHER_SKELETON_SKULL) >= 3 && count(farm, Items.WITHER_SKELETON_SKULL) <= 55,
                "about 22 skulls: " + count(farm, Items.WITHER_SKELETON_SKULL));
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void shulkerFarmMakesShells(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.SHULKER);
        cycles(h, farm, 300);
        int shells = count(farm, Items.SHULKER_SHELL);
        h.assertTrue(shells > 200 && shells < 290, "about 4 shells in 5 kills: " + shells);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void witchFarmMakesAWitchsDrops(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.WITCH);
        cycles(h, farm, 100);
        // The vanilla table: up to 2 each of seven things, about 3 kinds a kill, nothing else
        java.util.Set<net.minecraft.world.item.Item> allowed = java.util.Set.of(Items.GLOWSTONE_DUST, Items.SUGAR, Items.SPIDER_EYE,
                Items.GLASS_BOTTLE, Items.GUNPOWDER, Items.STICK, Items.REDSTONE);
        int total = 0;
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) {
            ItemStack stack = farm.getItem(i);
            if (stack.isEmpty()) continue;
            h.assertTrue(allowed.contains(stack.getItem()), "only a witch's drops, got " + stack);
            total += stack.getCount();
        }
        h.assertTrue(total > 400 && total < 1300, "100 witches drop several hundred items (about 8 each): " + total);
        h.assertTrue(count(farm, Items.REDSTONE) > 0 && count(farm, Items.GLOWSTONE_DUST) > 0, "redstone and glowstone dust among them");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void cropFarmsMakeTheirCrop(GameTestHelper h) {
        FarmBlockEntity cane = farm(h, FarmKind.SUGAR_CANE);
        cycles(h, cane, 10);
        h.assertValueEqual(count(cane, Items.SUGAR_CANE), 30, "3 sugar cane a cycle");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity bamboo = farm(h, FarmKind.BAMBOO);
        cycles(h, bamboo, 10);
        h.assertValueEqual(count(bamboo, Items.BAMBOO), 40, "4 bamboo a cycle");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity wart = farm(h, FarmKind.NETHER_WART);
        cycles(h, wart, 10);
        h.assertValueEqual(count(wart, Items.NETHER_WART), 40, "4 nether wart a cycle");
        h.succeed();
    }

    private static java.util.Set<net.minecraft.world.item.Item> items(FarmBlockEntity farm) {
        java.util.Set<net.minecraft.world.item.Item> out = new java.util.HashSet<>();
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) if (!farm.getItem(i).isEmpty()) out.add(farm.getItem(i).getItem());
        return out;
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 60)
    public void mobFarmsMakeTheirMobsLootAsIfAPlayerKilledThem(GameTestHelper h) {
        // Zombie, skeleton, spider, creeper and the raiders
        FarmBlockEntity monster = farm(h, FarmKind.MONSTER);
        cycles(h, monster, 30);
        java.util.Set<net.minecraft.world.item.Item> got = items(monster);
        h.assertTrue(got.contains(Items.ROTTEN_FLESH) && got.contains(Items.BONE) && got.contains(Items.STRING) && got.contains(Items.GUNPOWDER),
                "a monster mix of flesh, bones, string and gunpowder, got " + got);
        h.assertTrue(got.contains(Items.SPIDER_EYE) || got.contains(Items.EMERALD) || got.contains(Items.IRON_INGOT) || got.contains(Items.OMINOUS_BOTTLE),
                "player-kill drops among them, got " + got);
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity nether = farm(h, FarmKind.NETHER_MOB);
        cycles(h, nether, 40);
        got = items(nether);
        h.assertTrue(got.contains(Items.BLAZE_ROD) && got.contains(Items.GHAST_TEAR) && got.contains(Items.MAGMA_CREAM),
                "blaze rods (a player kill), ghast tears and magma cream, got " + got);
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity ocean = farm(h, FarmKind.OCEAN);
        cycles(h, ocean, 40);
        got = items(ocean);
        h.assertTrue(got.contains(Items.PRISMARINE_SHARD) && got.contains(Items.COD) && got.contains(Items.ROTTEN_FLESH), "prismarine, fish and flesh, got " + got);
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity slime = farm(h, FarmKind.SLIME);
        cycles(h, slime, 20);
        h.assertTrue(count(slime, Items.SLIME_BALL) > 5, "slime balls: " + count(slime, Items.SLIME_BALL));
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void livestockMakesWhatItsFeedPicks(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.LIVESTOCK);
        h.assertTrue(farm.canPlaceItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.WHEAT)), "wheat is feed");
        h.assertTrue(farm.canPlaceItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.PUMPKIN_SEEDS)), "so are seeds");
        h.assertTrue(!farm.canPlaceItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.DIAMOND)), "a diamond is not");
        farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.WHEAT, 20));
        cycles(h, farm, 20);
        h.assertValueEqual(farm.getItem(FarmBlockEntity.INPUT_SLOT).getCount(), 0, "one wheat a cycle");
        h.assertTrue(count(farm, Items.BEEF) > 0 && count(farm, Items.LEATHER) > 0 && count(farm, Items.MUTTON) > 0, "cow and sheep drops from wheat");
        h.assertTrue(count(farm, Items.PORKCHOP) == 0 && count(farm, Items.CHICKEN) == 0, "and no pig or chicken");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        farm = farm(h, FarmKind.LIVESTOCK);
        farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.WHEAT_SEEDS, 20));
        cycles(h, farm, 20);
        h.assertTrue(count(farm, Items.CHICKEN) > 0 && count(farm, Items.FEATHER) > 0, "chicken drops from seeds");
        h.assertValueEqual(count(farm, Items.EGG), 20, "and an egg a cycle");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        farm = farm(h, FarmKind.LIVESTOCK);
        farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.CARROT, 30));
        cycles(h, farm, 30);
        h.assertTrue(count(farm, Items.PORKCHOP) > 0 && count(farm, Items.RABBIT) > 0, "pig and rabbit drops from carrots");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void cropGourdGeneratorAndMossMakeWhatTheyShould(GameTestHelper h) {
        FarmBlockEntity crop = farm(h, FarmKind.CROP);
        cycles(h, crop, 4);
        h.assertValueEqual(count(crop, Items.WHEAT), 12, "3 wheat a cycle");
        h.assertValueEqual(count(crop, Items.CARROT), 12, "3 carrots");
        h.assertValueEqual(count(crop, Items.POTATO), 12, "3 potatoes");
        h.assertValueEqual(count(crop, Items.BEETROOT), 12, "3 beetroot");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity gourd = farm(h, FarmKind.GOURD);
        h.assertValueEqual(FarmKind.GOURD.modes().size(), 4, "melon, pumpkin, cactus and kelp");
        cycles(h, gourd, 1);
        h.assertValueEqual(count(gourd, Items.MELON_SLICE), 12, "melon mode: 12 slices");
        gourd.cycleMode();
        cycles(h, gourd, 1);
        h.assertValueEqual(count(gourd, Items.PUMPKIN), 3, "pumpkin mode: 3");
        gourd.cycleMode(); gourd.cycleMode();
        cycles(h, gourd, 1);
        h.assertValueEqual(count(gourd, Items.KELP), 6, "kelp mode: 6");
        gourd.cycleMode();
        h.assertValueEqual(gourd.mode(), 0, "the modes go round");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity gen = farm(h, FarmKind.GENERATOR);
        cycles(h, gen, 1);
        h.assertValueEqual(count(gen, Items.COBBLESTONE), 8, "cobblestone: 8");
        gen.setMode(FarmKind.GENERATOR.modes().indexOf("obsidian"));
        h.assertValueEqual(gen.cycleTicks(), 2 * FarmKind.GENERATOR.cycleTicks(), "obsidian takes twice as long");
        cycles(h, gen, 1);
        h.assertValueEqual(count(gen, Items.OBSIDIAN), 1, "and gives 1");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity moss = farm(h, FarmKind.MOSS);
        cycles(h, moss, 10);
        h.assertValueEqual(count(moss, Items.MOSS_BLOCK), 30, "3 moss blocks a cycle");
        h.assertValueEqual(count(moss, Items.MOSS_CARPET), 20, "2 carpets");
        h.assertTrue(count(moss, Items.AZALEA) + count(moss, Items.FLOWERING_AZALEA) <= 10, "the odd azalea");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void boneMealIsNeededByMossAndSpeedsUpCrops(GameTestHelper h) {
        FarmBlockEntity moss = farm(h, FarmKind.MOSS);
        tick(h, moss, 1);
        h.assertTrue(moss.isStarved(), "the Moss Farm waits for bone meal");
        moss.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BONE_MEAL, 2));
        tick(h, moss, 1);
        h.assertTrue(!moss.isStarved(), "and runs with it");
        h.assertValueEqual(moss.getItem(FarmBlockEntity.INPUT_SLOT).getCount(), 1, "one bone meal used");
        h.assertValueEqual(moss.boostLeft(), 3, "for 3 cycles");
        tick(h, moss, 3 * FarmKind.MOSS.cycleTicks());
        h.assertValueEqual(count(moss, Items.MOSS_BLOCK), 9, "3 cycles of moss on one bone meal");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity crop = farm(h, FarmKind.CROP);
        tick(h, crop, 1);
        h.assertTrue(!crop.isStarved(), "the Crop Farm needs no bone meal");
        tick(h, crop, FarmKind.CROP.cycleTicks() - 1);
        h.assertValueEqual(count(crop, Items.WHEAT), 3, "one cycle at the normal speed");
        crop.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BONE_MEAL, 1));
        tick(h, crop, FarmKind.CROP.cycleTicks() / 2);
        h.assertValueEqual(count(crop, Items.WHEAT), 6, "a cycle in half the time with bone meal");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void beeFarmMakesCombAndHoneyInATank(GameTestHelper h) {
        FarmBlockEntity bees = farm(h, FarmKind.BEE);
        cycles(h, bees, 10);
        h.assertTrue(count(bees, Items.HONEYCOMB) >= 10 && count(bees, Items.HONEYCOMB) <= 30, "1 to 3 honeycomb a cycle: " + count(bees, Items.HONEYCOMB));
        h.assertValueEqual(bees.honey().stored(), 20 * net.storagenexus.essentials.farm.HoneyTank.BOTTLE, "2 bottles of honey a cycle in the tank");
        // Pipes can take it out and not put it in
        var outside = bees.honey().outside();
        h.assertTrue(!outside.supportsInsertion(), "nothing goes in from outside");
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long out = outside.extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.storagenexus.essentials.registry.ModFluids.HONEY),
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, tx);
            h.assertValueEqual(out, net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, "a bucket of honey through a pipe");
            tx.commit();
        }
        // A glass bottle takes a bottle of honey
        h.assertTrue(bees.honey().takeBottle(), "a bottle of honey");
        h.assertValueEqual(bees.honey().stored(), 20 * net.storagenexus.essentials.farm.HoneyTank.BOTTLE - net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET
                - net.storagenexus.essentials.farm.HoneyTank.BOTTLE, "taken from the tank");
        // And it survives a save
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        bees.saveWithFullMetadata(out);
        FarmBlockEntity loaded = new FarmBlockEntity(bees.getBlockPos(), bees.getBlockState());
        loaded.setLevel(h.getLevel());
        loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertValueEqual(loaded.honey().stored(), bees.honey().stored(), "the honey is saved with the farm");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void theNewFarmsAndTheBreederRunOnTheirTimes(GameTestHelper h) {
        // The defaults (the test world's saved config has already been moved on from the old 24000, too)
        h.assertValueEqual(new net.storagenexus.essentials.config.NexusConfig().villagerBreederTicks, 12000, "the breeder: 10 minutes");
        h.assertValueEqual(FarmKind.VILLAGER_BREEDER.cycleTicks(), 12000, "and an old config moved to it");
        h.assertValueEqual(FarmKind.WITCH.cycleTicks(), 200, "witches: 10 seconds");
        h.assertValueEqual(FarmKind.SUGAR_CANE.cycleTicks(), 400, "cane: 20 seconds");
        h.assertValueEqual(FarmKind.BAMBOO.cycleTicks(), 400, "bamboo: 20 seconds");
        h.assertValueEqual(FarmKind.NETHER_WART.cycleTicks(), 600, "wart: 30 seconds");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void piglinBarterTradesGoldForBarterLoot(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.PIGLIN_BARTER);
        farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.GOLD_INGOT, 40));
        cycles(h, farm, 25);
        h.assertValueEqual(farm.getItem(FarmBlockEntity.INPUT_SLOT).getCount(), 15, "one gold ingot per barter");
        int stacks = 0;
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) if (!farm.getItem(i).isEmpty()) stacks++;
        h.assertTrue(stacks > 0, "bartering gave loot");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void villagerBreederMakesABabyVillagerFromFood(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.VILLAGER_BREEDER);
        farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BREAD, 7));
        cycles(h, farm, 1);
        h.assertValueEqual(farm.getItem(FarmBlockEntity.INPUT_SLOT).getCount(), 4, "3 bread per villager");
        ItemStack baby = farm.getItem(0);
        h.assertTrue(baby.is(ModItems.CAPTURED_MOB), "the baby is a Captured Mob");
        CapturedMob mob = baby.get(ModComponents.CAPTURED_MOB);
        h.assertTrue(mob != null && mob.type() == EntityTypes.VILLAGER, "a villager");
        h.assertTrue(mob.data().toString().contains("Age"), "its data has an age");
        h.assertTrue(mob.data().getIntOr("Age", 0) < 0, "a baby: age " + mob.data().getIntOr("Age", 0));
        // 12 carrots do the same
        farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.CARROT, 13));
        cycles(h, farm, 1);
        h.assertValueEqual(farm.getItem(FarmBlockEntity.INPUT_SLOT).getCount(), 1, "12 carrots per villager");
        h.succeed();
    }

    // ---------------------------------------------------------------- when they work

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 400)
    public void cycleLengthComesFromTheConfig(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.ZOMBIFIED_PIGLIN);
        int per = NexusConfig.get().zombifiedPiglinTicks;
        tick(h, farm, per - 1);
        h.assertTrue(farm.progressFraction() > 0.9F, "almost there");
        h.assertValueEqual(count(farm, Items.GOLD_NUGGET) + count(farm, Items.GOLD_INGOT), 0, "nothing yet");
        // Keep going until something drops (a cycle can give no nuggets)
        for (int round = 0; round < 20 && count(farm, Items.GOLD_NUGGET) + count(farm, Items.GOLD_INGOT) == 0; round++) tick(h, farm, per);
        h.assertTrue(count(farm, Items.GOLD_NUGGET) + count(farm, Items.GOLD_INGOT) > 0, "the cycles made gold");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void farmsWithInputWaitForIt(GameTestHelper h) {
        FarmBlockEntity breeder = farm(h, FarmKind.VILLAGER_BREEDER);
        tick(h, breeder, 50);
        h.assertValueEqual(breeder.progress(), 0, "no food, no progress");
        h.assertTrue(breeder.isStarved(), "starved");
        breeder.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BREAD, 2));
        tick(h, breeder, 5);
        h.assertValueEqual(breeder.progress(), 0, "2 bread is not enough for one villager");
        breeder.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BREAD, 3));
        tick(h, breeder, 5);
        h.assertValueEqual(breeder.progress(), 5, "3 bread: it works");
        h.assertTrue(!breeder.canPlaceItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.DIAMOND)), "only food goes in");
        h.assertTrue(breeder.canPlaceItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BEETROOT)), "beetroot does");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void redstoneAndAFullOutputPauseIt(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.SHULKER);
        tick(h, farm, 10);
        h.assertValueEqual(farm.progress(), 10, "working");
        h.setBlock(FARM, ModBlocks.FARMS.get(FarmKind.SHULKER).defaultBlockState().setValue(FarmBlock.POWERED, true));
        FarmBlockEntity powered = h.getBlockEntity(FARM, FarmBlockEntity.class);
        tick(h, powered, 10);
        h.assertValueEqual(powered.progress(), 10, "a redstone signal pauses it: the progress stays");
        // A full output
        FarmBlockEntity full = farm(h, FarmKind.SHULKER);
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) full.setItem(i, new ItemStack(Items.DIRT, 64));
        int before = full.progress();
        tick(h, full, 10);
        h.assertValueEqual(full.progress(), before, "a full output waits");
        h.assertTrue(full.isFull(), "and says so");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void hoppersTakeFromBelowAndFeedFromAbove(GameTestHelper h) {
        FarmBlockEntity piglin = farm(h, FarmKind.PIGLIN_BARTER);
        h.assertTrue(piglin.getSlotsForFace(Direction.DOWN).length == FarmBlockEntity.OUTPUT_SLOTS, "the output from below");
        h.assertTrue(piglin.getSlotsForFace(Direction.UP).length == 1 && piglin.getSlotsForFace(Direction.UP)[0] == FarmBlockEntity.INPUT_SLOT, "the input from above");
        h.assertTrue(piglin.canPlaceItemThroughFace(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.GOLD_INGOT), Direction.NORTH), "gold goes in from the side");
        h.assertTrue(!piglin.canPlaceItemThroughFace(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.GOLD_INGOT), Direction.DOWN), "not from below");
        h.assertTrue(!piglin.canTakeItemThroughFace(0, ItemStack.EMPTY, Direction.UP), "no taking from above");
        FarmBlockEntity shulker = farm(h, FarmKind.SHULKER);
        h.assertTrue(shulker.getSlotsForFace(Direction.UP).length == 0, "a farm with no input has no input side");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void everyFarmHasABlockAnItemAndAName(GameTestHelper h) {
        for (FarmKind kind : FarmKind.values()) {
            h.assertTrue(ModBlocks.FARMS.get(kind).kind() == kind, kind + " block");
            h.assertTrue(ModItems.FARMS.get(kind).getDescriptionId().endsWith(kind.id()), kind + " item");
            h.assertTrue(ModItems.TAB_ORDER.contains(ModItems.FARMS.get(kind)), kind + " is in the creative tab");
        }
        h.succeed();
    }
}
