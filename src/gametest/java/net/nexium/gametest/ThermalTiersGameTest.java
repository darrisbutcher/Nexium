package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.nexium.energy.ThermalGeneratorBlock;
import net.nexium.energy.ThermalGeneratorBlockEntity;
import net.nexium.registry.ModBlocks;

/** The Thermal Generator's three tiers (80, 320 and 1,280 NE a tick) and its output throttle (off, 25%, 50%, 75%, 100%). */
public class ThermalTiersGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static ThermalGeneratorBlockEntity place(GameTestHelper h, Block block, int buckets) {
        h.setBlock(POS, net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(POS, block);
        ThermalGeneratorBlockEntity g = h.getBlockEntity(POS, ThermalGeneratorBlockEntity.class);
        g.tank().variant = FluidVariant.of(Fluids.LAVA);
        g.tank().amount = buckets * FluidConstants.BUCKET;
        return g;
    }

    private static void tick(GameTestHelper h, ThermalGeneratorBlockEntity g, int times) {
        for (int i = 0; i < times; i++) ThermalGeneratorBlockEntity.serverTick(h.getLevel(), g.getBlockPos(), g.getBlockState(), g);
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void eachTierMakesMoreAndBurnsAFullBucketFaster(GameTestHelper h) {
        Block[] blocks = {ModBlocks.THERMAL_GENERATOR, ModBlocks.ADVANCED_THERMAL_GENERATOR, ModBlocks.NEXIUM_THERMAL_GENERATOR};
        int[] output = {80, 320, 1280}, bucketTicks = {10_000, 2_000, 500}, tank = {4, 8, 16};
        long[] capacity = {80_000, 320_000, 1_280_000};
        for (int t = 0; t < 3; t++) {
            ThermalGeneratorBlockEntity g = place(h, blocks[t], 1);
            h.assertValueEqual(((ThermalGeneratorBlock) blocks[t]).tier(), t + 1, "tier of block " + t);
            h.assertValueEqual(g.tankCapacity(), tank[t] * FluidConstants.BUCKET, "tank of tier " + (t + 1));
            h.assertValueEqual(g.energy().capacity(), capacity[t], "buffer of tier " + (t + 1));
            tick(h, g, 10);
            h.assertValueEqual(g.energy().stored(), 10L * output[t], "output of tier " + (t + 1));
            h.assertValueEqual(g.burnLeft(), bucketTicks[t] - 10, "a bucket lasts " + bucketTicks[t] + " ticks at tier " + (t + 1));
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void theThrottleScalesOutputAndLavaUseTogether(GameTestHelper h) {
        ThermalGeneratorBlockEntity g = place(h, ModBlocks.ADVANCED_THERMAL_GENERATOR, 2);
        g.setThrottle(2); // 50%
        tick(h, g, 100);
        h.assertValueEqual(g.energy().stored(), 100L * 160, "half of 320 NE a tick");
        // Half speed burns half as much: 100 ticks used 100 * 2 quarter ticks of the 8,000 in a bucket
        h.assertValueEqual(g.burnLeft(), 2_000 - 50, "after 100 ticks at half speed, 50 ticks of the bucket are gone");
        g.setThrottle(0);
        long before = g.energy().stored();
        tick(h, g, 20);
        h.assertValueEqual(g.energy().stored(), before, "off makes nothing");
        h.assertFalse(h.getBlockState(POS).getValue(ThermalGeneratorBlock.LIT), "and is not lit");
        h.assertValueEqual(g.lava(), FluidConstants.BUCKET, "and burns no more lava");
        g.setThrottle(4);
        tick(h, g, 1);
        h.assertTrue(h.getBlockState(POS).getValue(ThermalGeneratorBlock.LIT), "full again lights it");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aBucketGivesTheSameEnergyAtAnySpeed(GameTestHelper h) {
        for (int step = 1; step <= 4; step++) {
            ThermalGeneratorBlockEntity g = place(h, ModBlocks.NEXIUM_THERMAL_GENERATOR, 1);
            g.setThrottle(step);
            tick(h, g, 4_000);
            h.assertValueEqual(g.energy().stored(), 500L * 1280, "a bucket (500 ticks at 1,280 NE) at throttle " + step);
            h.assertValueEqual(g.burnLeft(), 0, "is used up at throttle " + step);
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void theButtonStepsThroughTheOutputs(GameTestHelper h) {
        ThermalGeneratorBlockEntity g = place(h, ModBlocks.THERMAL_GENERATOR, 0);
        var menu = new net.nexium.energy.ThermalGeneratorMenu(0, h.makeMockServerPlayerInLevel().getInventory(), g);
        var player = h.makeMockServerPlayerInLevel();
        h.assertValueEqual(g.throttle(), 4, "starts at full");
        menu.clickMenuButton(player, net.nexium.energy.ThermalGeneratorMenu.BTN_THROTTLE);
        h.assertValueEqual(g.throttle(), 0, "forward from full wraps to off");
        menu.clickMenuButton(player, net.nexium.energy.ThermalGeneratorMenu.BTN_THROTTLE);
        h.assertValueEqual(g.throttle(), 1, "then 25%");
        menu.clickMenuButton(player, net.nexium.energy.ThermalGeneratorMenu.BTN_THROTTLE_BACK);
        menu.clickMenuButton(player, net.nexium.energy.ThermalGeneratorMenu.BTN_THROTTLE_BACK);
        h.assertValueEqual(g.throttle(), 4, "back twice from 25% reaches full");
        h.succeed();
    }
}
