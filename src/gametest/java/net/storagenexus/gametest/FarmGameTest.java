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

/** The other one-block farms: the named chests that make them, and what each one makes. */
public class FarmGameTest {
    private static final BlockPos LEFT = new BlockPos(2, 1, 2), RIGHT = new BlockPos(3, 1, 2);
    private static final BlockPos FARM = new BlockPos(4, 2, 4);

    // ---------------------------------------------------------------- helpers

    private static List<ChestBlockEntity> doubleChest(GameTestHelper h, String name) {
        h.setBlock(LEFT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        h.setBlock(RIGHT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        List<ChestBlockEntity> halves = List.of(h.getBlockEntity(LEFT, ChestBlockEntity.class), h.getBlockEntity(RIGHT, ChestBlockEntity.class));
        ItemStack named = new ItemStack(Items.CHEST);
        named.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        halves.get(0).applyComponentsFromItemStack(named);
        return halves;
    }

    private static void fill(List<ChestBlockEntity> halves, List<ItemStack> stacks) {
        int i = 0;
        for (ChestBlockEntity half : halves) {
            for (int slot = 0; slot < half.getContainerSize(); slot++, i++) {
                half.setItem(slot, i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
            }
            half.setChanged();
        }
    }

    private static ItemStack captured(EntityType<?> type) {
        return MobCapture.toItem(new CapturedMob(type, new CompoundTag(), Optional.empty(), 20.0F, 20.0F, false));
    }

    private static void add(List<ItemStack> list, Item item, int count) {
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            list.add(new ItemStack(item, n));
            count -= n;
        }
    }

    /** The recipe's materials for a kind, with mobs as spawn eggs or as Captured Mobs, then the filler. */
    private static List<ItemStack> recipe(FarmKind kind, boolean capturedMobs) {
        List<ItemStack> list = new ArrayList<>();
        switch (kind) {
            case VILLAGER_BREEDER -> {
                if (capturedMobs) for (int i = 0; i < 2; i++) list.add(captured(EntityTypes.VILLAGER));
                else list.add(new ItemStack(Items.VILLAGER_SPAWN_EGG, 2));
                for (int i = 0; i < 4; i++) list.add(new ItemStack(i % 2 == 0 ? Items.BED.red() : Items.BED.white()));
                add(list, Items.COMPOSTER, 2);
                add(list, Items.HAY_BLOCK, 8);
                add(list, Items.EMERALD_BLOCK, 2);
                while (list.size() < FarmRecipe.CHEST_SLOTS) list.add(new ItemStack(Items.OAK_PLANKS, 64));
            }
            case PIGLIN_BARTER -> {
                if (capturedMobs) for (int i = 0; i < 2; i++) list.add(captured(EntityTypes.PIGLIN));
                else list.add(new ItemStack(Items.PIGLIN_SPAWN_EGG, 2));
                add(list, Items.GOLD_BLOCK, 9);
                add(list, Items.SOUL_SAND, 16);
                add(list, Items.CRIMSON_PLANKS, 8);
                add(list, Items.BIRCH_PLANKS, 8);
                while (list.size() < FarmRecipe.CHEST_SLOTS) list.add(new ItemStack(Items.NETHERRACK, 64));
            }
            case ZOMBIFIED_PIGLIN -> {
                if (capturedMobs) for (int i = 0; i < 4; i++) list.add(captured(EntityTypes.ZOMBIFIED_PIGLIN));
                else list.add(new ItemStack(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, 4));
                add(list, Items.MAGMA_BLOCK, 8);
                add(list, Items.NETHER_BRICKS, 20);
                add(list, Items.GOLD_INGOT, 16);
                while (list.size() < FarmRecipe.CHEST_SLOTS) list.add(new ItemStack(Items.NETHERRACK, 64));
            }
            case WITHER_SKELETON -> {
                if (capturedMobs) for (int i = 0; i < 4; i++) list.add(captured(EntityTypes.WITHER_SKELETON));
                else list.add(new ItemStack(Items.WITHER_SKELETON_SPAWN_EGG, 4));
                add(list, Items.NETHER_BRICK_FENCE, 16);
                add(list, Items.SOUL_SAND, 8);
                add(list, Items.COAL_BLOCK, 4);
                add(list, Items.MAGMA_BLOCK, 4);
                while (list.size() < FarmRecipe.CHEST_SLOTS) list.add(new ItemStack(Items.NETHERRACK, 64));
            }
            case SHULKER -> {
                if (capturedMobs) for (int i = 0; i < 4; i++) list.add(captured(EntityTypes.SHULKER));
                else list.add(new ItemStack(Items.SHULKER_SPAWN_EGG, 4));
                add(list, Items.PURPUR_BLOCK, 32);
                add(list, Items.END_ROD, 8);
                add(list, Items.ENDER_PEARL, 8);
                while (list.size() < FarmRecipe.CHEST_SLOTS) list.add(new ItemStack(Items.END_STONE_BRICKS, 64));
            }
        }
        return list;
    }

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

    // ---------------------------------------------------------------- the chests

    private static void chestBecomes(GameTestHelper h, FarmKind kind, String name, boolean capturedMobs) {
        fill(doubleChest(h, name), recipe(kind, capturedMobs));
        h.runAfterDelay(2, () -> {
            h.assertBlockNotPresent(Blocks.CHEST, LEFT);
            h.assertBlockNotPresent(Blocks.CHEST, RIGHT);
            h.assertItemEntityPresent(ModItems.FARMS.get(kind), LEFT, 2.0);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(LEFT)).inflate(3)).size() == 1,
                    "only the farm dropped");
            h.succeed();
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void villagerBreederChest(GameTestHelper h) {
        chestBecomes(h, FarmKind.VILLAGER_BREEDER, "Nexus Villager Breeder", false);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void piglinBarterChest(GameTestHelper h) {
        chestBecomes(h, FarmKind.PIGLIN_BARTER, "Nexus Piglin Barter", true);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void zombifiedPiglinChest(GameTestHelper h) {
        chestBecomes(h, FarmKind.ZOMBIFIED_PIGLIN, " nexus ZOMBIFIED piglin ", false);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void witherSkeletonChest(GameTestHelper h) {
        chestBecomes(h, FarmKind.WITHER_SKELETON, "Nexus Wither Skeleton Farm", true);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void shulkerChest(GameTestHelper h) {
        chestBecomes(h, FarmKind.SHULKER, "Nexus Shulker Farm", false);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void wrongChestStaysAndRecipesDoNotMix(GameTestHelper h) {
        // The shulker recipe in a chest named for the wither skeleton farm: nothing happens
        fill(doubleChest(h, "Nexus Wither Skeleton Farm"), recipe(FarmKind.SHULKER, false));
        FarmRecipe wither = FarmRecipe.of(FarmKind.WITHER_SKELETON);
        FarmRecipe.Check check = wither.check(recipe(FarmKind.SHULKER, false));
        h.assertTrue(!wither.complete(check), "another farm's materials are not this recipe");
        h.assertTrue(!wither.problems(check).isEmpty(), "the player is told what's wrong");
        // One short: three wither skeletons
        List<ItemStack> stacks = recipe(FarmKind.WITHER_SKELETON, false);
        stacks.set(0, new ItemStack(Items.WITHER_SKELETON_SPAWN_EGG, 3));
        stacks.add(0, new ItemStack(Items.NETHERRACK, 64));
        stacks.remove(stacks.size() - 1);
        h.assertTrue(!wither.complete(wither.check(stacks)), "a skeleton short");
        h.assertTrue(wither.complete(wither.check(recipe(FarmKind.WITHER_SKELETON, false))), "the full recipe is complete");
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.CHEST, LEFT);
            h.succeed();
        });
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
