package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.nexium.essentials.client.FarmScreen;
import net.nexium.essentials.client.IronFarmScreen;
import net.nexium.essentials.client.XpFarmScreen;
import net.nexium.essentials.farm.FarmBlockEntity;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.essentials.ironfarm.IronFarmBlockEntity;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.xpfarm.XpFarmBlockEntity;
import net.nexium.client.screen.FreezerScreen;
import net.nexium.freezer.FreezerBlockEntity;

import java.util.function.Function;

/** The Overclock button on the three farm screens in a real game: a real click steps the setting, Shift goes back. */
public class OverclockClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];

            // A farm with the longest title, to see the button clear it
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                at[0] = player.blockPosition().offset(3, 2, 3);
                player.level().setBlockAndUpdate(at[0], ModBlocks.FARMS.get(FarmKind.ZOMBIFIED_PIGLIN).defaultBlockState());
                var farm = (FarmBlockEntity) player.level().getBlockEntity(at[0]);
                farm.energy().set(farm.energy().capacity());
                player.openMenu(farm);
            });
            ctx.waitTicks(12);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmScreen), "the farm screen opens");
            check(level(world, at[0]) == 0, "it starts off");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] overclock off: " + ctx.takeScreenshot("nexus_overclock_farm_off"));

            click(ctx, mc -> ((FarmScreen) mc.gui.screen()).overclockCentre());
            check(level(world, at[0]) == 1, "a click made it 2x: " + level(world, at[0]));
            click(ctx, mc -> ((FarmScreen) mc.gui.screen()).overclockCentre());
            click(ctx, mc -> ((FarmScreen) mc.gui.screen()).overclockCentre());
            check(level(world, at[0]) == 3, "three clicks made it 8x: " + level(world, at[0]));
            // The buffer grew with it, and the screen can hover
            long capacity = world.getServer().computeOnServer(server -> ((FarmBlockEntity) server.overworld().getBlockEntity(at[0])).energy().capacity());
            check(capacity == FarmKind.ZOMBIFIED_PIGLIN.energyPerTick() * 200L * 8, "the buffer is 8x: " + capacity);
            world.getServer().runOnServer(server -> {
                var farm = (FarmBlockEntity) server.overworld().getBlockEntity(at[0]);
                farm.energy().set(farm.energy().capacity());
            });
            ctx.waitTicks(30);
            System.out.println("[nexus] overclock 8x with the tip: " + ctx.takeScreenshot("nexus_overclock_farm_8x_tip"));
            // Move off the button, to see the screen itself
            ctx.getInput().setCursorPos(0, 0);
            world.getServer().runOnServer(server -> {
                var farm = (FarmBlockEntity) server.overworld().getBlockEntity(at[0]);
                farm.energy().set(farm.energy().capacity());
            });
            ctx.waitTicks(4);
            System.out.println("[nexus] overclock 8x: " + ctx.takeScreenshot("nexus_overclock_farm_8x"));
            // Shift-click goes back
            ctx.getInput().holdKey(InputConstants.KEY_LSHIFT);
            click(ctx, mc -> ((FarmScreen) mc.gui.screen()).overclockCentre());
            ctx.getInput().releaseKey(InputConstants.KEY_LSHIFT);
            check(level(world, at[0]) == 2, "Shift-click went back to 4x: " + level(world, at[0]));

            // The Iron Farm
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.closeContainer();
                BlockPos pos = at[0].south(1);
                player.level().setBlockAndUpdate(pos, ModBlocks.IRON_FARM.defaultBlockState());
                var farm = (IronFarmBlockEntity) player.level().getBlockEntity(pos);
                farm.setOverclock(2);
                farm.energy().set(farm.energy().capacity());
                player.openMenu(farm);
            });
            ctx.waitTicks(12);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof IronFarmScreen), "the Iron Farm screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] overclock iron farm 4x: " + ctx.takeScreenshot("nexus_overclock_iron_farm_4x"));
            click(ctx, mc -> ((IronFarmScreen) mc.gui.screen()).overclockCentre());
            int iron = world.getServer().computeOnServer(server -> ((IronFarmBlockEntity) server.overworld().getBlockEntity(at[0].south(1))).overclock());
            check(iron == 3, "a click took the Iron Farm from 4x to 8x: " + iron);

            // The Experience Farm
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.closeContainer();
                BlockPos pos = at[0].south(2);
                player.level().setBlockAndUpdate(pos, ModBlocks.XP_FARM.defaultBlockState());
                var farm = (XpFarmBlockEntity) player.level().getBlockEntity(pos);
                farm.setOverclock(3);
                farm.energy().set(farm.energy().capacity());
                farm.addExperience(5_000);
                player.openMenu(farm);
            });
            ctx.waitTicks(12);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof XpFarmScreen), "the Experience Farm screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] overclock xp farm 8x: " + ctx.takeScreenshot("nexus_overclock_xp_farm_8x"));
            click(ctx, mc -> ((XpFarmScreen) mc.gui.screen()).overclockCentre());
            int xp = world.getServer().computeOnServer(server -> ((XpFarmBlockEntity) server.overworld().getBlockEntity(at[0].south(2))).overclock());
            check(xp == 0, "a click took the Experience Farm from 8x to off: " + xp);

            // The Freezer
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.closeContainer();
                BlockPos pos = at[0].north(1);
                player.level().setBlockAndUpdate(pos, net.nexium.registry.ModBlocks.FREEZER.defaultBlockState());
                var freezer = (FreezerBlockEntity) player.level().getBlockEntity(pos);
                freezer.setOverclock(2);
                freezer.setWater(FreezerBlockEntity.CAPACITY);
                freezer.energy().set(freezer.energy().capacity());
                player.openMenu(freezer);
            });
            ctx.waitTicks(12);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FreezerScreen), "the Freezer screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] overclock freezer 4x: " + ctx.takeScreenshot("nexus_overclock_freezer_4x"));
            click(ctx, mc -> ((FreezerScreen) mc.gui.screen()).overclockCentre());
            int freezerLevel = world.getServer().computeOnServer(server -> ((FreezerBlockEntity) server.overworld().getBlockEntity(at[0].north(1))).overclock());
            check(freezerLevel == 3, "a click took the Freezer from 4x to 8x: " + freezerLevel);
        }
    }

    private static int level(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer(server -> ((FarmBlockEntity) server.overworld().getBlockEntity(pos)).overclock());
    }

    /** A real left click on the button whose middle (in GUI pixels) this gives. */
    private static void click(ClientGameTestContext ctx, Function<net.minecraft.client.Minecraft, int[]> centre) {
        double[] at = ctx.computeOnClient(mc -> {
            int[] c = centre.apply(mc);
            double scale = mc.getWindow().getGuiScale();
            return new double[]{c[0] * scale, c[1] * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(4);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
