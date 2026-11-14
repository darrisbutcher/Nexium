package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.client.screen.NetworkBusScreen;
import net.storagenexus.network.NetworkBusBlock;
import net.storagenexus.network.NetworkBusBlockEntity;
import net.storagenexus.network.NetworkBusMenu;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.util.Menus;

/** The Import/Export Bus screen: six face buttons, usable only where there is an inventory, and a click turns the bus. */
public class NetworkBusClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                at[0] = player.blockPosition().offset(3, 2, 3);
                var level = player.level();
                level.setBlockAndUpdate(at[0].west(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(at[0].east(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(at[0], ModBlocks.NETWORK_BUS.defaultBlockState().setValue(NetworkBusBlock.FACING, Direction.WEST));
                var bus = (NetworkBusBlockEntity) level.getBlockEntity(at[0]);
                Menus.open(player, Component.literal("Import/Export Bus"), at[0], (id, inv, p) -> new NetworkBusMenu(id, inv, bus));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof NetworkBusScreen), "the bus screen opens");

            // Only the two sides with a chest can be chosen
            for (Direction d : Direction.values()) {
                boolean want = d == Direction.WEST || d == Direction.EAST;
                boolean active = ctx.computeOnClient(mc -> ((NetworkBusScreen) mc.gui.screen()).faceButtonActive(d));
                check(active == want, "the " + d + " button is " + (want ? "usable" : "greyed out") + ", it reads " + active);
            }
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] bus screen: " + ctx.takeScreenshot("nexus_bus_screen"));

            // A real click on the East button turns the bus
            click(ctx, Direction.EAST);
            ctx.waitTicks(8);
            check(facing(world, at[0]) == Direction.EAST, "clicking East turned the bus to face East, it faces " + facing(world, at[0]));
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] bus screen east: " + ctx.takeScreenshot("nexus_bus_screen_east"));

            // A greyed-out button does nothing
            click(ctx, Direction.UP);
            ctx.waitTicks(8);
            check(facing(world, at[0]) == Direction.EAST, "a greyed-out button does not turn it, it faces " + facing(world, at[0]));

            // A chest on the north appears as a new choice while the screen is open
            world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(at[0].north(), Blocks.CHEST.defaultBlockState()));
            ctx.waitTicks(8);
            check(ctx.computeOnClient(mc -> ((NetworkBusScreen) mc.gui.screen()).faceButtonActive(Direction.NORTH)), "a new chest makes its side usable at once");
            click(ctx, Direction.NORTH);
            ctx.waitTicks(8);
            check(facing(world, at[0]) == Direction.NORTH, "and clicking it turns the bus there, it faces " + facing(world, at[0]));
        }
    }

    private static Direction facing(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer(server -> server.overworld().getBlockState(pos).getValue(NetworkBusBlock.FACING));
    }

    /** Presses the left mouse button on a side's button. */
    private static void click(ClientGameTestContext ctx, Direction side) {
        double[] at = ctx.computeOnClient(mc -> {
            int[] centre = ((NetworkBusScreen) mc.gui.screen()).faceButtonCentre(side);
            double scale = mc.getWindow().getGuiScale();
            return new double[]{centre[0] * scale, centre[1] * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(2);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
