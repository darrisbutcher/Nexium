package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluids;
import net.nexium.client.screen.ThermalGeneratorScreen;
import net.nexium.energy.ThermalGeneratorBlockEntity;
import net.nexium.registry.ModBlocks;

/** The three Thermal Generators side by side, and the output button on the top one's screen answering a real click. */
public class ThermalTiersClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                var level = server.overworld();
                BlockPos c = player.blockPosition().offset(-2, 0, 4);
                at[0] = c;
                var blocks = new net.minecraft.world.level.block.Block[]{ModBlocks.THERMAL_GENERATOR, ModBlocks.ADVANCED_THERMAL_GENERATOR, ModBlocks.NEXIUM_THERMAL_GENERATOR};
                for (int i = 0; i < 3; i++) {
                    level.setBlockAndUpdate(c.east(i * 2), blocks[i].defaultBlockState());
                    var g = (ThermalGeneratorBlockEntity) level.getBlockEntity(c.east(i * 2));
                    g.tank().variant = FluidVariant.of(Fluids.LAVA);
                    g.tank().amount = (i + 1) * 2 * FluidConstants.BUCKET;
                }
                player.snapTo(c.getX() + 2.5, c.getY(), c.getZ() - 3.5, 0.0F, 18.0F);
            });
            ctx.waitTicks(40);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] thermal tiers: " + ctx.takeScreenshot("nexus_thermal_tiers"));

            // The Nexium one's screen, and the output button
            BlockPos top = at[0].east(4);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.openMenu((ThermalGeneratorBlockEntity) server.overworld().getBlockEntity(top));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof ThermalGeneratorScreen);
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] thermal screen full: " + ctx.takeScreenshot("nexus_thermal_screen_full"));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((ThermalGeneratorScreen) mc.gui.screen()).throttleCentre()));
            ctx.waitTicks(8);
            check(world.getServer().computeOnServer(server -> ((ThermalGeneratorBlockEntity) server.overworld().getBlockEntity(top)).throttle()) == 0,
                    "a click on the output button steps from 100% to off");
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((ThermalGeneratorScreen) mc.gui.screen()).throttleCentre()));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((ThermalGeneratorScreen) mc.gui.screen()).throttleCentre()));
            ctx.waitTicks(8);
            check(world.getServer().computeOnServer(server -> ((ThermalGeneratorBlockEntity) server.overworld().getBlockEntity(top)).throttle()) == 2,
                    "two more clicks reach 50%");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] thermal screen half: " + ctx.takeScreenshot("nexus_thermal_screen_half"));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
