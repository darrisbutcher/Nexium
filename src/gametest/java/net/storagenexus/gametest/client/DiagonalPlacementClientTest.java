package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.storagenexus.client.DiagonalPreview;
import net.storagenexus.essentials.client.NexusEssentialsClient;

import java.util.Arrays;

/**
 * Diagonal placement with the real key: the key is on the Key Binds list (Left Alt by default), and with it held a right-click on
 * a wall places the block diagonally up and out of the spot aimed at, while without it the same click places it the normal way.
 * The outline of the target cell shows only while the key is held, white where the block would go and red where the cell is taken.
 */
public class DiagonalPlacementClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            check(Arrays.asList(mc.options.keyMappings).contains(NexusEssentialsClient.DIAGONAL_PLACE), "the diagonal key is in the game's key list");
            check(NexusEssentialsClient.DIAGONAL_PLACE.getDefaultKey().getValue() == InputConstants.KEY_LALT, "it defaults to Left Alt");
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                base[0] = player.blockPosition();
                // Open ground in front, with a column of stone three high four blocks ahead (south)
                for (int x = -2; x <= 2; x++) for (int z = 1; z <= 8; z++) for (int y = 0; y <= 5; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                for (int y = 0; y <= 2; y++) player.level().setBlockAndUpdate(base[0].offset(0, y, 4), Blocks.STONE.defaultBlockState());
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 0.0F);
                player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 10));
                player.getInventory().setSelectedSlot(0);
            });
            ctx.waitTicks(15);

            // The crosshair is on the north face of the middle stone block, level with the eyes (the upper half of it)
            BlockPos aimed = base[0].offset(0, 1, 4);
            String looking = ctx.computeOnClient(mc -> mc.hitResult instanceof BlockHitResult b ? b.getBlockPos().toShortString() + " " + b.getDirection() : "nothing");
            check(looking.equals(aimed.toShortString() + " " + Direction.NORTH), "the crosshair is on the north face of the middle stone block: " + looking);

            // With the key held: one out (north) and one up from the block aimed at
            BlockPos diagonal = aimed.north().above();
            BlockPos normal = aimed.north();
            ctx.waitTicks(2);
            check(ctx.computeOnClient(mc -> DiagonalPreview.target() == null), "no outline while the key is up");
            System.out.println("[nexus] no outline: " + ctx.takeScreenshot("nexus_diagonal_preview_off"));
            ctx.getInput().holdKey(NexusEssentialsClient.DIAGONAL_PLACE);
            ctx.waitTicks(3);
            String outlined = ctx.computeOnClient(mc -> DiagonalPreview.target() + " " + DiagonalPreview.placeable());
            check(outlined.equals(diagonal + " true"), "key held: the outline is on the diagonal cell and it is free (" + diagonal + " true): " + outlined);
            System.out.println("[nexus] diagonal preview: " + ctx.takeScreenshot("nexus_diagonal_preview_free"));
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(12);
            // The cell now holds the block just placed, so the outline turns red
            String taken = ctx.computeOnClient(mc -> DiagonalPreview.target() + " " + DiagonalPreview.placeable());
            check(taken.equals(diagonal + " false"), "once placed the same cell is outlined as taken (" + diagonal + " false): " + taken);
            System.out.println("[nexus] diagonal preview taken: " + ctx.takeScreenshot("nexus_diagonal_preview_taken"));
            ctx.getInput().releaseKey(NexusEssentialsClient.DIAGONAL_PLACE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> DiagonalPreview.target() == null), "released: the outline is gone");
            String result = world.getServer().computeOnServer(server -> {
                var level = server.getPlayerList().getPlayers().get(0).level();
                return level.getBlockState(diagonal).is(Blocks.COBBLESTONE) + " " + level.getBlockState(normal).isAir() + " "
                        + server.getPlayerList().getPlayers().get(0).getInventory().getItem(0).getCount();
            });
            check(result.equals("true true 9"), "held: the block is diagonally up and out, the normal cell is empty, and one was used (diagonal, normal cell empty, count): " + result);
            System.out.println("[nexus] diagonal placement: " + ctx.takeScreenshot("nexus_diagonal_placement"));

            // Key released: the same click puts it in the normal cell
            ctx.waitTicks(6);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(12);
            String normalResult = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().get(0).level().getBlockState(normal).is(Blocks.COBBLESTONE) + " "
                            + server.getPlayerList().getPlayers().get(0).getInventory().getItem(0).getCount());
            check(normalResult.equals("true 8"), "released: the block goes in the normal cell (placed, count): " + normalResult);
            System.out.println("[nexus] normal placement after: " + ctx.takeScreenshot("nexus_diagonal_then_normal"));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
