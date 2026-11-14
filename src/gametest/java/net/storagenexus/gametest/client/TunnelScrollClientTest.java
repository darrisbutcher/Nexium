package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.registry.ModItems;

/**
 * Holding the tunnel key and turning the scroll wheel steps the 3x3 Mining tunnel up and down (off, 2, 3, 8, 16, 24, 32) in a
 * real game, with real input, and the hotbar slot stays put while it is held; without the key the wheel is the hotbar's.
 */
public class TunnelScrollClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ItemStack aiot = new ItemStack(ModItems.AIOTS.get(AiotTier.DIAMOND));
                var slots = AiotModules.slots(aiot);
                slots.set(0, new ItemStack(ModItems.MODULES.get(ModuleType.AREA_MINING)));
                AiotModules.setSlots(aiot, slots);
                player.getInventory().setItem(0, aiot);
                player.getInventory().setItem(1, new ItemStack(Items.STONE, 5));
                player.getInventory().setSelectedSlot(0);
            });
            ctx.waitTicks(10);
            check(selected(ctx) == 0 && tunnel(world) == 1, "it starts on the AIOT in slot 0, tunnel off");

            // Without the key the wheel is the hotbar's, as always
            ctx.getInput().scroll(-1);
            ctx.waitTicks(3);
            check(selected(ctx) == 1, "the wheel alone moves the hotbar slot, it is on " + selected(ctx));
            ctx.getInput().scroll(1);
            ctx.waitTicks(3);
            check(selected(ctx) == 0 && tunnel(world) == 1, "and back, with the tunnel untouched");

            // A plain press of the key changes nothing
            ctx.getInput().pressKey(ToolMenuClient.STEP_TUNNEL);
            ctx.waitTicks(4);
            check(tunnel(world) == 1, "a plain press does not step it, it is " + tunnel(world));

            // Held, the wheel steps it: up through every step, then no further
            ctx.getInput().holdKey(ToolMenuClient.STEP_TUNNEL);
            ctx.waitTicks(2);
            int[] up = {2, 3, 8, 16, 24, 32, 32};
            for (int expected : up) {
                ctx.getInput().scroll(1);
                ctx.waitTicks(4);
                check(tunnel(world) == expected, "the wheel up steps to " + expected + ", it is " + tunnel(world));
                check(selected(ctx) == 0, "and the hotbar stays on slot 0, it is on " + selected(ctx));
            }
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] tunnel scroll top: " + ctx.takeScreenshot("nexus_tunnel_scroll_top"));

            // And down again, to off, and no further
            int[] down = {24, 16, 8, 3, 2, 1, 1};
            for (int expected : down) {
                ctx.getInput().scroll(-1);
                ctx.waitTicks(4);
                check(tunnel(world) == expected, "the wheel down steps to " + expected + ", it is " + tunnel(world));
                check(selected(ctx) == 0, "and the hotbar stays on slot 0, it is on " + selected(ctx));
            }

            // Let go of the key: the wheel is the hotbar's again
            ctx.getInput().releaseKey(ToolMenuClient.STEP_TUNNEL);
            ctx.waitTicks(2);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(3);
            check(selected(ctx) == 1 && tunnel(world) == 1, "released, the wheel moves the hotbar again");

            // Holding the key with something else in hand leaves the wheel alone
            ctx.getInput().holdKey(ToolMenuClient.STEP_TUNNEL);
            ctx.waitTicks(2);
            ctx.getInput().scroll(1);
            ctx.waitTicks(3);
            check(selected(ctx) == 0, "with stone in hand the key does not take the wheel, it is on " + selected(ctx));
            ctx.getInput().releaseKey(ToolMenuClient.STEP_TUNNEL);
        }
    }

    private static int selected(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
    }

    private static int tunnel(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> AiotModules.tunnelLength(server.getPlayerList().getPlayers().get(0).getInventory().getItem(0)));
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
