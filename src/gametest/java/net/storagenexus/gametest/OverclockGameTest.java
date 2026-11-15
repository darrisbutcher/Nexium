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

    // ---------------------------------------------------------------- the Freezer

    private static net.storagenexus.freezer.FreezerBlockEntity freezer(GameTestHelper h) {
        return freezer(h, POS);
    }

    /** A Freezer with a full tank at this spot (a block set again over the same block keeps its block entity, so each one needs a spot of its own). */
    private static net.storagenexus.freezer.FreezerBlockEntity freezer(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, net.storagenexus.registry.ModBlocks.FREEZER);
        var f = h.getBlockEntity(pos, net.storagenexus.freezer.FreezerBlockEntity.class);
        f.setWater(net.storagenexus.freezer.FreezerBlockEntity.CAPACITY);
        return f;
    }

    private static void tick(GameTestHelper h, net.storagenexus.freezer.FreezerBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) net.storagenexus.freezer.FreezerBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
    }

    private static int ice(net.storagenexus.freezer.FreezerBlockEntity f) {
        int n = 0;
        for (int i = 0; i < f.getContainerSize(); i++) if (f.getItem(i).is(Items.ICE)) n += f.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void packedAndBlueIceTakeHalfAsLongAsTheyDid(GameTestHelper h) {
        h.assertValueEqual(new net.storagenexus.essentials.config.NexusConfig().freezerPackedIceTicks, 450, "packed ice: 22.5 seconds");
        h.assertValueEqual(new net.storagenexus.essentials.config.NexusConfig().freezerBlueIceTicks, 900, "blue ice: 45 seconds");
        h.assertValueEqual(new net.storagenexus.essentials.config.NexusConfig().freezerIceTicks, 100, "ice is as it was");
        // A file written by an older version moves down once, only if it is still on the old numbers
        var old = new net.storagenexus.essentials.config.NexusConfig();
        old.configVersion = 3;
        old.freezerPackedIceTicks = 900;
        old.freezerBlueIceTicks = 1800;
        net.storagenexus.essentials.config.NexusConfig.migrate(old, true);
        h.assertValueEqual(old.freezerPackedIceTicks, 450, "an old packed ice time is halved");
        h.assertValueEqual(old.freezerBlueIceTicks, 900, "and an old blue ice time");
        var custom = new net.storagenexus.essentials.config.NexusConfig();
        custom.configVersion = 3;
        custom.freezerPackedIceTicks = 1000;
        custom.freezerBlueIceTicks = 2000;
        net.storagenexus.essentials.config.NexusConfig.migrate(custom, true);
        h.assertValueEqual(custom.freezerPackedIceTicks, 1000, "a time the owner chose stays");
        h.assertValueEqual(custom.freezerBlueIceTicks, 2000, "and the other");
        var current = new net.storagenexus.essentials.config.NexusConfig();
        current.configVersion = 4;
        current.freezerPackedIceTicks = 900;
        net.storagenexus.essentials.config.NexusConfig.migrate(current, true);
        h.assertValueEqual(current.freezerPackedIceTicks, 900, "a file already on this version is left alone, so 900 can be chosen again");
        h.succeed();
    }

    @GameTest
    public void anOverclockedFreezerWorksFasterOnMoreEnergyAndWater(GameTestHelper h) {
        var f = freezer(h);
        f.setOverclock(3); // 8x
        h.assertValueEqual(f.energy().capacity(), 20L * 200 * 8, "an 8x buffer");
        f.energy().set(f.energy().capacity());
        long before = f.energy().stored();
        long waterBefore = f.water();
        tick(h, f, 12);
        h.assertValueEqual(f.progress(), 96, "12 ticks at 8x");
        h.assertValueEqual(ice(f), 0, "not an ice yet");
        h.assertValueEqual(before - f.energy().stored(), 12L * 8 * 20, "8x the energy a tick");
        tick(h, f, 1);
        h.assertValueEqual(ice(f), 1, "the thirteenth tick makes it: 100 ticks of work in 13");
        h.assertValueEqual(f.progress(), 4, "and the 4 ticks past the end count towards the next");
        h.assertValueEqual(waterBefore - f.water(), net.storagenexus.freezer.FreezerMode.ICE.cost() * 104 / 100, "water for 104 ticks of work, paid up to the next ice");
        h.succeed();
    }

    @GameTest
    public void anOverclockedFreezerRunsAsFastAsTheWaterAndEnergyAllow(GameTestHelper h) {
        var f = freezer(h);
        f.setOverclock(3);
        f.energy().set(50);
        tick(h, f, 1);
        h.assertValueEqual(f.progress(), 2, "50 NE pays for 2 ticks of work");

        var g = freezer(h, POS.east());
        g.setOverclock(3);
        g.energy().set(g.energy().capacity());
        g.setWater(2_500); // 810 droplets a tick of ice
        tick(h, g, 1);
        h.assertValueEqual(g.progress(), 3, "2,500 droplets of water last 3 ticks of work");
        h.assertValueEqual(g.water(), 2_500L - 3 * 810, "and exactly that is used");
        h.succeed();
    }

    @GameTest
    public void theFreezersSettingIsSavedKeptInTheItemAndStepsWithItsButton(GameTestHelper h) {
        var f = freezer(h);
        f.setOverclock(2);
        f.energy().set(5_000);
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        f.saveWithFullMetadata(out);
        var loaded = new net.storagenexus.freezer.FreezerBlockEntity(f.getBlockPos(), f.getBlockState());
        loaded.setLevel(h.getLevel());
        loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertValueEqual(loaded.overclock(), 2, "the level is saved");
        h.assertValueEqual(loaded.energy().stored(), 5_000L, "with the energy it held, more than a normal buffer");

        Integer kept = f.collectComponents().get(ModComponents.OVERCLOCK);
        h.assertTrue(kept != null && kept == 2, "the level goes into the item");
        h.assertTrue(freezer(h, POS.west()).collectComponents().get(ModComponents.OVERCLOCK) == null, "a Freezer that is not overclocked leaves its item plain");

        var g = freezer(h, POS.east());
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        var menu = new net.storagenexus.freezer.FreezerMenu(1, player.getInventory(), g);
        h.assertTrue(menu.clickMenuButton(player, net.storagenexus.freezer.FreezerMenu.BTN_OVERCLOCK), "its button works");
        h.assertValueEqual(g.overclock(), 1, "forward to 2x");
        h.assertTrue(menu.clickMenuButton(player, net.storagenexus.freezer.FreezerMenu.BTN_OVERCLOCK_BACK), "and back");
        h.assertTrue(menu.clickMenuButton(player, net.storagenexus.freezer.FreezerMenu.BTN_OVERCLOCK_BACK), "back past off");
        h.assertValueEqual(menu.overclock(), 3, "to 8x, which the screen reads");
        h.succeed();
    }

    // ---------------------------------------------------------------- conduits reach them from any face

    /** The machines conduits pull the output of, each with an item in its first output slot. */
    private static net.minecraft.world.WorldlyContainer machine(GameTestHelper h, int which, BlockPos pos) {
        switch (which) {
            case 0 -> h.setBlock(pos, ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
            case 1 -> h.setBlock(pos, ModBlocks.IRON_FARM);
            default -> h.setBlock(pos, net.storagenexus.registry.ModBlocks.FREEZER);
        }
        var machine = (net.minecraft.world.WorldlyContainer) h.getBlockEntity(pos, net.minecraft.world.level.block.entity.BlockEntity.class);
        machine.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        return machine;
    }

    @GameTest
    public void everyFaceOfAFarmTheIronFarmAndTheFreezerGivesItsOutputAndTakesNothing(GameTestHelper h) {
        for (int which = 0; which < 3; which++) {
            BlockPos pos = new BlockPos(1 + which * 2, 1, 2);
            var machine = machine(h, which, pos);
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                var storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(pos), side);
                h.assertTrue(storage != null, "machine " + which + " has item storage on " + side);
                try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    long taken = storage.extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.COBBLESTONE), 64, t);
                    h.assertValueEqual(taken, 8L, "machine " + which + " gives its output on " + side);
                }
                try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    long put = storage.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.COBBLESTONE), 64, t);
                    h.assertValueEqual(put, 0L, "machine " + which + " takes nothing on " + side);
                }
            }
            // Hoppers use the container itself, and are as before: the bottom only
            h.assertValueEqual(machine.getSlotsForFace(net.minecraft.core.Direction.UP).length, 0, "a hopper above gets nothing from machine " + which);
            h.assertValueEqual(machine.getSlotsForFace(net.minecraft.core.Direction.EAST).length, 0, "nor one at the side");
            h.assertTrue(machine.getSlotsForFace(net.minecraft.core.Direction.DOWN).length > 0, "a hopper below still takes the output of machine " + which);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 200)
    public void aConduitOnTheSideOfEachMachinePullsItsOutput(GameTestHelper h) {
        BlockPos[] chests = new BlockPos[3];
        for (int which = 0; which < 3; which++) {
            BlockPos pos = new BlockPos(1, 1, 1 + which * 2), conduit = pos.east(), chest = conduit.east();
            machine(h, which, pos);
            h.setBlock(chest, net.minecraft.world.level.block.Blocks.CHEST);
            h.setBlock(conduit, net.storagenexus.registry.ModBlocks.CONDUIT_BUNDLE);
            var bundle = h.getBlockEntity(conduit, net.storagenexus.conduit.ConduitBundleBlockEntity.class);
            bundle.addConduit(net.storagenexus.conduit.ConduitType.ITEM, 1);
            // The side facing the machine takes from it; the side facing the chest (insert, as it starts) fills it
            bundle.config(net.storagenexus.conduit.ConduitType.ITEM, net.minecraft.core.Direction.WEST).mode = net.storagenexus.conduit.SideConfig.Mode.EXTRACT;
            bundle.changed();
            chests[which] = chest;
        }
        h.succeedWhen(() -> {
            for (int which = 0; which < 3; which++) {
                h.assertValueEqual(h.getBlockEntity(chests[which], net.minecraft.world.level.block.entity.ChestBlockEntity.class).countItem(Items.COBBLESTONE), 8,
                        "machine " + which + "'s output reached the chest through a side conduit");
            }
        });
    }
}
