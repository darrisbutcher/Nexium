package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.essentials.sponge.BigSponges;

/**
 * The combined sponges in a real game: all six stand in the world as the right block (dry above, wet below, one column for each size)
 * and their items show their names and pictures in the inventory. Takes screenshots of the wall and the inventory.
 */
public class BigSpongesClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                base[0] = player.blockPosition();
                for (int x = -4; x <= 4; x++) for (int z = 1; z <= 8; z++) for (int y = 0; y <= 4; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                // Three columns, one for each size: the wet one at the bottom, the dry one on top. (No water near, so the dry ones stay dry.)
                for (BigSponges.Size size : BigSponges.Size.values()) {
                    int x = (size.ordinal() - 1) * 2;
                    player.level().setBlockAndUpdate(base[0].offset(x, 0, 5), BigSponges.wetBlock(size).defaultBlockState());
                    player.level().setBlockAndUpdate(base[0].offset(x, 1, 5), BigSponges.block(size).defaultBlockState());
                }
                for (BigSponges.Size size : BigSponges.Size.values()) {
                    player.getInventory().setItem(size.ordinal() * 2, new ItemStack(BigSponges.item(size)));
                    player.getInventory().setItem(size.ordinal() * 2 + 1, new ItemStack(BigSponges.wetItem(size)));
                }
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 20.0F);
            });
            ctx.waitTicks(20);
            boolean right = ctx.computeOnClient(mc -> {
                for (BigSponges.Size size : BigSponges.Size.values()) {
                    int x = (size.ordinal() - 1) * 2;
                    if (mc.level.getBlockState(base[0].offset(x, 0, 5)).getBlock() != BigSponges.wetBlock(size)) return false;
                    if (mc.level.getBlockState(base[0].offset(x, 1, 5)).getBlock() != BigSponges.block(size)) return false;
                }
                return true;
            });
            check(right, "all six sponges stand in the world as the right block on the client");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] sponges wall: " + ctx.takeScreenshot("nexus_sponges_wall"));

            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            String names = ctx.computeOnClient(mc -> mc.player.getInventory().getItem(0).getHoverName().getString() + " / "
                    + mc.player.getInventory().getItem(1).getHoverName().getString() + " / " + mc.player.getInventory().getItem(5).getHoverName().getString());
            check(names.equals("Large Sponge / Wet Large Sponge / Wet Giant Sponge"), "the names read right in the inventory: " + names);
            System.out.println("[nexus] sponges inventory: " + ctx.takeScreenshot("nexus_sponges_inventory"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
