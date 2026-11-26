package net.nexium.gametest;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.EnergyNetwork;
import net.nexium.energy.CombustionGeneratorBlockEntity;
import net.nexium.energy.PowerMeter;
import net.nexium.energy.PowerReportPayload;
import net.nexium.essentials.farm.FarmBlockEntity;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.registry.ModBlocks;

/**
 * The Power Meter: it finds the network of the Energy Conduit it sits against, tells producers from consumers and what each
 * moves, and its controls (priority, off switch) change the conduit sides that feed a machine and stop it taking power.
 */
public class PowerMeterGameTest {
    private static final BlockPos GENERATOR = new BlockPos(1, 2, 2), C1 = new BlockPos(2, 2, 2), C2 = new BlockPos(3, 2, 2),
            FARM = new BlockPos(4, 2, 2), METER = new BlockPos(2, 2, 1);

    private static void build(GameTestHelper h) {
        h.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR);
        h.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 16));
        h.setBlock(C1, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(C2, ModBlocks.CONDUIT_BUNDLE);
        h.getBlockEntity(C1, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.getBlockEntity(C2, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.setBlock(FARM, net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        h.setBlock(METER, ModBlocks.POWER_METER);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aMeterFacadeOnAConduitWorksLikeAMeterAgainstIt(GameTestHelper h) {
        BlockPos face = new BlockPos(2, 2, 3), energy = new BlockPos(3, 2, 3);
        h.setBlock(face, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(energy, ModBlocks.CONDUIT_BUNDLE);
        var faced = h.getBlockEntity(face, ConduitBundleBlockEntity.class);
        faced.addConduit(ConduitType.ITEM, 1);
        faced.setFacade(ModBlocks.POWER_METER.defaultBlockState());
        h.assertTrue(PowerMeter.isMeter(h.getLevel(), h.absolutePos(face)), "a conduit wearing the meter is a meter");
        h.assertTrue(PowerMeter.networkOf(h.getLevel(), h.absolutePos(face)) == null, "with no Energy Conduit in reach it has no network");
        h.getBlockEntity(energy, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.assertTrue(PowerMeter.networkOf(h.getLevel(), h.absolutePos(face)) != null, "an Energy Conduit beside it gives it a network");
        faced.addConduit(ConduitType.ENERGY, 1);
        h.assertTrue(PowerMeter.networkOf(h.getLevel(), h.absolutePos(face)) == EnergyNetwork.of(faced), "carrying Energy itself, it reports its own network");
        faced.setFacade(null);
        h.assertFalse(PowerMeter.isMeter(h.getLevel(), h.absolutePos(face)), "without the facade it is just a conduit");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aMeterAwayFromConduitsHasNoNetwork(GameTestHelper h) {
        h.setBlock(METER, ModBlocks.POWER_METER);
        h.assertTrue(PowerMeter.networkOf(h.getLevel(), h.absolutePos(METER)) == null, "no Energy Conduit beside it, no network");
        h.setBlock(C1, ModBlocks.CONDUIT_BUNDLE);
        h.assertTrue(PowerMeter.networkOf(h.getLevel(), h.absolutePos(METER)) == null, "a conduit without an energy conduit is not a network");
        h.getBlockEntity(C1, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.assertTrue(PowerMeter.networkOf(h.getLevel(), h.absolutePos(METER)) != null, "an Energy Conduit beside it is");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 300)
    public void theMeterSeesWhoMakesAndUsesPowerAndTheControlsWork(GameTestHelper h) {
        build(h);
        BlockPos meter = h.absolutePos(METER), farmPos = h.absolutePos(FARM), genPos = h.absolutePos(GENERATOR);
        int[] phase = {0};
        FarmBlockEntity farm = h.getBlockEntity(FARM, FarmBlockEntity.class);
        h.succeedWhen(() -> {
            EnergyNetwork network = PowerMeter.networkOf(h.getLevel(), meter);
            h.assertTrue(network != null, "the meter has a network");
            EnergyNetwork.Report r = network.report(h.getLevel());
            if (phase[0] == 0) {
                EnergyNetwork.Endpoint gen = r.endpoints().stream().filter(e -> e.pos().equals(genPos)).findFirst().orElse(null);
                EnergyNetwork.Endpoint use = r.endpoints().stream().filter(e -> e.pos().equals(farmPos)).findFirst().orElse(null);
                h.assertTrue(gen != null && use != null, "the generator and the farm are both listed: " + r.endpoints());
                h.assertValueEqual(gen.role(), EnergyNetwork.Role.PRODUCER, "the generator makes power");
                h.assertValueEqual(use.role(), EnergyNetwork.Role.CONSUMER, "the farm uses it");
                h.assertTrue(r.produced() > 0 && r.used() > 0, "something is made and used: " + r.produced() + " / " + r.used());
                h.assertTrue(farm.progress() > 0 || farm.energy().stored() > 0, "and the farm really has power");
                // Priority: set on the conduit side that feeds the farm
                h.assertTrue(network.control(h.getLevel(), farmPos, 5, false), "the control finds the farm's side");
                ConduitBundleBlockEntity c2 = h.getBlockEntity(C2, ConduitBundleBlockEntity.class);
                h.assertValueEqual(c2.config(ConduitType.ENERGY, Direction.EAST).priority, 5, "the priority is on the conduit side beside the farm");
                // Off switch
                h.assertTrue(network.control(h.getLevel(), farmPos, 0, true), "and switches it off");
                h.assertTrue(c2.config(ConduitType.ENERGY, Direction.EAST).paused, "the side is paused");
                phase[0] = 1;
                farm.energy().set(0);
                h.assertTrue(false, "switched off, now waiting for it to stay without power");
            }
            h.assertTrue(r.endpoints().stream().anyMatch(e -> e.pos().equals(farmPos) && e.paused()), "the report shows it paused");
            h.assertTrue(r.used() < 0.01f, "nothing is used while it is off: " + r.used());
            h.assertValueEqual(farm.energy().stored(), 0L, "and the farm gets none");
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 300)
    public void theMeterListsAChargingStationAsAConsumer(GameTestHelper h) {
        build(h);
        h.setBlock(FARM, ModBlocks.CHARGING_STATION);
        h.getBlockEntity(FARM, net.nexium.energy.ChargingStationBlockEntity.class).slots().setItem(0, new ItemStack(net.nexium.registry.ModItems.IRON_CELL));
        BlockPos meter = h.absolutePos(METER), stationPos = h.absolutePos(FARM);
        h.succeedWhen(() -> {
            EnergyNetwork network = PowerMeter.networkOf(h.getLevel(), meter);
            h.assertTrue(network != null, "the meter has a network");
            EnergyNetwork.Report r = network.report(h.getLevel());
            EnergyNetwork.Endpoint use = r.endpoints().stream().filter(e -> e.pos().equals(stationPos)).findFirst().orElse(null);
            h.assertTrue(use != null, "the Charging Station is listed: " + r.endpoints());
            h.assertValueEqual(use.role(), EnergyNetwork.Role.CONSUMER, "as a consumer");
            h.assertTrue(use.perTick() > 0, "and the meter sees it taking power: " + use.perTick());
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 300)
    public void theMeterListsAMachineThatOnlyABatteryFeeds(GameTestHelper h) {
        build(h);
        BlockPos batteryAt = new BlockPos(3, 2, 3), stationAt = new BlockPos(3, 2, 4);
        h.setBlock(batteryAt, ModBlocks.IRON_BATTERY);
        h.getBlockEntity(batteryAt, net.nexium.energy.BatteryBlockEntity.class).energy().set(100_000);
        h.setBlock(stationAt, ModBlocks.CHARGING_STATION);
        h.getBlockEntity(stationAt, net.nexium.energy.ChargingStationBlockEntity.class).slots().setItem(0, new ItemStack(net.nexium.registry.ModItems.IRON_CELL));
        BlockPos meter = h.absolutePos(METER), station = h.absolutePos(stationAt);
        h.succeedWhen(() -> {
            EnergyNetwork network = PowerMeter.networkOf(h.getLevel(), meter);
            h.assertTrue(network != null, "the meter has a network");
            EnergyNetwork.Report r = network.report(h.getLevel());
            EnergyNetwork.Endpoint use = r.endpoints().stream().filter(e -> e.pos().equals(station)).findFirst().orElse(null);
            h.assertTrue(use != null, "the station only the Battery touches is listed: " + r.endpoints());
            h.assertTrue(use.direct() && use.role() == EnergyNetwork.Role.CONSUMER, "as a machine fed by a Battery");
            h.assertTrue(use.perTick() > 0, "and the meter sees it taking power: " + use.perTick());
            h.assertTrue(!network.control(h.getLevel(), station, 5, false), "its priority is not the network's to set");
        });
    }

    /** The meter's row for the generator at {@code GENERATOR}, once a full second has been counted; null before that. */
    private static EnergyNetwork.Endpoint generatorRow(GameTestHelper h) {
        EnergyNetwork network = PowerMeter.networkOf(h.getLevel(), h.absolutePos(METER));
        h.assertTrue(network != null, "the meter has a network");
        EnergyNetwork.Report r = network.report(h.getLevel());
        BlockPos genPos = h.absolutePos(GENERATOR);
        return r.endpoints().stream().filter(e -> e.pos().equals(genPos)).findFirst().orElse(null);
    }

    /** A burning generator, with blaze rods, on its own conduit: the meter shows the 20 NE/t it makes. */
    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 200)
    public void aBurningGeneratorShowsItsFullOutputInTheMeter(GameTestHelper h) {
        h.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR);
        h.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class).setItem(0, new ItemStack(Items.BLAZE_ROD, 8));
        h.setBlock(C1, ModBlocks.CONDUIT_BUNDLE);
        h.getBlockEntity(C1, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.setBlock(METER, ModBlocks.POWER_METER);
        h.succeedWhen(() -> {
            h.assertTrue(h.getTick() > 70, "waiting for a few full seconds to be counted");
            EnergyNetwork.Endpoint gen = generatorRow(h);
            h.assertTrue(gen != null, "the generator is listed");
            h.assertTrue(Math.abs(gen.perTick() - CombustionGeneratorBlockEntity.OUTPUT) < 1.5f, "the meter shows about 20 NE/t for the generator, not " + gen.perTick());
        });
    }

    /** The same, with a Battery touching the generator, which the generator also fills directly. */
    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 200)
    public void aGeneratorTouchingABatteryStillShowsWhatItMakes(GameTestHelper h) {
        h.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR);
        h.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class).setItem(0, new ItemStack(Items.BLAZE_ROD, 8));
        h.setBlock(C1, ModBlocks.CONDUIT_BUNDLE);
        h.getBlockEntity(C1, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.setBlock(METER, ModBlocks.POWER_METER);
        h.setBlock(new BlockPos(1, 2, 3), ModBlocks.IRON_BATTERY); // touches the generator, not the conduit
        h.succeedWhen(() -> {
            h.assertTrue(h.getTick() > 70, "waiting for a few full seconds to be counted");
            EnergyNetwork.Endpoint gen = generatorRow(h);
            h.assertTrue(gen != null, "the generator is listed");
            h.assertTrue(Math.abs(gen.perTick() - CombustionGeneratorBlockEntity.OUTPUT) < 3f, "with a battery touching it, the meter shows " + gen.perTick() + " NE/t for the generator, not 40");
        });
    }

    /** The same, with a farm touching the generator, which the generator also feeds directly. */
    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 200)
    public void aGeneratorTouchingAMachineStillShowsWhatItMakes(GameTestHelper h) {
        h.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR);
        h.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class).setItem(0, new ItemStack(Items.BLAZE_ROD, 8));
        h.setBlock(C1, ModBlocks.CONDUIT_BUNDLE);
        h.getBlockEntity(C1, ConduitBundleBlockEntity.class).addConduit(ConduitType.ENERGY, 1);
        h.setBlock(METER, ModBlocks.POWER_METER);
        h.setBlock(new BlockPos(1, 2, 3), net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE)); // touches the generator, not the conduit
        h.succeedWhen(() -> {
            h.assertTrue(h.getTick() > 70, "waiting for a few full seconds to be counted");
            EnergyNetwork.Endpoint gen = generatorRow(h);
            h.assertTrue(gen != null, "the generator is listed");
            h.assertTrue(Math.abs(gen.perTick() - CombustionGeneratorBlockEntity.OUTPUT) < 3f, "with a machine touching it, the meter shows " + gen.perTick() + " NE/t for the generator, not 40");
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aReportSurvivesTheJourneyToTheScreen(GameTestHelper h) {
        EnergyNetwork.Report report = new EnergyNetwork.Report(1234, 10_000, 500, 250_000, 12.5f, 20f, -7.5f, true, 90,
                java.util.List.of(new EnergyNetwork.Endpoint(new BlockPos(1, 2, 3), EnergyNetwork.Role.CONSUMER, 20f, -5, true),
                        new EnergyNetwork.Endpoint(new BlockPos(4, 5, 6), EnergyNetwork.Role.BATTERY, -7.5f, 0, false),
                        new EnergyNetwork.Endpoint(new BlockPos(7, 8, 9), EnergyNetwork.Role.CONSUMER, 3f, 0, false, true)),
                new float[EnergyNetwork.HISTORY], new float[EnergyNetwork.HISTORY]);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        PowerReportPayload.STREAM_CODEC.encode(buf, new PowerReportPayload(new BlockPos(9, 9, 9), true, report));
        PowerReportPayload back = PowerReportPayload.STREAM_CODEC.decode(buf);
        h.assertValueEqual(back.meter(), new BlockPos(9, 9, 9), "the meter's place");
        h.assertTrue(back.open(), "the open flag");
        h.assertValueEqual(back.report().endpoints(), report.endpoints(), "the endpoints, priorities and switches");
        h.assertValueEqual(back.report().secondsLeft(), 90, "the time left");
        h.assertTrue(back.report().deficit(), "the deficit flag");
        h.assertValueEqual(back.report().batteryCapacity(), 250_000L, "the battery size");
        buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        PowerReportPayload.STREAM_CODEC.encode(buf, new PowerReportPayload(new BlockPos(9, 9, 9), false, null));
        h.assertTrue(PowerReportPayload.STREAM_CODEC.decode(buf).report() == null, "no network travels as none");
        h.succeed();
    }
}
