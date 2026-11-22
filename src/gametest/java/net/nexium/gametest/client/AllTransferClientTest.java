package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * ShiftTransfer, ScrollTransfer: related client tests that used to each start a world of their own. They run one after the other in
 * one world, with the same checks and screenshots as before; starting a world is a good part of what a small client test costs.
 */
public class AllTransferClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            run(ctx, world, new ShiftTransferClientPart()::part, "ShiftTransfer");
            run(ctx, world, new ScrollTransferClientPart()::part, "ScrollTransfer");
        }
    }

    private interface Part {
        void run(ClientGameTestContext ctx, TestSingleplayerContext world);
    }

    /** Runs one part, then puts the world back in shape for the next: no screen open, no keys held, nothing carried, no stray items or mobs. */
    private static void run(ClientGameTestContext ctx, TestSingleplayerContext world, Part part, String name) {
        System.out.println("[nexus] part " + name);
        part.run(ctx, world);
        ctx.setScreen(() -> null);
        ctx.waitTicks(2);
        world.getServer().runCommand("clear @a");
        world.getServer().runCommand("kill @e[type=!player]");
        world.getServer().runCommand("gamemode survival @a");
        ctx.waitTicks(5);
    }
}
