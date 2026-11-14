package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.conduit.SideConfig;
import net.storagenexus.energy.CombustionGeneratorBlock;
import net.storagenexus.energy.CombustionGeneratorBlockEntity;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.registry.ModBlocks;

/**
 * Nexus Energy: the Combustion Generator burns fuel into it, gives it to the machines touching it, and Energy Conduits
 * carry it to machines further away at their tier's rate.
 */
public class EnergyGameTest {
    private static final BlockPos GENERATOR = new BlockPos(1, 2, 2);

    private static CombustionGeneratorBlockEntity generator(GameTestHelper h) {
        h.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR);
        return h.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class);
    }

    private static FarmBlockEntity farm(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        return h.getBlockEntity(pos, FarmBlockEntity.class);
    }

    private static void tick(GameTestHelper h, CombustionGeneratorBlockEntity g, int ticks) {
        for (int i = 0; i < ticks; i++) CombustionGeneratorBlockEntity.serverTick(h.getLevel(), g.getBlockPos(), g.getBlockState(), g);
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theGeneratorBurnsFuelIntoEnergy(GameTestHelper h) {
        CombustionGeneratorBlockEntity g = generator(h);
        h.assertValueEqual(CombustionGeneratorBlockEntity.burnTicks(h.getLevel(), new ItemStack(Items.COAL)), 1600, "coal burns for 80 seconds");
        h.assertValueEqual(CombustionGeneratorBlockEntity.burnTicks(h.getLevel(), new ItemStack(Items.OAK_PLANKS)), 300, "planks for 15");
        h.assertTrue(g.canPlaceItem(0, new ItemStack(Items.CHARCOAL)) && !g.canPlaceItem(0, new ItemStack(Items.STONE)), "only fuel goes in");
        tick(h, g, 10);
        h.assertValueEqual(g.energy().stored(), 0L, "nothing without fuel");
        g.setItem(0, new ItemStack(Items.COAL, 2));
        tick(h, g, 10);
        h.assertValueEqual(g.energy().stored(), 200L, "20 NE a tick");
        h.assertValueEqual(g.getItem(0).getCount(), 1, "one piece burning");
        h.assertTrue(h.getBlockState(GENERATOR).getValue(CombustionGeneratorBlock.LIT), "lit while it burns");
        // Full: it finishes the piece that is burning, and never lights the next
        g.energy().set(CombustionGeneratorBlockEntity.CAPACITY);
        tick(h, g, 1600);
        h.assertValueEqual(g.getItem(0).getCount(), 1, "the next piece is kept while it is full");
        h.assertTrue(!h.getBlockState(GENERATOR).getValue(CombustionGeneratorBlock.LIT), "and it goes out");
        // The outside can take its energy but not put any in
        h.assertValueEqual(g.energy().insert(100, false), 0L, "nothing goes in");
        h.assertValueEqual(g.energy().extract(100, false), 100L, "energy comes out");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void aFarmBesideTheGeneratorRuns(GameTestHelper h) {
        CombustionGeneratorBlockEntity g = generator(h);
        FarmBlockEntity farm = farm(h, GENERATOR.east());
        g.setItem(0, new ItemStack(Items.COAL, 1));
        h.succeedWhen(() -> {
            h.assertTrue(farm.progress() >= 20, "the farm works on the generator's energy: progress " + farm.progress());
            h.assertTrue(!farm.isUnpowered(), "and has power");
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void conduitsCarryEnergyToAFarmFurtherAway(GameTestHelper h) {
        CombustionGeneratorBlockEntity g = generator(h);
        BlockPos c1 = GENERATOR.east(), c2 = c1.east(), farmPos = c2.east();
        h.setBlock(c1, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(c2, ModBlocks.CONDUIT_BUNDLE);
        FarmBlockEntity farm = farm(h, farmPos);
        ConduitBundleBlockEntity one = h.getBlockEntity(c1, ConduitBundleBlockEntity.class);
        ConduitBundleBlockEntity two = h.getBlockEntity(c2, ConduitBundleBlockEntity.class);
        one.addConduit(ConduitType.ENERGY, 1);
        two.addConduit(ConduitType.ENERGY, 1);
        h.assertValueEqual(one.config(ConduitType.ENERGY, Direction.WEST).mode, SideConfig.Mode.BOTH, "energy sides take and give without a wrench");
        h.assertTrue(one.connectsTo(ConduitType.ENERGY, Direction.WEST), "it joins the generator");
        h.assertTrue(two.connectsTo(ConduitType.ENERGY, Direction.EAST), "and the farm");
        g.energy().set(CombustionGeneratorBlockEntity.CAPACITY);
        // One tick of a Tier 1 conduit moves up to 2,500 NE: more than the farm holds, so it is filled at once
        long room = farm.energy().capacity();
        h.assertTrue(room < ConduitBundleBlockEntity.ENERGY_TIER_1, "the farm holds less than a tick of a Tier 1 conduit: " + room);
        nextTick(h);
        ConduitBundleBlockEntity.serverTick(h.getLevel(), one.getBlockPos(), one.getBlockState(), one);
        h.assertValueEqual(farm.energy().stored(), room, "the farm is filled in a tick on Tier 1");
        // The generator fills the pool first (a full 2,500); what the farm could not hold stays in the cable
        h.assertValueEqual(g.energy().stored(), CombustionGeneratorBlockEntity.CAPACITY - ConduitBundleBlockEntity.ENERGY_TIER_1, "a Tier 1 tick taken from the generator");
        h.assertValueEqual(net.storagenexus.conduit.EnergyNetwork.of(one).stored(), ConduitBundleBlockEntity.ENERGY_TIER_1 - room, "the rest waits in the cable");
        // Tier 2 does the same once the farm is empty again
        farm.energy().set(0);
        one.addConduit(ConduitType.ENERGY, 2);
        two.addConduit(ConduitType.ENERGY, 2);
        nextTick(h);
        ConduitBundleBlockEntity.serverTick(h.getLevel(), one.getBlockPos(), one.getBlockState(), one);
        h.assertValueEqual(farm.energy().stored(), room, "filled in a tick on Tier 2");
        h.succeedWhen(() -> h.assertTrue(farm.progress() >= 20, "the farm runs: progress " + farm.progress()));
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void theSolarPanelMakesEnergyOnlyInTheSun(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.SOLAR_PANEL);
        net.storagenexus.energy.SolarPanelBlockEntity panel = h.getBlockEntity(pos, net.storagenexus.energy.SolarPanelBlockEntity.class);
        h.assertValueEqual(net.storagenexus.energy.SolarPanelBlockEntity.OUTPUT, 30, "30 NE a tick");
        h.assertValueEqual(net.storagenexus.energy.SolarPanelBlockEntity.CAPACITY, 5_000L, "it stores 5,000 NE");
        h.assertValueEqual(panel.energy().capacity(), 5_000L, "in its buffer");
        var level = h.getLevel();
        BlockPos abs = h.absolutePos(pos);
        h.assertValueEqual(net.storagenexus.energy.SolarPanelBlockEntity.RAIN_OUTPUT, 8, "8 NE a tick in rain");
        long expected = net.storagenexus.energy.SolarPanelBlockEntity.output(level, abs);
        net.storagenexus.energy.SolarPanelBlockEntity.serverTick(level, abs, h.getBlockState(pos), panel);
        // Whether it is day depends on when the test world runs, so the output is checked against the same rule
        h.assertValueEqual(panel.energy().stored(), expected, "what the rule says");
        h.assertTrue(expected == 0 || expected == 30 || expected == 8, "0, 8 or 30");
        // Under a roof it makes nothing
        panel.energy().set(0);
        h.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.STONE);
        net.storagenexus.energy.SolarPanelBlockEntity.serverTick(level, abs, h.getBlockState(pos), panel);
        h.assertValueEqual(panel.energy().stored(), 0L, "nothing under a roof");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void theThermalGeneratorBurnsLava(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.THERMAL_GENERATOR);
        net.storagenexus.energy.ThermalGeneratorBlockEntity g = h.getBlockEntity(pos, net.storagenexus.energy.ThermalGeneratorBlockEntity.class);
        for (int i = 0; i < 5; i++) net.storagenexus.energy.ThermalGeneratorBlockEntity.serverTick(h.getLevel(), g.getBlockPos(), g.getBlockState(), g);
        h.assertValueEqual(g.energy().stored(), 0L, "nothing without lava");
        // Lava only goes in
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertValueEqual(g.tank().insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, tx), 0L, "water is refused");
            h.assertValueEqual(g.tank().insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA),
                    5 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, tx), 4 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, "lava fills 4 buckets");
            tx.commit();
        }
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertValueEqual(g.tank().extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA), 1, tx), 0L, "and none comes back out");
        }
        for (int i = 0; i < 10; i++) net.storagenexus.energy.ThermalGeneratorBlockEntity.serverTick(h.getLevel(), g.getBlockPos(), g.getBlockState(), g);
        h.assertValueEqual(g.energy().stored(), 800L, "80 NE a tick");
        h.assertValueEqual(g.lava(), 3 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, "a bucket taken");
        h.assertTrue(h.getBlockState(pos).getValue(net.storagenexus.energy.ThermalGeneratorBlock.LIT), "lit while it burns");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void batteriesStoreAndGiveEnergyAndShowTheirCharge(GameTestHelper h) {
        h.assertValueEqual(ModBlocks.IRON_BATTERY.capacity(), 250_000L, "iron");
        h.assertValueEqual(ModBlocks.GOLD_BATTERY.capacity(), 1_000_000L, "gold");
        h.assertValueEqual(ModBlocks.DIAMOND_BATTERY.capacity(), 10_000_000L, "diamond");
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.IRON_BATTERY);
        net.storagenexus.energy.BatteryBlockEntity b = h.getBlockEntity(pos, net.storagenexus.energy.BatteryBlockEntity.class);
        h.assertValueEqual(b.energy().insert(1_000_000, false), 250_000L, "it takes what fits");
        h.assertValueEqual(b.energy().extract(1000, false), 1000L, "and gives it back");
        h.assertValueEqual(b.charge(), 6, "nearly full shows six cells");
        b.energy().set(1);
        h.assertValueEqual(b.charge(), 1, "any charge shows a cell");
        b.energy().set(0);
        h.assertValueEqual(b.charge(), 0, "empty shows none");
        // A comparator reads it, and the charge shows on the block after a moment
        b.energy().set(125_000);
        net.storagenexus.energy.BatteryBlockEntity.serverTick(h.getLevel(), b.getBlockPos(), b.getBlockState(), b);
        for (int i = 0; i < 6; i++) net.storagenexus.energy.BatteryBlockEntity.serverTick(h.getLevel(), b.getBlockPos(), h.getBlockState(pos), b);
        h.assertTrue(h.getBlockState(pos).getValue(net.storagenexus.energy.BatteryBlock.CHARGE) >= 3, "half full shows its cells");
        BlockPos abs = h.absolutePos(pos);
        int signal = h.getBlockState(pos).getAnalogOutputSignal(h.getLevel(), abs, Direction.NORTH);
        h.assertTrue(signal >= 7 && signal <= 9, "a comparator reads about half: " + signal);
        // Broken, it keeps its charge; placed from that item it has it again
        var back = b.collectComponents();
        h.assertValueEqual(back.get(net.storagenexus.registry.ModComponents.BATTERY_CHARGE), 125_000L, "the dropped item keeps the charge");
        var item = new net.minecraft.world.item.ItemStack(ModBlocks.IRON_BATTERY);
        item.set(net.storagenexus.registry.ModComponents.BATTERY_CHARGE, 40_000L);
        b.applyComponentsFromItemStack(item);
        h.assertValueEqual(b.energy().stored(), 40_000L, "and gets it back when placed");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void aSolarPanelChargesABatteryThatPowersAFarmThroughConduits(GameTestHelper h) {
        BlockPos battery = new BlockPos(1, 2, 2), c1 = battery.east(), farmPos = c1.east();
        h.setBlock(battery, ModBlocks.IRON_BATTERY);
        h.setBlock(c1, ModBlocks.CONDUIT_BUNDLE);
        FarmBlockEntity farm = farm(h, farmPos);
        ConduitBundleBlockEntity conduit = h.getBlockEntity(c1, ConduitBundleBlockEntity.class);
        conduit.addConduit(ConduitType.ENERGY, 1);
        net.storagenexus.energy.BatteryBlockEntity b = h.getBlockEntity(battery, net.storagenexus.energy.BatteryBlockEntity.class);
        b.energy().set(100_000);
        nextTick(h);
        ConduitBundleBlockEntity.serverTick(h.getLevel(), conduit.getBlockPos(), conduit.getBlockState(), conduit);
        long room = farm.energy().capacity();
        h.assertValueEqual(farm.energy().stored(), room, "the conduit fills the farm out of the battery in a tick");
        h.assertValueEqual(b.energy().stored(), 100_000L - room, "from the battery");
        h.succeed();
    }

    private static int reactorSlot;

    /** A fresh reactor with this many rods, each in a spot of its own. */
    private static net.storagenexus.energy.RedstoneReactorBlockEntity reactor(GameTestHelper h, int rods) {
        BlockPos pos = new BlockPos(1 + reactorSlot++ % 5, 2, 1 + reactorSlot / 5 % 3);
        h.setBlock(pos, ModBlocks.REDSTONE_REACTOR);
        var r = h.getBlockEntity(pos, net.storagenexus.energy.RedstoneReactorBlockEntity.class);
        for (int i = 0; i < rods; i++) r.setItem(i, new net.minecraft.world.item.ItemStack(net.storagenexus.registry.ModItems.REDSTONE_FUEL_ROD));
        return r;
    }

    private static void run(GameTestHelper h, net.storagenexus.energy.RedstoneReactorBlockEntity r, int ticks) {
        // Something is always drawing the power, so the buffer never fills and idles the rods
        for (int i = 0; i < ticks; i++) {
            if (r.output() > 0 || i > 0) r.energy().set(0);
            net.storagenexus.energy.RedstoneReactorBlockEntity.serverTick(h.getLevel(), r.getBlockPos(), r.getBlockState(), r);
        }
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theReactorMakes200PerRodAtTheDefaultDepthOnAnyNumberOfRods(GameTestHelper h) {
        reactorSlot = 0;
        for (int rods : new int[] {1, 4, 9}) {
            var r = reactor(h, rods);
            run(h, r, 1);
            h.assertValueEqual(r.output(), 200 * rods, rods + " rods at the default depth");
        }
        var none = reactor(h, 0);
        run(h, none, 3);
        h.assertValueEqual(none.energy().stored(), 0L, "no rods, no power");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theReactorDepthTradesPowerForHeatAndWear(GameTestHelper h) {
        reactorSlot = 0;
        int[] power = {900, 1350, 1800, 2250, 2700};
        double[] heat = new double[5];
        for (int d = 0; d < 5; d++) {
            var r = reactor(h, 9);
            r.setDepth(d);
            run(h, r, 1);
            h.assertValueEqual(r.output(), power[d], "depth " + d);
            heat[d] = r.heat() + net.storagenexus.energy.RedstoneReactorBlockEntity.PASSIVE_COOLING;
        }
        h.assertTrue(heat[0] < heat[1] && heat[1] < heat[2] && heat[2] < heat[3] && heat[3] < heat[4], "deeper rods run hotter");
        h.assertTrue(Math.abs(heat[3] / heat[2] - 1.6) < 0.001 && Math.abs(heat[4] / heat[2] - 2.25) < 0.001, "1.6x and 2.25x the default heat");
        // The menu buttons move the depth, stopping at both ends
        var r = reactor(h, 1);
        var menu = new net.storagenexus.energy.RedstoneReactorMenu(1, h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory(), r);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (int i = 0; i < 5; i++) menu.clickMenuButton(player, net.storagenexus.energy.RedstoneReactorMenu.BTN_INSERT);
        h.assertValueEqual(r.depth(), 4, "Inserted 2 is the end");
        for (int i = 0; i < 9; i++) menu.clickMenuButton(player, net.storagenexus.energy.RedstoneReactorMenu.BTN_RETRACT);
        h.assertValueEqual(r.depth(), 0, "Retracted 2 is the other");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theReactorFallsOffShutsDownAndRestarts(GameTestHelper h) {
        reactorSlot = 0;
        var f = net.storagenexus.energy.RedstoneReactorBlockEntity.class;
        h.assertValueEqual(net.storagenexus.energy.RedstoneReactorBlockEntity.heatFactor(0.59), 1.0, "full output to 60%");
        h.assertTrue(Math.abs(net.storagenexus.energy.RedstoneReactorBlockEntity.heatFactor(0.75) - 0.55) < 1e-9, "falling steadily in between");
        var r = reactor(h, 9);
        r.setHeat(0.75);
        r.energy().set(0);
        run(h, r, 1);
        h.assertTrue(r.output() > 900 && r.output() < 1200, "about 55% of 1,800 at 75% heat: " + r.output());
        r.setHeat(0.9005);
        run(h, r, 1);
        h.assertTrue(r.isShutDown() || r.heat() >= 0.9, "the redline shuts it off");
        run(h, r, 1);
        h.assertValueEqual(r.output(), 0, "and it makes nothing");
        r.setHeat(0.7);
        run(h, r, 1);
        h.assertValueEqual(r.output(), 0, "still off at 70%, it restarts only under 50%");
        r.setHeat(0.45);
        run(h, r, 1);
        h.assertTrue(!r.isShutDown() && r.output() > 0, "restarts under 50%");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void iceCoolsTheReactorAndBlueIceHoldsFullOutput(GameTestHelper h) {
        reactorSlot = 0;
        var r = reactor(h, 9);
        for (int c = 0; c < 3; c++) r.setItem(9 + c, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_ICE, 64));
        r.energy().set(0);
        run(h, r, 3000);
        r.energy().set(0);
        h.assertTrue(r.heat() < net.storagenexus.energy.RedstoneReactorBlockEntity.FULL_OUTPUT_BELOW, "blue ice keeps nine default rods in the full band: " + r.heat());
        h.assertTrue(r.getItem(9).getCount() < 64, "and is used up");
        var hot = reactor(h, 9);
        run(h, hot, 4500);
        h.assertTrue(hot.heat() > 0.6 || hot.isShutDown(), "without ice the same reactor overheats: " + hot.heat());
        var cool = reactor(h, 1);
        cool.setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_ICE, 4));
        run(h, cool, 100);
        h.assertValueEqual(cool.getItem(9).getCount(), 4, "ice is not used while the core is cool");
        h.assertTrue(!r.canPlaceItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)), "only ice goes in");
        h.assertTrue(!r.canPlaceItem(0, new net.minecraft.world.item.ItemStack(net.storagenexus.registry.ModItems.SPENT_FUEL_ROD)), "only live rods go in");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aRodWearsOutIntoASpentRod(GameTestHelper h) {
        reactorSlot = 0;
        var r = reactor(h, 1);
        var rod = r.getItem(0);
        rod.setDamageValue(rod.getMaxDamage() - 2);
        r.energy().set(0);
        run(h, r, 1);
        h.assertTrue(r.getItem(0).is(net.storagenexus.registry.ModItems.REDSTONE_FUEL_ROD), "one tick of wear at the default depth");
        run(h, r, 1);
        h.assertTrue(r.getItem(0).is(net.storagenexus.registry.ModItems.SPENT_FUEL_ROD), "worn out, it leaves a Spent Fuel Rod");
        run(h, r, 1);
        h.assertValueEqual(r.output(), 0, "and makes nothing");
        // A deeper rod wears 1.5x as fast
        var deep = reactor(h, 1);
        deep.setDepth(4);
        int start = deep.getItem(0).getDamageValue();
        run(h, deep, 100);
        h.assertValueEqual(deep.getItem(0).getDamageValue() - start, 150, "Inserted 2 wears 1.5x");
        h.succeed();
    }

    /** One tick of the network the conduit at this spot belongs to, advanced a tick so it runs again. */
    private static void netTick(GameTestHelper h, ConduitBundleBlockEntity c) {
        ConduitBundleBlockEntity.serverTick(h.getLevel(), c.getBlockPos(), c.getBlockState(), c);
    }

    private static void nextTick(GameTestHelper h) {
        // The network runs once per game tick: the helper's level time doesn't move in a test, so ask it for a fresh network
        net.storagenexus.util.StorageEvents.conduitsChanged();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void conduitsShareOnePoolThatHoldsEnergy(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 2, 1);
        for (int i = 0; i < 3; i++) h.setBlock(a.east(i), ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity[] c = new ConduitBundleBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            c[i] = h.getBlockEntity(a.east(i), ConduitBundleBlockEntity.class);
            c[i].addConduit(ConduitType.ENERGY, i == 2 ? 2 : 1);
        }
        var network = net.storagenexus.conduit.EnergyNetwork.of(c[0]);
        h.assertValueEqual(network.size(), 3, "three conduits in one network");
        h.assertValueEqual(network.capacity(), 10_000L + 10_000L + 200_000L, "their capacities add up");
        h.assertValueEqual(network.insert(5_000), 5_000L, "the pool takes energy");
        h.assertValueEqual(network.stored(), 5_000L, "and holds it");
        h.assertValueEqual(c[0].energyStored() + c[1].energyStored() + c[2].energyStored(), 5_000L, "spread over the conduits");
        h.assertValueEqual(network.extract(1_500), 1_500L, "and gives it out again");
        h.assertValueEqual(network.stored(), 3_500L, "what is left");
        // Cutting the run keeps each part's energy
        h.setBlock(a.east(1), net.minecraft.world.level.block.Blocks.AIR);
        long left = c[0].energyStored() + c[2].energyStored();
        h.assertTrue(left > 0 && left <= 3_500L, "the pieces keep their share: " + left);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void machinesComeBeforeBatteriesAndShareEvenly(GameTestHelper h) {
        // generator - conduit - farm, and a battery and a second farm on the same conduit
        BlockPos conduit = new BlockPos(3, 2, 3);
        h.setBlock(conduit, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity c = h.getBlockEntity(conduit, ConduitBundleBlockEntity.class);
        c.addConduit(ConduitType.ENERGY, 2);
        // The Battery is nearest, a farm each side of it further round
        h.setBlock(conduit.west(), ModBlocks.IRON_BATTERY);
        var battery = h.getBlockEntity(conduit.west(), net.storagenexus.energy.BatteryBlockEntity.class);
        FarmBlockEntity farmA = farm(h, conduit.east());
        FarmBlockEntity farmB = farm(h, conduit.north());
        // The battery is the only source, with plenty
        battery.energy().set(200_000);
        // Make the battery a source only, and the farms sinks only
        c.config(ConduitType.ENERGY, Direction.WEST).mode = SideConfig.Mode.EXTRACT;
        c.config(ConduitType.ENERGY, Direction.EAST).mode = SideConfig.Mode.INSERT;
        c.config(ConduitType.ENERGY, Direction.NORTH).mode = SideConfig.Mode.INSERT;
        c.changed();
        farmA.energy().set(0);
        farmB.energy().set(0);
        // Run until the farms have what they can hold
        for (int i = 0; i < 6; i++) {
            net.storagenexus.util.StorageEvents.conduitsChanged();
            netTick(h, c);
        }
        h.assertTrue(farmA.energy().stored() > 0 && farmB.energy().stored() > 0, "both farms are filled");
        long diff = Math.abs(farmA.energy().stored() - farmB.energy().stored());
        h.assertTrue(diff <= 1, "evenly: " + farmA.energy().stored() + " and " + farmB.energy().stored());
        h.succeed();
    }

    /** A Reactor with plenty stored on one side of a conduit and a Battery on the other: one tick moves the conduit's rate. */
    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void conduitsMove2500AndFiftyThousandEnergyATick(GameTestHelper h) {
        h.assertValueEqual(ConduitBundleBlockEntity.ENERGY_TIER_1, 2_500L, "Tier 1 moves 2,500 NE a tick");
        h.assertValueEqual(ConduitBundleBlockEntity.ENERGY_TIER_2, 50_000L, "Tier 2 moves 50,000 NE a tick");
        h.assertValueEqual(net.storagenexus.conduit.EnergyNetwork.CAPACITY_TIER_1, 10_000L, "a Tier 1 segment holds 10,000");
        h.assertValueEqual(net.storagenexus.conduit.EnergyNetwork.CAPACITY_TIER_2, 200_000L, "a Tier 2 segment holds 200,000");
        BlockPos conduit = new BlockPos(3, 2, 3);
        h.setBlock(conduit, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity c = h.getBlockEntity(conduit, ConduitBundleBlockEntity.class);
        c.addConduit(ConduitType.ENERGY, 1);
        h.setBlock(conduit.west(), ModBlocks.REDSTONE_REACTOR);
        var reactor = h.getBlockEntity(conduit.west(), net.storagenexus.energy.RedstoneReactorBlockEntity.class);
        h.setBlock(conduit.east(), ModBlocks.IRON_BATTERY);
        var battery = h.getBlockEntity(conduit.east(), net.storagenexus.energy.BatteryBlockEntity.class);
        reactor.energy().set(900_000);
        battery.energy().set(0);
        nextTick(h);
        netTick(h, c);
        h.assertValueEqual(battery.energy().stored(), 2_500L, "2,500 NE in one tick on Tier 1");
        h.assertValueEqual(reactor.energy().stored(), 900_000L - 2_500L, "taken from the reactor");
        c.addConduit(ConduitType.ENERGY, 2);
        battery.energy().set(0);
        reactor.energy().set(900_000);
        nextTick(h);
        netTick(h, c);
        h.assertValueEqual(battery.energy().stored(), 50_000L, "50,000 NE in one tick on Tier 2");
        h.assertValueEqual(reactor.energy().stored(), 900_000L - 50_000L, "taken from the reactor");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aBatteryGivesOnlyWhatTheGeneratorsLeaveShortAndTakesTheRest(GameTestHelper h) {
        BlockPos conduit = new BlockPos(3, 2, 3);
        h.setBlock(conduit, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity c = h.getBlockEntity(conduit, ConduitBundleBlockEntity.class);
        c.addConduit(ConduitType.ENERGY, 1);
        // A battery (the backup), a farm (the machine) and a generator with a little energy on one conduit
        h.setBlock(conduit.north(), ModBlocks.IRON_BATTERY);
        var battery = h.getBlockEntity(conduit.north(), net.storagenexus.energy.BatteryBlockEntity.class);
        FarmBlockEntity farm = farm(h, conduit.south());
        h.setBlock(conduit.east(), ModBlocks.SOLAR_PANEL);
        var panel = h.getBlockEntity(conduit.east(), net.storagenexus.energy.SolarPanelBlockEntity.class);
        farm.energy().set(0);
        battery.energy().set(100_000);
        panel.energy().set(0);
        c.changed();
        // The panel has nothing: the farm is short, so the battery covers it
        long room = farm.energy().capacity();
        net.storagenexus.util.StorageEvents.conduitsChanged();
        netTick(h, c);
        h.assertValueEqual(farm.energy().stored(), room, "the farm is served, from the battery");
        h.assertValueEqual(battery.energy().stored(), 100_000L - room, "the battery gave exactly what the farm took");
        // The panel has 1,500 and the farm wants its full room: the panel goes first, the battery makes up the rest
        farm.energy().set(0);
        battery.energy().set(100_000);
        panel.energy().set(1_500);
        net.storagenexus.util.StorageEvents.conduitsChanged();
        netTick(h, c);
        h.assertValueEqual(farm.energy().stored(), room, "the farm gets all it wants");
        h.assertValueEqual(panel.energy().stored(), 0L, "all of the panel's energy was used first");
        h.assertValueEqual(battery.energy().stored(), 100_000L - (room - 1_500L), "and the battery gave only what was short");
        // With the farm full, the panel's energy charges the battery instead, and the battery gives nothing
        farm.energy().set(farm.energy().capacity());
        battery.energy().set(1_000);
        panel.energy().set(400);
        net.storagenexus.util.StorageEvents.conduitsChanged();
        netTick(h, c);
        h.assertValueEqual(battery.energy().stored(), 1_400L, "the surplus charges the battery");
        h.assertValueEqual(panel.energy().stored(), 0L, "the panel gave it all");
        h.succeed();
    }

    /** What was reported: a Solar Panel, a run of conduits and a Battery. The Battery has to charge, and a panel never takes energy in. */
    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aSolarPanelChargesABatteryThroughConduitsAndGeneratorsNeverTakeEnergyIn(GameTestHelper h) {
        BlockPos first = new BlockPos(1, 2, 3);
        ConduitBundleBlockEntity[] run = new ConduitBundleBlockEntity[6];
        for (int i = 0; i < run.length; i++) {
            h.setBlock(first.east(i + 1), ModBlocks.CONDUIT_BUNDLE);
            run[i] = h.getBlockEntity(first.east(i + 1), ConduitBundleBlockEntity.class);
            run[i].addConduit(ConduitType.ENERGY, 1);
        }
        h.setBlock(first, ModBlocks.IRON_BATTERY);
        var battery = h.getBlockEntity(first, net.storagenexus.energy.BatteryBlockEntity.class);
        h.setBlock(first.east(run.length + 1), ModBlocks.SOLAR_PANEL);
        var one = h.getBlockEntity(first.east(run.length + 1), net.storagenexus.energy.SolarPanelBlockEntity.class);
        long made = 0;
        for (int tick = 0; tick < 200; tick++) {
            made += one.energy().add(net.storagenexus.energy.SolarPanelBlockEntity.OUTPUT, false);   // what the sun gives each tick
            net.storagenexus.util.StorageEvents.conduitsChanged();
            netTick(h, run[0]);
        }
        h.assertTrue(battery.energy().stored() >= made - 400, "the battery holds what the panel made: " + battery.energy().stored() + " of " + made);
        h.assertTrue(battery.energy().stored() > 500, "well past 500 NE: " + battery.energy().stored());
        // A second panel on the same run: panels never take energy in, so the first one's charge only ever leaves
        h.setBlock(first.east(0).above(), ModBlocks.SOLAR_PANEL);
        h.setBlock(first.east(run.length / 2 + 1).above(), ModBlocks.SOLAR_PANEL);
        var two = h.getBlockEntity(first.east(run.length / 2 + 1).above(), net.storagenexus.energy.SolarPanelBlockEntity.class);
        h.assertValueEqual(two.energy().insert(100, false), 0L, "a Solar Panel takes nothing in from outside");
        one.energy().set(400);
        two.energy().set(0);
        long before = battery.energy().stored();
        net.storagenexus.util.StorageEvents.conduitsChanged();
        netTick(h, run[0]);
        h.assertValueEqual(two.energy().stored(), 0L, "the second panel was not filled from the first");
        h.assertValueEqual(battery.energy().stored(), before + 400, "the first panel's 400 went to the battery");
        h.succeed();
    }
}
