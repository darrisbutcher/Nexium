package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.storagenexus.essentials.client.FarmScreen;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.registry.ModBlocks;

/**
 * The ten newer farms in a real game: their faces in a row, the Gourd and Cactus Farm's screen (no mode button), and the Bee Farm's screen.
 */
public class MoreFarmsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                BlockPos base = player.blockPosition();
                at[0] = base;
                var level = player.level();
                FarmKind[] kinds = {FarmKind.MONSTER, FarmKind.NETHER_MOB, FarmKind.OCEAN, FarmKind.SLIME, FarmKind.LIVESTOCK,
                        FarmKind.CROP, FarmKind.GOURD, FarmKind.GENERATOR, FarmKind.BEE, FarmKind.MOSS};
                for (int i = 0; i < kinds.length; i++) {
                    level.setBlockAndUpdate(base.offset((i % 5) * 2 - 4, 0, 4 + (i / 5) * 3), ModBlocks.FARMS.get(kinds[i]).defaultBlockState());
                }
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 2.5, 0.0F, 22.0F);
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(22.0F);
            });
            ctx.getInput().pressKey(options -> options.keyToggleGui);
            ctx.waitTicks(20);
            System.out.println("[nexus] ten farms: " + ctx.takeScreenshot("nexus_ten_farms"));
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            // The Gourd and Cactus Farm: its screen, with no mode button
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.snapTo(at[0].getX() - 1.5, at[0].getY(), at[0].getZ() + 5.5, 0.0F, 22.0F); // close enough to use it
                player.openMenu((FarmBlockEntity) player.level().getBlockEntity(at[0].offset(-2, 0, 7)));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmScreen), "a farm's screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            check(ctx.computeOnClient(mc -> mc.gui.screen().children().stream().noneMatch(c -> c instanceof AbstractWidget)), "no mode button");
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(2);
            System.out.println("[nexus] gourd farm screen: " + ctx.takeScreenshot("nexus_gourd_farm_screen"));
            ctx.runOnClient(mc -> mc.player.closeContainer());
            ctx.waitTicks(5);

            // The Bee Farm's screen
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.openMenu((FarmBlockEntity) player.level().getBlockEntity(at[0].offset(2, 0, 7)));
            });
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(2);
            System.out.println("[nexus] bee farm screen: " + ctx.takeScreenshot("nexus_bee_farm_screen"));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
