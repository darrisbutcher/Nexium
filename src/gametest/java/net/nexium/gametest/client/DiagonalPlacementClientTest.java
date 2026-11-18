package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.client.DiagonalPreview;
import net.nexium.essentials.client.NexusEssentialsClient;

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
                // and a second column to the west, for the straight-across placement
                for (int y = 0; y <= 2; y++) player.level().setBlockAndUpdate(base[0].offset(-2, y, 4), Blocks.STONE.defaultBlockState());
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 0.0F);
                player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 10));
                player.getInventory().setSelectedSlot(0);
            });
            ctx.waitTicks(15);

            // The crosshair is on the north face of the middle stone block, near the top of it (so the top edge decides)
            BlockPos aimed = base[0].offset(0, 1, 4);
            ctx.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(aimed.getX() + 0.5, aimed.getY() + 0.9, aimed.getZ())));
            ctx.waitTicks(3);
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
            check(ctx.computeOnClient(mc -> DiagonalPreview.edge() == Direction.UP && DiagonalPreview.face() == Direction.NORTH), "and the top edge of the aimed north face is the one marked");
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

            // Aimed at the middle of a face instead: straight across from it with a one block gap
            BlockPos west = base[0].offset(-2, 1, 4);
            ctx.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(west.getX() + 0.5, west.getY() + 0.5, west.getZ())));
            ctx.waitTicks(4);
            String west_looking = ctx.computeOnClient(mc -> mc.hitResult instanceof BlockHitResult b ? b.getBlockPos().toShortString() + " " + b.getDirection() : "nothing");
            check(west_looking.equals(west.toShortString() + " " + Direction.NORTH), "the crosshair is on the middle of the north face of the west stone block: " + west_looking);
            ctx.getInput().holdKey(NexusEssentialsClient.DIAGONAL_PLACE);
            ctx.waitTicks(3);
            BlockPos across = west.north(2);
            String gap = ctx.computeOnClient(mc -> DiagonalPreview.target() + " " + DiagonalPreview.placeable() + " " + DiagonalPreview.edge() + " " + DiagonalPreview.face());
            check(gap.equals(across + " true null " + Direction.NORTH), "aimed at the middle: the outline is two out, straight across, with no edge and the face marked (" + across + " true null NORTH): " + gap);
            System.out.println("[nexus] gap placement preview: " + ctx.takeScreenshot("nexus_gap_preview"));
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(12);
            String gapResult = world.getServer().computeOnServer(server -> {
                var level = server.getPlayerList().getPlayers().get(0).level();
                return level.getBlockState(across).is(Blocks.COBBLESTONE) + " " + level.getBlockState(west.north()).isAir() + " "
                        + server.getPlayerList().getPlayers().get(0).getInventory().getItem(0).getCount();
            });
            check(gapResult.equals("true true 7"), "held: the block is two out with the cell between empty, and one was used (placed, gap empty, count): " + gapResult);
            System.out.println("[nexus] gap placement: " + ctx.takeScreenshot("nexus_gap_placement"));
            ctx.getInput().releaseKey(NexusEssentialsClient.DIAGONAL_PLACE);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
