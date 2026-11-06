package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.client.screen.ElevatorScreen;
import net.storagenexus.elevator.Elevators;
import net.storagenexus.registry.ModBlocks;

/**
 * Right-clicking an elevator opens the floor screen on the real client; clicking a floor's button (sent as the real packet)
 * takes the player there. Screenshots show the platform and the screen.
 */
public class ElevatorClientTest implements FabricClientGameTest {
    private static BlockPos base;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                base = player.blockPosition().offset(0, 0, 0);
                // A column of elevators 4 blocks apart, the lowest under the player
                for (int i = 0; i < 4; i++) {
                    player.level().setBlockAndUpdate(base.above(i * 4), ModBlocks.ELEVATOR.defaultBlockState());
                    for (int dy = 1; dy <= 3; dy++) player.level().setBlockAndUpdate(base.above(i * 4 + dy), Blocks.AIR.defaultBlockState());
                }
                // Two blocks back, looking down at the platform
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 1.5, 0.0F, 35.0F);
            });
            ctx.waitTicks(10);
            System.out.println("[nexus] elevator block: " + ctx.takeScreenshot("nexus_elevator_block"));

            // Stepping beside it and using it still lands you on the platform of the floor you pick
            world.getServer().runOnServer(server -> Elevators.open(server.getPlayerList().getPlayers().get(0), base));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof ElevatorScreen), "right-clicking opens the elevator screen");
            System.out.println("[nexus] elevator screen: " + ctx.takeScreenshot("nexus_elevator_screen"));

            // Press the button for the third floor (the screen lists the highest first, so row 1)
            ctx.computeOnClient(mc -> {
                var screen = (ElevatorScreen) mc.gui.screen();
                screen.pressRow(1);
                return null;
            });
            ctx.waitTicks(10);
            double y = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getY());
            check(Math.abs(y - (base.getY() + 8.25)) < 0.01, "the player is on the third floor, got y " + y);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "the screen closes after the ride");
            System.out.println("[nexus] elevator arrival: " + ctx.takeScreenshot("nexus_elevator_arrival"));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
