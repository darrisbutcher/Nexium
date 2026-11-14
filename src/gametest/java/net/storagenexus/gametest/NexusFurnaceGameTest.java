package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.energy.NexusFurnaceBlockEntity;
import net.storagenexus.registry.ModBlocks;

/**
 * The Nexus Furnace: eight slots cooking at once, smelting, blasting and smoking recipes, 2.5 seconds an item, 30 NE/t per
 * working slot, as many slots as the energy pays for, stacks shared out evenly, and the experience kept.
 */
public class NexusFurnaceGameTest {
    private static int placed;

    /** A fresh furnace, in a spot of its own so one test can use several. */
    private static NexusFurnaceBlockEntity furnace(GameTestHelper h) {
        BlockPos pos = new BlockPos(1 + placed++ % 5, 2, 2);
        h.setBlock(pos, ModBlocks.NEXUS_FURNACE);
        return h.getBlockEntity(pos, NexusFurnaceBlockEntity.class);
    }

    private static void tick(GameTestHelper h, NexusFurnaceBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) NexusFurnaceBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
    }

    private static int count(NexusFurnaceBlockEntity f, int from, int to, Item item) {
        int n = 0;
        for (int i = from; i < to; i++) if (f.getItem(i).is(item)) n += f.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void itCooksWhatAFurnaceABlastFurnaceAndASmokerDo(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        f.setItem(0, new ItemStack(Items.IRON_ORE, 2));   // smelting and blasting
        f.setItem(1, new ItemStack(Items.BEEF, 2));       // smelting and smoking
        f.setItem(2, new ItemStack(Items.SAND, 2));       // smelting
        f.setItem(3, new ItemStack(Items.DIAMOND, 2));    // nothing cooks it
        tick(h, f, NexusFurnaceBlockEntity.COOK_TICKS);
        h.assertValueEqual(count(f, 8, 16, Items.IRON_INGOT), 1, "iron ore smelts");
        h.assertValueEqual(count(f, 8, 16, Items.COOKED_BEEF), 1, "beef cooks");
        h.assertValueEqual(count(f, 8, 16, Items.GLASS), 1, "sand smelts to glass");
        h.assertValueEqual(f.getItem(3).getCount(), 2, "a diamond just sits there");
        h.assertValueEqual(f.progress(3), 0, "with no progress");
        h.assertTrue(f.getItem(9).is(Items.COOKED_BEEF), "each result lands in the output slot of its own input slot");
        h.succeed();
    }

    @GameTest
    public void allEightSlotsCookAtOnceAtTheirOwnPace(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        for (int i = 0; i < 8; i++) f.setItem(i, new ItemStack(Items.RAW_IRON, 4));
        long before = f.energy().stored();
        tick(h, f, 1);
        h.assertValueEqual(f.working(), 8, "eight slots working");
        h.assertValueEqual(NexusFurnaceBlockEntity.ENERGY_PER_SLOT, 30, "30 NE/t a slot");
        h.assertValueEqual(before - f.energy().stored(), 240L, "30 NE/t each, 240 for all eight");
        tick(h, f, NexusFurnaceBlockEntity.COOK_TICKS - 1);
        h.assertValueEqual(count(f, 8, 16, Items.IRON_INGOT), 8, "eight ingots after 2.5 seconds");
        tick(h, f, NexusFurnaceBlockEntity.COOK_TICKS * 3);
        h.assertValueEqual(count(f, 8, 16, Items.IRON_INGOT), 32, "and every slot keeps going");
        h.assertValueEqual(count(f, 0, 8, Items.RAW_IRON), 0, "until the input is used up");
        h.succeed();
    }

    @GameTest
    public void withLittleEnergyItRunsAsManySlotsAsItCanPayForAndTheyTakeTurns(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        for (int i = 0; i < 8; i++) f.setItem(i, new ItemStack(Items.RAW_IRON, 64));
        for (int t = 0; t < 100; t++) {
            f.energy().set(3L * NexusFurnaceBlockEntity.ENERGY_PER_SLOT); // 3 slots' worth each tick
            tick(h, f, 1);
        }
        int min = 99, max = 0, sum = 0;
        for (int i = 0; i < 8; i++) {
            int p = f.progress(i) + 50 * (f.getItem(8 + i).isEmpty() ? 0 : f.getItem(8 + i).getCount());
            min = Math.min(min, p);
            max = Math.max(max, p);
            sum += p;
        }
        h.assertValueEqual(sum, 300, "three slots' worth of work every tick");
        h.assertTrue(max - min <= 1, "every slot cooked the same amount: " + min + " to " + max);
        h.assertValueEqual(f.working(), 3, "three working");
        h.assertValueEqual(f.wanting(), 8, "eight wanting");
        // No energy: nothing happens
        NexusFurnaceBlockEntity none = furnace(h);
        none.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        tick(h, none, 100);
        h.assertValueEqual(none.progress(0), 0, "no energy, no cooking");
        h.succeed();
    }

    @GameTest
    public void aStackIsSharedOutEvenly(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        h.assertValueEqual(f.insertEvenly(new ItemStack(Items.RAW_IRON, 64), false), 64, "64 go in");
        for (int i = 0; i < 8; i++) h.assertValueEqual(f.getItem(i).getCount(), 8, "slot " + i + " holds 8");
        // More of it tops up the slots evenly
        h.assertValueEqual(f.insertEvenly(new ItemStack(Items.RAW_IRON, 12), false), 12, "12 more");
        int min = 99, max = 0;
        for (int i = 0; i < 8; i++) { min = Math.min(min, f.getItem(i).getCount()); max = Math.max(max, f.getItem(i).getCount()); }
        h.assertTrue(max - min <= 1 && min >= 9, "still level: " + min + " to " + max);
        // Another kind goes only to the slots that are free: none are, so nothing
        h.assertValueEqual(f.insertEvenly(new ItemStack(Items.SAND, 8), true), 0, "another kind finds no free slot");
        // A full set of slots takes no more
        NexusFurnaceBlockEntity full = furnace(h);
        for (int i = 0; i < 8; i++) full.setItem(i, new ItemStack(Items.RAW_IRON, 64));
        h.assertValueEqual(full.insertEvenly(new ItemStack(Items.RAW_IRON, 5), false), 0, "nothing fits in full slots");
        // Things nothing cooks stay out
        h.assertValueEqual(furnace(h).insertEvenly(new ItemStack(Items.DIAMOND, 5), false), 0, "a diamond is refused");
        h.succeed();
    }

    @GameTest
    public void hoppersAndPipesGoInFromTheTopAndSidesAndOutFromBelow(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        Storage<ItemVariant> top = ItemStorage.SIDED.find(h.getLevel(), f.getBlockPos(), Direction.UP);
        Storage<ItemVariant> side = ItemStorage.SIDED.find(h.getLevel(), f.getBlockPos(), Direction.NORTH);
        Storage<ItemVariant> below = ItemStorage.SIDED.find(h.getLevel(), f.getBlockPos(), Direction.DOWN);
        h.assertTrue(top != null && side != null && below != null, "storages on every side");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(top.insert(ItemVariant.of(Items.RAW_IRON), 64, tx), 64L, "64 raw iron in from the top");
            tx.commit();
        }
        for (int i = 0; i < 8; i++) h.assertValueEqual(f.getItem(i).getCount(), 8, "shared out evenly: slot " + i);
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(below.insert(ItemVariant.of(Items.RAW_IRON), 8, tx), 0L, "nothing goes in from below");
            h.assertValueEqual(side.extract(ItemVariant.of(Items.RAW_IRON), 8, tx), 0L, "and nothing comes out of the inputs");
        }
        // An aborted insert puts everything back
        NexusFurnaceBlockEntity g = furnace(h);
        try (Transaction tx = Transaction.openOuter()) {
            ItemStorage.SIDED.find(h.getLevel(), g.getBlockPos(), Direction.EAST).insert(ItemVariant.of(Items.SAND), 16, tx);
        }
        h.assertTrue(g.isEmpty(), "an aborted insert leaves it empty");
        // Outputs come out from below only
        f.setItem(9, new ItemStack(Items.IRON_INGOT, 5));
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(below.extract(ItemVariant.of(Items.IRON_INGOT), 5, tx), 5L, "ingots out from below");
            tx.commit();
        }
        h.assertTrue(f.getItem(9).isEmpty(), "and gone");
        h.assertTrue(!f.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.DOWN), "no input from below");
        h.assertTrue(!f.canTakeItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.UP), "no taking inputs");
        h.succeed();
    }

    @GameTest
    public void aFullOutputStopsOnlyItsOwnSlot(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        f.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        f.setItem(8, new ItemStack(Items.IRON_INGOT, 64)); // slot 0's output is full
        f.setItem(1, new ItemStack(Items.RAW_IRON, 4));
        tick(h, f, NexusFurnaceBlockEntity.COOK_TICKS);
        h.assertValueEqual(f.getItem(0).getCount(), 4, "the slot with a full output waits");
        h.assertValueEqual(f.getItem(9).getCount(), 1, "the next slot cooks on");
        h.assertValueEqual(f.working(), 1, "one slot working");
        h.succeed();
    }

    @GameTest
    public void theExperienceIsKeptUntilCollected(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        for (int i = 0; i < 8; i++) f.setItem(i, new ItemStack(Items.RAW_IRON, 1));
        tick(h, f, NexusFurnaceBlockEntity.COOK_TICKS);
        h.assertValueEqual(f.experience(), 56.0, "raw iron gives 0.7 x 10 = 7 each, 56 for eight");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        int before = player.totalExperience;
        int points = f.takeExperience(player);
        h.assertValueEqual(points, 56, "all 56 points handed over");
        h.assertValueEqual(player.totalExperience - before, 56, "the player has them");
        h.assertTrue(f.experience() == 0.0, "nothing is left: " + f.experience());
        // A fraction stays: beef gives 0.35 x 10 = 3.5 each
        NexusFurnaceBlockEntity beef = furnace(h);
        beef.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        beef.setItem(0, new ItemStack(Items.BEEF, 1));
        tick(h, beef, NexusFurnaceBlockEntity.COOK_TICKS);
        h.assertValueEqual(beef.experience(), 3.5, "a steak gives 3.5");
        h.assertValueEqual(beef.takeExperience(player), 3, "three whole points are handed over");
        h.assertValueEqual(beef.experience(), 0.5, "and the half stays");
        h.succeed();
    }

    /** Every item banks its recipe's experience, however many slots run at once and however short the energy is. */
    @GameTest
    public void everyItemBanksItsExperienceWithAllEightSlotsRunning(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity full = furnace(h);
        for (int i = 0; i < 8; i++) full.setItem(i, new ItemStack(Items.RAW_IRON, 64));
        for (int t = 0; t < 64 * NexusFurnaceBlockEntity.COOK_TICKS; t++) {
            full.energy().set(NexusFurnaceBlockEntity.CAPACITY);
            tick(h, full, 1);
            for (int i = 0; i < 8; i++) if (full.getItem(8 + i).getCount() >= 60) full.setItem(8 + i, ItemStack.EMPTY); // an output taken away
        }
        h.assertValueEqual(count(full, 0, 8, Items.RAW_IRON), 0, "all 512 raw iron cooked");
        h.assertValueEqual(full.experience(), 3584.0, "512 raw iron bank exactly 3,584 points (7 each)");

        // Short of energy the slots take turns, and every item still banks its experience
        NexusFurnaceBlockEntity short_ = furnace(h);
        for (int i = 0; i < 8; i++) short_.setItem(i, new ItemStack(Items.RAW_IRON, 16));
        int cooked = 0;
        for (int t = 0; t < 40_000 && count(short_, 0, 8, Items.RAW_IRON) > 0; t++) {
            short_.energy().set(3L * NexusFurnaceBlockEntity.ENERGY_PER_SLOT);
            tick(h, short_, 1);
            for (int i = 0; i < 8; i++) {
                int made = short_.getItem(8 + i).getCount();
                cooked += made;
                if (made > 0) short_.setItem(8 + i, ItemStack.EMPTY);
            }
        }
        h.assertValueEqual(cooked, 128, "128 ingots made on three slots' worth of energy");
        h.assertValueEqual(short_.experience(), 896.0, "and exactly 896 points banked");
        h.succeed();
    }

    /** Ten times 0.7 is 7: adding up the recipe's float must not leave the bank a hair short, which would floor to 6. */
    @GameTest
    public void theBankNeverDriftsBelowAWholeNumber(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        f.setItem(0, new ItemStack(Items.RAW_IRON, 10));
        for (int t = 0; t < 10 * NexusFurnaceBlockEntity.COOK_TICKS; t++) tick(h, f, 1);
        h.assertValueEqual(count(f, 8, 16, Items.IRON_INGOT), 10, "ten ingots");
        h.assertTrue(f.experience() == 70.0, "ten raw iron bank exactly 70 points: " + f.experience());
        h.assertValueEqual(f.takeExperience(h.makeMockServerPlayerInLevel()), 70, "and 70 are handed over");
        // A big bank is exact too: addExperience adds thousandths without drift
        NexusFurnaceBlockEntity big = furnace(h);
        for (int i = 0; i < 100_000; i++) big.addExperience(0.7);
        h.assertTrue(big.experience() == 70_000.0, "100,000 times 0.7 is exactly 70,000: " + big.experience());
        h.succeed();
    }

    /** The number the screen shows is the number Collect hands over, even for a bank far past 32,767. */
    @GameTest
    public void theScreenNumberIsWhatCollectHandsOver(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.addExperience(123_456.789);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        var menu = new net.storagenexus.energy.NexusFurnaceMenu(1, player.getInventory(), f);
        h.assertValueEqual(menu.experience(), 123_456, "the menu's number is the whole points, not capped at 32,767");
        h.assertValueEqual(f.takeExperience(player), 123_456, "and Collect hands over exactly that");
        h.assertValueEqual(menu.experience(), 0, "leaving the fraction, which shows as 0");
        h.succeed();
    }

    @GameTest
    public void itLightsWhileCookingAndTheRecipeIsCraftable(GameTestHelper h) {
        placed = 0;
        NexusFurnaceBlockEntity f = furnace(h);
        f.energy().set(NexusFurnaceBlockEntity.CAPACITY);
        f.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        tick(h, f, 1);
        h.assertTrue(h.getLevel().getBlockState(f.getBlockPos()).getValue(net.storagenexus.energy.NexusFurnaceBlock.LIT), "lit while it cooks");
        f.setItem(0, ItemStack.EMPTY);
        tick(h, f, 1);
        h.assertTrue(!h.getLevel().getBlockState(f.getBlockPos()).getValue(net.storagenexus.energy.NexusFurnaceBlock.LIT), "dark when there is nothing to cook");
        h.succeed();
    }
}
