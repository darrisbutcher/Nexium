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
        // One tick of a Tier 1 conduit moves 500 NE
        ConduitBundleBlockEntity.serverTick(h.getLevel(), one.getBlockPos(), one.getBlockState(), one);
        h.assertValueEqual(farm.energy().stored(), 500L, "500 NE a tick on Tier 1");
        h.assertValueEqual(g.energy().stored(), CombustionGeneratorBlockEntity.CAPACITY - 500, "taken from the generator");
        // Tier 2 moves 4,000 a tick (the farm holds 2,000)
        one.addConduit(ConduitType.ENERGY, 2);
        two.addConduit(ConduitType.ENERGY, 2);
        ConduitBundleBlockEntity.serverTick(h.getLevel(), one.getBlockPos(), one.getBlockState(), one);
        ConduitBundleBlockEntity.serverTick(h.getLevel(), one.getBlockPos(), one.getBlockState(), one);
        h.assertValueEqual(farm.energy().stored(), 2000L, "filled in a tick on Tier 2");
        h.succeedWhen(() -> h.assertTrue(farm.progress() >= 20, "the farm runs: progress " + farm.progress()));
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void theSolarPanelMakesEnergyOnlyInTheSun(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.SOLAR_PANEL);
        net.storagenexus.energy.SolarPanelBlockEntity panel = h.getBlockEntity(pos, net.storagenexus.energy.SolarPanelBlockEntity.class);
        h.assertValueEqual(net.storagenexus.energy.SolarPanelBlockEntity.OUTPUT, 20, "20 NE a tick");
        var level = h.getLevel();
        BlockPos abs = h.absolutePos(pos);
        h.assertValueEqual(net.storagenexus.energy.SolarPanelBlockEntity.RAIN_OUTPUT, 5, "a quarter in rain");
        long expected = net.storagenexus.energy.SolarPanelBlockEntity.output(level, abs);
        net.storagenexus.energy.SolarPanelBlockEntity.serverTick(level, abs, h.getBlockState(pos), panel);
        // Whether it is day depends on when the test world runs, so the output is checked against the same rule
        h.assertValueEqual(panel.energy().stored(), expected, "what the rule says");
        h.assertTrue(expected == 0 || expected == 20 || expected == 5, "0, 5 or 20");
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
        ConduitBundleBlockEntity.serverTick(h.getLevel(), conduit.getBlockPos(), conduit.getBlockState(), conduit);
        h.assertValueEqual(farm.energy().stored(), 500L, "the conduit takes 500 NE a tick out of the battery");
        h.assertValueEqual(b.energy().stored(), 99_500L, "from the battery");
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
        h.assertValueEqual(network.capacity(), 2_000L + 2_000L + 16_000L, "their capacities add up");
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

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aBatteryOnlyTakesWhatIsLeftOverAndAFarmIsServedFirst(GameTestHelper h) {
        BlockPos conduit = new BlockPos(3, 2, 3);
        h.setBlock(conduit, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity c = h.getBlockEntity(conduit, ConduitBundleBlockEntity.class);
        c.addConduit(ConduitType.ENERGY, 1);
        CombustionGeneratorBlockEntity g = generator(h);
        h.setBlock(conduit.north(), ModBlocks.IRON_BATTERY);
        var battery = h.getBlockEntity(conduit.north(), net.storagenexus.energy.BatteryBlockEntity.class);
        FarmBlockEntity farm = farm(h, conduit.south());
        farm.energy().set(0);
        battery.energy().set(0);
        // A source with a little energy: the generator is two blocks away, so use a second battery as the source
        h.setBlock(conduit.east(), ModBlocks.GOLD_BATTERY);
        var source = h.getBlockEntity(conduit.east(), net.storagenexus.energy.BatteryBlockEntity.class);
        source.energy().set(1_000_000);
        c.config(ConduitType.ENERGY, Direction.EAST).mode = SideConfig.Mode.EXTRACT;
        c.config(ConduitType.ENERGY, Direction.NORTH).mode = SideConfig.Mode.INSERT;
        c.config(ConduitType.ENERGY, Direction.SOUTH).mode = SideConfig.Mode.INSERT;
        c.changed();
        net.storagenexus.util.StorageEvents.conduitsChanged();
        netTick(h, c);
        // The source gives 500 a tick: the farm takes all of it first, the battery nothing
        h.assertValueEqual(farm.energy().stored(), 500L, "the farm is served first");
        h.assertValueEqual(battery.energy().stored(), 0L, "the battery gets nothing while a machine wants it all");
        // With the farm full, the pool builds up, and the battery takes only what is above half of the pool
        farm.energy().set(farm.energy().capacity());
        for (int i = 0; i < 6; i++) {
            net.storagenexus.util.StorageEvents.conduitsChanged();
            netTick(h, c);
        }
        h.assertTrue(battery.energy().stored() > 0, "the surplus goes to the battery: " + battery.energy().stored());
        h.assertTrue(net.storagenexus.conduit.EnergyNetwork.of(c).stored() >= 999L, "while half the pool stays in reserve: " + net.storagenexus.conduit.EnergyNetwork.of(c).stored());
        h.succeed();
    }
}
