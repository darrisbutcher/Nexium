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
