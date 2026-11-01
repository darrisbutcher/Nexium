package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmChest;
import net.storagenexus.essentials.ironfarm.IronFarmMenu;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import net.storagenexus.essentials.ironfarm.IronFarmTimers;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IronFarmGameTest {
    /** A double chest facing north: the left half is west of the right half. */
    private static final BlockPos LEFT = new BlockPos(2, 1, 2), RIGHT = new BlockPos(3, 1, 2);
    private static final BlockPos FARM = new BlockPos(4, 2, 4);

    // ---------------------------------------------------------------- helpers

    private static List<ChestBlockEntity> doubleChest(GameTestHelper h, String name) {
        h.setBlock(LEFT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        h.setBlock(RIGHT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        List<ChestBlockEntity> halves = List.of(h.getBlockEntity(LEFT, ChestBlockEntity.class), h.getBlockEntity(RIGHT, ChestBlockEntity.class));
        if (name != null) name(halves.get(0), name);
        return halves;
    }

    /** Names a chest the way placing an anvil-renamed chest does. */
    private static void name(ChestBlockEntity chest, String name) {
        ItemStack named = new ItemStack(Items.CHEST);
        named.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        chest.applyComponentsFromItemStack(named);
    }

    private static void fill(List<ChestBlockEntity> halves, List<ItemStack> stacks) {
        int i = 0;
        for (ChestBlockEntity half : halves) {
            for (int slot = 0; slot < half.getContainerSize(); slot++, i++) {
                half.setItem(slot, i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
            }
            // Menus and hoppers call this after moving items; setItem alone doesn't
            half.setChanged();
        }
    }

    private static ItemStack captured(EntityType<?> type) {
        return MobCapture.toItem(new CapturedMob(type, new CompoundTag(), Optional.empty(), 20.0F, 20.0F, false));
    }

    /** The full recipe for a double chest: spawn eggs, or Captured Mobs (which don't stack). */
    private static List<ItemStack> recipe(boolean capturedMobs) {
        List<ItemStack> list = new ArrayList<>();
        if (capturedMobs) {
            list.add(captured(EntityTypes.ZOMBIE));
            list.add(captured(EntityTypes.HUSK));
            list.add(captured(EntityTypes.DROWNED));
            list.add(captured(EntityTypes.ZOMBIE_VILLAGER));
            for (int i = 0; i < 12; i++) list.add(captured(EntityTypes.VILLAGER));
        } else {
            list.add(new ItemStack(Items.ZOMBIE_SPAWN_EGG, 3));
            list.add(new ItemStack(Items.HUSK_SPAWN_EGG, 1));
            list.add(new ItemStack(Items.VILLAGER_SPAWN_EGG, 12));
        }
        // Materials can be split over slots and mixed between kinds
        list.add(new ItemStack(Items.TORCH, 32));
        list.add(new ItemStack(Items.SOUL_TORCH, 1));
        list.add(new ItemStack(Items.OAK_SLAB, 10));
        list.add(new ItemStack(Items.SMOOTH_STONE_SLAB, 10));
        list.add(new ItemStack(Items.OAK_SIGN, 16));
        list.add(new ItemStack(Items.SPRUCE_TRAPDOOR, 12));
        for (int i = 0; i < 12; i++) list.add(new ItemStack(i % 2 == 0 ? Items.BED.red() : Items.BED.white()));
        while (list.size() < IronFarmRecipe.CHEST_SLOTS) list.add(new ItemStack(Items.SMOOTH_STONE, 64));
        return list;
    }

    private static IronFarmBlockEntity farm(GameTestHelper h) {
        h.setBlock(FARM, ModBlocks.IRON_FARM);
        return h.getBlockEntity(FARM, IronFarmBlockEntity.class);
    }

    private static void tick(GameTestHelper h, IronFarmBlockEntity farm, int ticks) {
        for (int i = 0; i < ticks; i++) IronFarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
    }

    private static int count(IronFarmBlockEntity farm, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) if (farm.getItem(i).is(item)) n += farm.getItem(i).getCount();
        return n;
    }

    private static int perGolem() {
        return NexusConfig.get().ironFarmGolemTicks;
    }

    // ---------------------------------------------------------------- the chest

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void chestWithSpawnEggsBecomesIronFarm(GameTestHelper h) {
        fill(doubleChest(h, "Nexus Iron Farm"), recipe(false));
        h.runAfterDelay(2, () -> {
            h.assertBlockNotPresent(Blocks.CHEST, LEFT);
            h.assertBlockNotPresent(Blocks.CHEST, RIGHT);
            h.assertItemEntityPresent(ModItems.IRON_FARM, LEFT, 2.0);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(h.absolutePos(LEFT)).inflate(3)).size() == 1, "only the Iron Farm dropped");
            h.succeed();
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void chestWithCapturedMobsBecomesIronFarm(GameTestHelper h) {
        // The name is matched without caring about upper and lower case or spaces at the ends
        fill(doubleChest(h, "  nexus IRON farm "), recipe(true));
        h.runAfterDelay(2, () -> {
            h.assertBlockNotPresent(Blocks.CHEST, LEFT);
            h.assertItemEntityPresent(ModItems.IRON_FARM, LEFT, 2.0);
            h.succeed();
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void incompleteChestStays(GameTestHelper h) {
        List<ItemStack> stacks = recipe(false);
        // Swap one bed for another stack of smooth stone: every slot is still filled, but a bed is missing
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).is(Items.BED.red())) {
                stacks.set(i, new ItemStack(Items.SMOOTH_STONE, 64));
                break;
            }
        }
        List<ChestBlockEntity> chest = doubleChest(h, "Nexus Iron Farm");
        fill(chest, stacks);
        IronFarmRecipe.Check check = IronFarmRecipe.check(stacks);
        h.assertValueEqual(check.count(IronFarmRecipe.Part.BEDS), 11, "beds counted");
        h.assertTrue(!check.complete(), "one bed short is not complete");
        h.assertTrue(!check.problems().isEmpty(), "the player is told what's missing");
        // Closing it with a player around reports and keeps the chest
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        IronFarmChest.process(h.getLevel(), h.absolutePos(LEFT), player);
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.CHEST, LEFT);
            h.assertBlockPresent(Blocks.CHEST, RIGHT);
            h.assertValueEqual(h.getBlockEntity(LEFT, ChestBlockEntity.class).getItem(0).getCount(), 3, "contents untouched");
            h.succeed();
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void unnamedChestStays(GameTestHelper h) {
        fill(doubleChest(h, null), recipe(false));
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.CHEST, LEFT);
            h.succeed();
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void otherNameStays(GameTestHelper h) {
        fill(doubleChest(h, "Iron Farm"), recipe(false));
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.CHEST, LEFT);
            h.succeed();
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void recipeChecksReportEveryProblem(GameTestHelper h) {
        // A single chest can't hold the recipe
        IronFarmRecipe.Check single = IronFarmRecipe.check(recipe(false).subList(0, 27));
        h.assertTrue(!single.complete(), "a single chest is never complete");
        h.assertValueEqual(single.problems().size(), 2, "double-chest message and the recipe");

        List<ItemStack> stacks = recipe(false);
        stacks.set(stacks.size() - 1, new ItemStack(Items.DIRT, 5));           // doesn't belong
        stacks.set(stacks.size() - 2, new ItemStack(Items.SMOOTH_STONE, 32)); // not a full stack
        stacks.set(stacks.size() - 3, ItemStack.EMPTY);                       // empty slot
        stacks.set(stacks.size() - 4, new ItemStack(Items.TORCH, 2));         // two torches too many
        IronFarmRecipe.Check check = IronFarmRecipe.check(stacks);
        h.assertTrue(!check.complete(), "not complete");
        h.assertValueEqual(check.strays().size(), 1, "dirt listed");
        h.assertValueEqual(check.partialFiller(), 1, "half stack of smooth stone");
        h.assertValueEqual(check.emptySlots(), 1, "empty slots");
        h.assertValueEqual(check.count(IronFarmRecipe.Part.TORCHES), 35, "torches counted");
        h.assertValueEqual(check.problems().size(), 4, "too many, take out, fill and partial lines");

        // A cow is not a zombie, and a zombie horse egg isn't either
        h.assertTrue(IronFarmRecipe.partOf(captured(EntityTypes.COW)) == null, "captured cow isn't a material");
        h.assertTrue(IronFarmRecipe.partOf(new ItemStack(Items.ZOMBIE_HORSE_SPAWN_EGG)) == null, "zombie horse isn't a zombie");
        h.assertTrue(IronFarmRecipe.partOf(new ItemStack(Items.DROWNED_SPAWN_EGG)) == IronFarmRecipe.Part.ZOMBIES, "drowned egg is a zombie");
        h.assertTrue(IronFarmRecipe.partOf(new ItemStack(Items.CRIMSON_HANGING_SIGN)) == IronFarmRecipe.Part.SIGNS, "hanging signs count");
        h.succeed();
    }

    // ---------------------------------------------------------------- the farm

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void farmMakesIronAtTheSetRate(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        tick(h, farm, perGolem() - 1);
        h.assertValueEqual(count(farm, Items.IRON_INGOT), 0, "no golem yet");
        tick(h, farm, 1);
        int iron = count(farm, Items.IRON_INGOT);
        h.assertTrue(iron >= 3 && iron <= 5, "one golem drops 3-5 iron, got " + iron);
        h.assertTrue(count(farm, Items.POPPY) <= 2, "and 0-2 poppies");
        tick(h, farm, perGolem() * 9);
        int tenGolems = count(farm, Items.IRON_INGOT);
        h.assertTrue(tenGolems >= 30 && tenGolems <= 50, "ten golems drop 30-50 iron, got " + tenGolems);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void outputOnlyLeavesFromBelow(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        h.assertValueEqual(farm.getSlotsForFace(Direction.DOWN).length, IronFarmBlockEntity.OUTPUT_SLOTS, "all output slots below");
        for (Direction side : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            h.assertValueEqual(farm.getSlotsForFace(side).length, 0, "no slots on " + side);
        }
        h.assertTrue(farm.canTakeItemThroughFace(0, iron, Direction.DOWN), "takes from below");
        h.assertTrue(!farm.canTakeItemThroughFace(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD), Direction.DOWN), "bread stays in");
        h.assertTrue(!farm.canPlaceItemThroughFace(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD), Direction.UP), "nothing goes in by hopper");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 400)
    public void hopperBelowPullsIron(GameTestHelper h) {
        h.setBlock(FARM.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        // A hopper above pointing down can't push anything in
        h.setBlock(FARM.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        farm(h);
        HopperBlockEntity above = h.getBlockEntity(FARM.above(), HopperBlockEntity.class);
        above.setItem(0, new ItemStack(Items.BREAD, 4));
        h.succeedWhen(() -> {
            HopperBlockEntity below = h.getBlockEntity(FARM.below(), HopperBlockEntity.class);
            boolean gotIron = false;
            for (int i = 0; i < below.getContainerSize(); i++) gotIron |= below.getItem(i).is(Items.IRON_INGOT);
            h.assertTrue(gotIron, "the hopper below has iron");
            h.assertValueEqual(above.getItem(0).getCount(), 4, "bread stayed in the hopper above");
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void breadBoostsThenCoolsDown(GameTestHelper h) {
        NexusConfig config = NexusConfig.get();
        IronFarmBlockEntity farm = farm(h);
        int loaf = config.ironFarmBreadSeconds * 20;
        farm.setItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD, 2));
        tick(h, farm, 1);
        h.assertValueEqual(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).getCount(), 1, "one loaf eaten at a time");
        h.assertValueEqual(farm.boostLeft(), loaf - 1, "boost started");
        // Two loaves: the second starts as soon as the first is gone
        tick(h, farm, 2 * loaf - 1);
        long golems = 2L * loaf * config.ironFarmBreadSpeedUp / perGolem();
        int iron = count(farm, Items.IRON_INGOT);
        h.assertTrue(iron >= golems * 3 && iron <= golems * 5, golems + " boosted golems should drop " + golems * 3 + "-" + golems * 5 + " iron, got " + iron);
        h.assertTrue(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).isEmpty(), "bread used up");
        h.assertValueEqual(farm.boostLeft(), 0, "boost over");
        h.assertValueEqual(farm.cooldown(), 2L * loaf * config.ironFarmCooldownMultiplier, "cooldown is 10x the boost");

        // During the cooldown bread can't go in, by hand or shift-click, and the farm works at normal speed
        h.assertTrue(!farm.canPlaceItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD)), "no bread during cooldown");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        IronFarmMenu menu = new IronFarmMenu(1, player.getInventory(), farm);
        h.assertTrue(!menu.getSlot(IronFarmBlockEntity.BREAD_SLOT).mayPlace(new ItemStack(Items.BREAD)), "bread slot locked");
        farm.clearContent();
        tick(h, farm, perGolem());
        int normal = count(farm, Items.IRON_INGOT);
        h.assertTrue(normal >= 3 && normal <= 5, "still makes one golem per " + perGolem() + " ticks while cooling down, got " + normal);
        h.assertValueEqual(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).getCount(), 0, "nothing eaten");

        tick(h, farm, (int) farm.cooldown());
        h.assertValueEqual(farm.cooldown(), 0L, "cooldown over");
        h.assertTrue(farm.canPlaceItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD)), "bread can go in again");
        h.assertTrue(menu.getSlot(IronFarmBlockEntity.BREAD_SLOT).mayPlace(new ItemStack(Items.BREAD)), "bread slot open again");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void fullFarmWaitsAndSavesItsBread(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) farm.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
        farm.setItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD, 3));
        tick(h, farm, perGolem() * 3);
        h.assertTrue(farm.isFull(), "full");
        h.assertValueEqual(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).getCount(), 3, "no bread eaten while full");
        // Room for a whole golem: one slot for iron and one for poppies
        farm.setItem(0, ItemStack.EMPTY);
        farm.setItem(1, ItemStack.EMPTY);
        tick(h, farm, 1);
        h.assertTrue(!farm.isFull(), "room again");
        h.assertValueEqual(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).getCount(), 2, "bread eaten once there's room");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void redstonePausesTheFarm(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        farm.setItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD, 2));
        farm.setTimers(new IronFarmTimers(0, 0, 0, 50));
        h.setBlock(FARM.north(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(farm.isPaused(), "a redstone signal pauses it");
        tick(h, farm, perGolem() * 2);
        h.assertValueEqual(count(farm, Items.IRON_INGOT), 0, "no golems while powered");
        h.assertValueEqual(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).getCount(), 2, "no bread eaten while powered");
        h.assertValueEqual(farm.cooldown(), 0L, "the cooldown still runs while powered");

        h.setBlock(FARM.north(), Blocks.AIR);
        h.assertTrue(!farm.isPaused(), "working again without the signal");
        tick(h, farm, 1);
        h.assertValueEqual(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).getCount(), 1, "eats bread again");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void timersStayInTheDroppedItem(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        farm.setTimers(new IronFarmTimers(40, 0, 0, 12_345));
        IronFarmTimers kept = farm.collectComponents().get(ModComponents.IRON_FARM_TIMERS);
        h.assertTrue(kept != null && kept.cooldown() == 12_345, "the cooldown goes into the item");

        ItemStack item = new ItemStack(ModItems.IRON_FARM);
        item.set(ModComponents.IRON_FARM_TIMERS, kept);
        h.setBlock(FARM.east(), ModBlocks.IRON_FARM);
        IronFarmBlockEntity placed = h.getBlockEntity(FARM.east(), IronFarmBlockEntity.class);
        placed.applyComponentsFromItemStack(item);
        h.assertValueEqual(placed.cooldown(), 12_345L, "and comes back when placed");
        h.assertTrue(!placed.acceptsBread(), "so breaking it doesn't skip the cooldown");

        h.setBlock(FARM.west(), ModBlocks.IRON_FARM);
        h.assertTrue(h.getBlockEntity(FARM.west(), IronFarmBlockEntity.class).collectComponents().get(ModComponents.IRON_FARM_TIMERS) == null,
                "a fresh farm's item stays plain, so it stacks");
        h.succeed();
    }
}
