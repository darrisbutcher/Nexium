package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.storagenexus.essentials.client.XpFarmScreen;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.xpfarm.XpFarmBlockEntity;
import net.storagenexus.essentials.xpfarm.XpFarmMenu;

/** The Experience Farm's screen in a real game: it shows the bank, and its three buttons hand over XP with real clicks. */
public class XpFarmClientTest implements FabricClientGameTest {
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
                player.level().setBlockAndUpdate(at[0], ModBlocks.XP_FARM.defaultBlockState());
                var farm = (XpFarmBlockEntity) player.level().getBlockEntity(at[0]);
                farm.energy().set(2_000);
                farm.addExperience(5_000);
                player.openMenu(farm);
            });
            ctx.waitTicks(12);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof XpFarmScreen), "the Experience Farm screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] xp farm screen: " + ctx.takeScreenshot("nexus_xp_farm_screen"));

            // Stop it making more, so the bank holds still for the sums
            world.getServer().runOnServer(server -> ((XpFarmBlockEntity) server.overworld().getBlockEntity(at[0])).energy().set(0));
            ctx.waitTicks(4);
            long bank = world.getServer().computeOnServer(server -> ((XpFarmBlockEntity) server.overworld().getBlockEntity(at[0])).experience());
            long shown = ctx.computeOnClient(mc -> ((XpFarmMenu) mc.player.containerMenu).experience());
            check(bank > 5_000 && bank < 5_200, "it banked a little more while it ran: " + bank);
            check(shown == bank, "the screen shows the whole bank, " + bank + ": " + shown);

            // +1 level: just enough for level 1 (7 points)
            click(ctx, 1);
            ctx.waitTicks(6);
            check(level(world) == 1, "+1 level took the player to level 1: " + level(world));
            check(bank(world, at[0]) == bank - 7, "and cost exactly 7 points: " + (bank - bank(world, at[0])));

            // +10 levels: on to level 11 (187 points in all)
            click(ctx, 2);
            ctx.waitTicks(6);
            check(level(world) == 11, "+10 levels took the player to level 11: " + level(world));
            check(bank(world, at[0]) == bank - 187, "and the two together cost 187: " + (bank - bank(world, at[0])));

            // Collect all: the rest
            click(ctx, 0);
            ctx.waitTicks(6);
            check(bank(world, at[0]) == 0, "Collect all emptied the bank: " + bank(world, at[0]));
            int total = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).totalExperience);
            check(total == bank, "the player has all " + bank + " points: " + total);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] xp farm screen empty: " + ctx.takeScreenshot("nexus_xp_farm_screen_empty"));
        }
    }

    private static int level(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).experienceLevel);
    }

    private static long bank(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer(server -> ((XpFarmBlockEntity) server.overworld().getBlockEntity(pos)).experience());
    }

    /** Presses the left mouse button on one of the three buttons (0 collect all, 1 one level, 2 ten levels). */
    private static void click(ClientGameTestContext ctx, int index) {
        double[] at = ctx.computeOnClient(mc -> {
            int[] centre = ((XpFarmScreen) mc.gui.screen()).buttonCentre(index);
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
