package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.farm.FarmMenu;
import net.storagenexus.essentials.farm.Overclock;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmMenu;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.xpfarm.XpFarmBlockEntity;
import net.storagenexus.essentials.xpfarm.XpFarmMenu;

/**
 * Overclock: a farm set to 2x, 4x or 8x does that many ticks of work a tick for that many times the energy, so a
 * cycle costs the same at any speed; short of energy it runs as fast as it can pay for; its buffer grows with the
 * setting; and the setting survives saving and the dropped item.
 */
public class OverclockGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static FarmBlockEntity farm(GameTestHelper h, FarmKind kind) {
        h.setBlock(POS, ModBlocks.FARMS.get(kind));
        return h.getBlockEntity(POS, FarmBlockEntity.class);
    }

    private static void tick(GameTestHelper h, FarmBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) FarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
    }

    @GameTest
    public void theLevelsAreOffThenTwoFourAndEightTimes(GameTestHelper h) {
        h.assertValueEqual(Overclock.multiplier(0), 1, "off is normal speed");
        h.assertValueEqual(Overclock.multiplier(1), 2, "2x");
        h.assertValueEqual(Overclock.multiplier(2), 4, "4x");
        h.assertValueEqual(Overclock.multiplier(3), 8, "8x");
        h.assertValueEqual(Overclock.clamp(9), 3, "out of range is held at 8x");
        h.assertValueEqual(Overclock.clamp(-2), 0, "and at off");
        h.assertValueEqual(Overclock.next(3), 0, "after 8x comes off");
        h.assertValueEqual(Overclock.previous(0), 3, "before off comes 8x");
        h.succeed();
    }

    @GameTest
    public void anOverclockedFarmWorksFasterAndUsesProportionallyMoreEnergy(GameTestHelper h) {
        FarmBlockEntity f = farm(h, FarmKind.SUGAR_CANE);
        int perTick = FarmKind.SUGAR_CANE.energyPerTick();
        for (int level = 0; level <= 3; level++) {
            int mult = Overclock.multiplier(level);
            f.setOverclock(level);
            f.setProgress(0);
            f.energy().set(f.energy().capacity());
            long before = f.energy().stored();
            tick(h, f, 3);
            h.assertValueEqual(f.progress(), 3 * mult, "3 ticks at " + mult + "x");
            h.assertValueEqual(before - f.energy().stored(), 3L * mult * perTick, "and " + mult + "x the energy a tick");
        }
        h.succeed();
    }

    @GameTest
    public void aCycleCostsTheSameEnergyAtAnySpeedAndTakesLessTime(GameTestHelper h) {
        FarmKind kind = FarmKind.ZOMBIFIED_PIGLIN;
        int cycle = kind.cycleTicks();
        long perCycle = (long) cycle * kind.energyPerTick();
        for (int level : new int[] {0, 1, 2, 3}) {
            FarmBlockEntity f = farm(h, kind);
            f.setOverclock(level);
            f.energy().set(f.energy().capacity());
            long before = f.energy().stored();
            int ticks = cycle / Overclock.multiplier(level);
            tick(h, f, ticks - 1);
            h.assertTrue(f.progress() > 0 && f.progress() < cycle, "not finished one tick early at level " + level);
            tick(h, f, 1);
            h.assertValueEqual(f.progress(), 0, "finished after " + ticks + " ticks at level " + level);
            h.assertValueEqual(before - f.energy().stored(), perCycle, "for the same energy at level " + level);
        }
        h.succeed();
    }

    @GameTest
    public void whatGoesPastTheEndOfACycleCountsTowardsTheNext(GameTestHelper h) {
        FarmBlockEntity f = farm(h, FarmKind.SUGAR_CANE);
        f.setOverclock(3);
        f.energy().set(f.energy().capacity());
        f.setProgress(f.cycleTicks() - 3);
        tick(h, f, 1);
        h.assertValueEqual(f.progress(), 5, "8 ticks of work with 3 to go leaves 5 for the next cycle");
        h.succeed();
    }

    @GameTest
    public void shortOfEnergyItRunsAsFastAsItCanPayFor(GameTestHelper h) {
        FarmBlockEntity f = farm(h, FarmKind.SUGAR_CANE); // 10 NE a tick
        f.setOverclock(3);
        f.energy().set(25);
        tick(h, f, 1);
        h.assertValueEqual(f.progress(), 2, "25 NE pays for 2 ticks of work, not 8");
        h.assertValueEqual(f.energy().stored(), 5L, "and 20 NE is used");
        h.assertTrue(!f.isUnpowered(), "it still ran");
        tick(h, f, 1);
        h.assertValueEqual(f.progress(), 2, "5 NE pays for nothing");
        h.assertTrue(f.isUnpowered(), "so it says it has no power");
        h.succeed();
    }

    @GameTest
    public void theBufferGrowsWithTheSettingAndShrinksBack(GameTestHelper h) {
        FarmBlockEntity f = farm(h, FarmKind.SUGAR_CANE);
        h.assertValueEqual(f.energy().capacity(), 2_000L, "ten seconds of work when off");
        f.setOverclock(1);
        h.assertValueEqual(f.energy().capacity(), 4_000L, "twice that at 2x");
        f.setOverclock(3);
        h.assertValueEqual(f.energy().capacity(), 16_000L, "eight times at 8x");
        h.assertValueEqual(f.energy().insert(100_000, false), 16_000L, "and it fills to that");
        f.setOverclock(0);
        h.assertValueEqual(f.energy().capacity(), 2_000L, "back to normal");
        h.assertValueEqual(f.energy().stored(), 2_000L, "what was stored is cut to fit");
        h.succeed();
    }

    @GameTest
    public void theSettingIsSavedAndStaysInTheDroppedItem(GameTestHelper h) {
        FarmBlockEntity f = farm(h, FarmKind.SUGAR_CANE);
        f.setOverclock(2);
        f.energy().set(7_000);
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        f.saveWithFullMetadata(out);
        FarmBlockEntity loaded = new FarmBlockEntity(f.getBlockPos(), f.getBlockState());
        loaded.setLevel(h.getLevel());
        loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertValueEqual(loaded.overclock(), 2, "the level is saved");
        h.assertValueEqual(loaded.energy().capacity(), 8_000L, "with its bigger buffer");
        h.assertValueEqual(loaded.energy().stored(), 7_000L, "and what it held, which is more than a normal buffer");

        Integer kept = f.collectComponents().get(ModComponents.OVERCLOCK);
        h.assertTrue(kept != null && kept == 2, "the level goes into the item");
        ItemStack item = new ItemStack(ModItems.FARMS.get(FarmKind.SUGAR_CANE));
        item.set(ModComponents.OVERCLOCK, kept);
        h.setBlock(POS.east(), ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        FarmBlockEntity placed = h.getBlockEntity(POS.east(), FarmBlockEntity.class);
        placed.applyComponentsFromItemStack(item);
        h.assertValueEqual(placed.overclock(), 2, "and comes back when placed");
        h.assertValueEqual(placed.energy().capacity(), 8_000L, "with the bigger buffer");
        h.setBlock(POS.west(), ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        h.assertTrue(h.getBlockEntity(POS.west(), FarmBlockEntity.class).collectComponents().get(ModComponents.OVERCLOCK) == null,
                "a farm that is not overclocked leaves its item plain, so it stacks");
        h.succeed();
    }

    @GameTest
    public void theButtonStepsTheSettingForwardAndBack(GameTestHelper h) {
        FarmBlockEntity f = farm(h, FarmKind.SUGAR_CANE);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        FarmMenu menu = new FarmMenu(1, player.getInventory(), f);
        for (int expected : new int[] {1, 2, 3, 0}) {
            h.assertTrue(menu.clickMenuButton(player, FarmMenu.BTN_OVERCLOCK), "the button works");
            h.assertValueEqual(f.overclock(), expected, "forward to " + expected);
        }
        h.assertTrue(menu.clickMenuButton(player, FarmMenu.BTN_OVERCLOCK_BACK), "the back button works");
        h.assertValueEqual(f.overclock(), 3, "back from off is 8x");
        h.assertValueEqual(menu.overclock(), 3, "and the menu reads it for the screen");
        h.succeed();
    }

    @GameTest
    public void theIronFarmOverclocks(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.IRON_FARM);
        IronFarmBlockEntity f = h.getBlockEntity(POS, IronFarmBlockEntity.class);
        int golem = net.storagenexus.essentials.config.NexusConfig.get().ironFarmGolemTicks;
        f.setOverclock(2); // 4x
        h.assertValueEqual(f.energy().capacity(), IronFarmBlockEntity.ENERGY_PER_TICK * 200L * 4, "a 4x buffer");
        f.energy().set(f.energy().capacity());
        long before = f.energy().stored();
        int ticks = (golem + 3) / 4; // 4 ticks of work a tick, so a golem takes this many
        for (int i = 0; i < ticks; i++) IronFarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
        int iron = 0;
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) if (f.getItem(i).is(Items.IRON_INGOT)) iron += f.getItem(i).getCount();
        h.assertTrue(iron >= 3 && iron <= 5, "a golem in a quarter of the time, got " + iron);
        h.assertValueEqual(before - f.energy().stored(), ticks * 4L * IronFarmBlockEntity.ENERGY_PER_TICK, "for 4x the energy a tick");

        ServerPlayer player = h.makeMockServerPlayerInLevel();
        IronFarmMenu menu = new IronFarmMenu(1, player.getInventory(), f);
        h.assertTrue(menu.clickMenuButton(player, IronFarmMenu.BTN_OVERCLOCK), "its button works");
        h.assertValueEqual(f.overclock(), 3, "2 steps on to 8x");
        h.assertValueEqual(menu.overclock(), 3, "and the screen reads it");
        h.succeed();
    }

    @GameTest
    public void theExperienceFarmOverclocks(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.XP_FARM);
        XpFarmBlockEntity f = h.getBlockEntity(POS, XpFarmBlockEntity.class);
        f.setOverclock(3);
        h.assertValueEqual(f.energy().capacity(), XpFarmBlockEntity.ENERGY_PER_TICK * 200L * 8, "an 8x buffer");
        f.energy().set(f.energy().capacity());
        XpFarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
        h.assertValueEqual(f.experience(), (long) XpFarmBlockEntity.XP_PER_TICK * 8, "8 ticks of experience in one");
        h.assertValueEqual(f.energy().capacity() - f.energy().stored(), XpFarmBlockEntity.ENERGY_PER_TICK * 8L, "for 8 ticks of energy");

        ServerPlayer player = h.makeMockServerPlayerInLevel();
        XpFarmMenu menu = new XpFarmMenu(1, player.getInventory(), f);
        h.assertTrue(menu.clickMenuButton(player, XpFarmMenu.BTN_OVERCLOCK), "its button works");
        h.assertValueEqual(f.overclock(), 0, "8x steps on to off");
        h.assertTrue(menu.clickMenuButton(player, XpFarmMenu.BTN_OVERCLOCK_BACK), "and back");
        h.assertValueEqual(menu.overclock(), 3, "the screen reads 8x");
        h.succeed();
    }
}
