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
        for (int i = 0; i < ticks; i++) {
            farm.energy().set(Long.MAX_VALUE); // always powered; the power tests tick the farm themselves
            FarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
        }
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
    public void livestockMakesEveryAnimalsDropsWithNothingFed(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.LIVESTOCK);
        h.assertTrue(!farm.canPlaceItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.WHEAT)), "no feed goes in");
        cycles(h, farm, 20);
        h.assertTrue(count(farm, Items.BEEF) > 0 && count(farm, Items.LEATHER) > 0 && count(farm, Items.MUTTON) > 0, "cow and sheep drops");
        h.assertTrue(count(farm, Items.CHICKEN) > 0 && count(farm, Items.FEATHER) > 0, "chicken drops");
        h.assertValueEqual(count(farm, Items.EGG), 20, "an egg a cycle");
        h.assertTrue(count(farm, Items.PORKCHOP) > 0 && count(farm, Items.RABBIT) > 0, "pig and rabbit drops");
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
        h.assertTrue(FarmKind.GOURD.modes().isEmpty(), "no mode button");
        cycles(h, gourd, 1);
        h.assertValueEqual(count(gourd, Items.MELON_SLICE), 12, "12 melon slices");
        h.assertValueEqual(count(gourd, Items.PUMPKIN), 3, "3 pumpkins");
        h.assertValueEqual(count(gourd, Items.CACTUS), 4, "4 cactus");
        h.assertValueEqual(count(gourd, Items.KELP), 6, "6 kelp, all in the same cycle");
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
        h.assertValueEqual(count(moss, Items.PALE_MOSS_BLOCK), 30, "3 pale moss blocks a cycle");
        h.assertValueEqual(count(moss, Items.PALE_MOSS_CARPET), 20, "2 pale carpets");
        h.assertValueEqual(count(moss, Items.PALE_HANGING_MOSS), 20, "2 pale hanging moss");
        h.assertValueEqual(count(moss, Items.RESIN_CLUMP), 60, "6 resin clumps a cycle");
        h.assertTrue(count(moss, Items.AZALEA) + count(moss, Items.FLOWERING_AZALEA) <= 10, "the odd azalea");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void mossAndCropFarmsRunOnPowerAlone(GameTestHelper h) {
        FarmBlockEntity moss = farm(h, FarmKind.MOSS);
        tick(h, moss, 1);
        h.assertTrue(!moss.isStarved(), "the Moss Farm needs no bone meal");
        tick(h, moss, 3 * FarmKind.MOSS.cycleTicks());
        h.assertTrue(count(moss, Items.MOSS_BLOCK) >= 9, "it makes moss");
        h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
        FarmBlockEntity crop = farm(h, FarmKind.CROP);
        tick(h, crop, FarmKind.CROP.cycleTicks());
        h.assertValueEqual(count(crop, Items.WHEAT), 3, "one cycle at the normal speed");
        for (FarmKind kind : FarmKind.values()) h.assertTrue(!kind.hasInput(), kind + " has no input");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void beeFarmMakesCombAndHoneyBottles(GameTestHelper h) {
        FarmBlockEntity bees = farm(h, FarmKind.BEE);
        cycles(h, bees, 10);
        h.assertTrue(count(bees, Items.HONEYCOMB) >= 10 && count(bees, Items.HONEYCOMB) <= 30, "1 to 3 honeycomb a cycle: " + count(bees, Items.HONEYCOMB));
        h.assertValueEqual(count(bees, Items.HONEY_BOTTLE), 20, "2 bottles of honey a cycle in the output");
        // A farm saved with 1.19's honey tank (a quarter bucket a bottle) hands it back as bottles
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        bees.saveWithFullMetadata(out);
        out.putLong("honey", 5 * (net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET / 4));
        FarmBlockEntity loaded = new FarmBlockEntity(bees.getBlockPos(), bees.getBlockState());
        loaded.setLevel(h.getLevel());
        loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertValueEqual(count(loaded, Items.HONEY_BOTTLE), 25, "the old tank comes back as 5 bottles");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void treeFarmMakesTheLogsOfItsTreeOrChorus(GameTestHelper h) {
        h.assertValueEqual(FarmKind.TREE.modes().size(), 12, "eleven woods and chorus");
        h.assertValueEqual(FarmKind.TREE.cycleTicks(), 400, "every 20 seconds");
        FarmBlockEntity trees = farm(h, FarmKind.TREE);
        for (String tree : FarmKind.TREE.modes()) {
            if (tree.equals("chorus")) continue;
            trees.setMode(FarmKind.TREE.modes().indexOf(tree));
            for (int i = 0; i < trees.getContainerSize(); i++) trees.setItem(i, ItemStack.EMPTY);
            cycles(h, trees, 2);
            h.assertValueEqual(count(trees, FarmKind.treeLog(tree)), 16, tree + ": 8 logs a cycle");
            int others = 0;
            for (int i = 0; i < trees.getContainerSize(); i++) if (!trees.getItem(i).isEmpty() && !trees.getItem(i).is(FarmKind.treeLog(tree))) others++;
            h.assertValueEqual(others, 0, tree + " makes only its own log");
        }
        trees.setMode(FarmKind.TREE.modes().indexOf("chorus"));
        for (int i = 0; i < trees.getContainerSize(); i++) trees.setItem(i, ItemStack.EMPTY);
        cycles(h, trees, 200);
        h.assertValueEqual(count(trees, Items.CHORUS_FRUIT), 800, "4 chorus fruit a cycle");
        int flowers = count(trees, Items.CHORUS_FLOWER);
        h.assertTrue(flowers > 0 && flowers < 40, "a chorus flower now and then: " + flowers + " in 200 cycles");
        trees.previousMode();
        h.assertTrue(trees.mode() == FarmKind.TREE.modes().indexOf("warped"), "shift-click steps back");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void froglightFarmMakesOneOfEachColour(GameTestHelper h) {
        FarmBlockEntity lights = farm(h, FarmKind.FROGLIGHT);
        cycles(h, lights, 4);
        h.assertValueEqual(count(lights, Items.OCHRE_FROGLIGHT), 4, "an ochre froglight a cycle");
        h.assertValueEqual(count(lights, Items.VERDANT_FROGLIGHT), 4, "a verdant froglight a cycle");
        h.assertValueEqual(count(lights, Items.PEARLESCENT_FROGLIGHT), 4, "a pearlescent froglight a cycle");
        h.assertValueEqual(FarmKind.FROGLIGHT.cycleTicks(), 400, "every 20 seconds");
        h.succeed();
    }

    /**
     * Every farm says how many empty output slots a cycle needs. With exactly that many free and every other slot full
     * of something else, nothing a cycle makes may be left without room: 60 cycles of each kind, on each of its inputs.
     */
    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void noCycleMakesMoreThanItsFreeSlotsHold(GameTestHelper h) {
        for (FarmKind kind : FarmKind.values()) {
            List<ItemStack> inputs = List.of(ItemStack.EMPTY);
            int modes = Math.max(1, kind.modes().size());
            for (ItemStack input : inputs) {
                for (int mode = 0; mode < modes; mode++) {
                    FarmBlockEntity farm = farm(h, kind);
                    farm.setMode(mode);
                    for (int cycle = 0; cycle < 60; cycle++) {
                        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) {
                            farm.setItem(i, i < kind.slotsNeeded() ? ItemStack.EMPTY : new ItemStack(Items.BEDROCK, 64));
                        }
                        if (!input.isEmpty()) farm.setItem(FarmBlockEntity.INPUT_SLOT, input.copy());
                        farm.cycle(h.getLevel(), kind, h.getLevel().getRandom());
                    }
                    h.assertValueEqual(farm.overflow(), 0, kind + " (mode " + mode + ", input " + input.getItem() + "): items that found no room");
                    h.setBlock(FARM, net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void theMonsterFarmsTotemIsRare(GameTestHelper h) {
        h.assertValueEqual(FarmBlockEntity.TOTEM_CHANCE, 0.02F, "a 2% chance each cycle");
        FarmBlockEntity monster = farm(h, FarmKind.MONSTER);
        int totems = 0, cycles = 2000;
        for (int i = 0; i < cycles; i++) {
            for (int slot = 0; slot < FarmBlockEntity.OUTPUT_SLOTS; slot++) monster.setItem(slot, ItemStack.EMPTY);
            monster.cycle(h.getLevel(), FarmKind.MONSTER, h.getLevel().getRandom());
            totems += count(monster, Items.TOTEM_OF_UNDYING);
        }
        // 2% of 2000 is 40: far from the evoker's one every cycle, and never more than one a cycle
        h.assertTrue(totems >= 10 && totems <= 90, "about 40 totems in 2000 cycles, got " + totems);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void farmsNeedPower(GameTestHelper h) {
        FarmBlockEntity cane = farm(h, FarmKind.SUGAR_CANE);
        h.assertValueEqual(FarmKind.SUGAR_CANE.energyPerTick(), 10, "a plant farm uses 10 NE a tick");
        h.assertValueEqual(FarmKind.MONSTER.energyPerTick(), 20, "a mob farm uses 20");
        // With no energy it waits
        for (int i = 0; i < 50; i++) FarmBlockEntity.serverTick(h.getLevel(), cane.getBlockPos(), cane.getBlockState(), cane);
        h.assertValueEqual(cane.progress(), 0, "no work without power");
        h.assertTrue(cane.isUnpowered(), "and it says so");
        // 100 NE is 10 ticks of work, then it waits again
        h.assertValueEqual(cane.energy().insert(100, false), 100L, "energy goes in from outside");
        for (int i = 0; i < 50; i++) FarmBlockEntity.serverTick(h.getLevel(), cane.getBlockPos(), cane.getBlockState(), cane);
        h.assertValueEqual(cane.progress(), 10, "10 ticks of work on 100 NE");
        h.assertValueEqual(cane.energy().stored(), 0L, "all used");
        // The buffer holds ten seconds of work and gives nothing back out
        h.assertValueEqual(cane.energy().capacity(), 2000L, "200 ticks' worth");
        h.assertValueEqual(cane.energy().insert(5000, false), 2000L, "it fills up");
        h.assertValueEqual(cane.energy().extract(100, false), 0L, "a machine's energy can't be taken out");
        // A full output uses no energy
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) cane.setItem(i, new ItemStack(Items.DIRT, 64));
        FarmBlockEntity.serverTick(h.getLevel(), cane.getBlockPos(), cane.getBlockState(), cane);
        h.assertValueEqual(cane.energy().stored(), 2000L, "nothing used while stopped");
        // It is found by the energy lookup, and kept when saved
        h.assertTrue(net.storagenexus.energy.NexusEnergy.SIDED.find(h.getLevel(), cane.getBlockPos(), net.minecraft.core.Direction.UP) == cane.energy(), "conduits can find it");
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        cane.saveWithFullMetadata(out);
        FarmBlockEntity loaded = new FarmBlockEntity(cane.getBlockPos(), cane.getBlockState());
        loaded.setLevel(h.getLevel());
        loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertValueEqual(loaded.energy().stored(), 2000L, "the energy is saved");
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
    public void piglinBarterMakesBarterLootWithoutGold(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.PIGLIN_BARTER);
        cycles(h, farm, 25);
        int stacks = 0;
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) if (!farm.getItem(i).isEmpty()) stacks++;
        h.assertTrue(stacks > 0, "bartering gave loot");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void villagerBreederMakesABabyVillagerWithoutFood(GameTestHelper h) {
        FarmBlockEntity farm = farm(h, FarmKind.VILLAGER_BREEDER);
        cycles(h, farm, 1);
        ItemStack baby = farm.getItem(0);
        h.assertTrue(baby.is(ModItems.CAPTURED_MOB), "the baby is a Captured Mob");
        CapturedMob mob = baby.get(ModComponents.CAPTURED_MOB);
        h.assertTrue(mob != null && mob.type() == EntityTypes.VILLAGER, "a villager");
        h.assertTrue(mob.data().getIntOr("Age", 0) < 0, "a baby: age " + mob.data().getIntOr("Age", 0));
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
    public void everyFarmWorksOnPowerAlone(GameTestHelper h) {
        FarmBlockEntity breeder = farm(h, FarmKind.VILLAGER_BREEDER);
        tick(h, breeder, 5);
        h.assertValueEqual(breeder.progress(), 5, "no food needed: it works");
        h.assertTrue(!breeder.isStarved(), "never starved");
        // An older farm's input item is dropped, not kept
        breeder.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.BREAD, 3));
        tick(h, breeder, 1);
        h.assertTrue(breeder.getItem(FarmBlockEntity.INPUT_SLOT).isEmpty(), "leftover input is cleared out");
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
    public void hoppersTakeFromBelowAndNothingGoesIn(GameTestHelper h) {
        FarmBlockEntity piglin = farm(h, FarmKind.PIGLIN_BARTER);
        h.assertTrue(piglin.getSlotsForFace(Direction.DOWN).length == FarmBlockEntity.OUTPUT_SLOTS, "the output from below");
        h.assertTrue(piglin.getSlotsForFace(Direction.UP).length == 0, "nothing goes in from above");
        h.assertTrue(!piglin.canPlaceItemThroughFace(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.GOLD_INGOT), Direction.NORTH), "gold doesn't go in");
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
