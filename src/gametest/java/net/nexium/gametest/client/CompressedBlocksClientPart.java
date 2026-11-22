package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.nexium.essentials.compressed.CompressedBlocks;

/**
 * The compressed blocks as the game draws them: two walls, one for the eight stone-family blocks and one for the other eight,
 * each column the plain block with its Compressed, Double Compressed and Triple Compressed versions above it. A screenshot of each.
 */
public class CompressedBlocksClientPart {
    /** Runs in the world the group's test made (see the All* test); the window is resized and the world is made there. */
    void part(ClientGameTestContext ctx, TestSingleplayerContext world) {
        {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("weather clear");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                base[0] = player.blockPosition();
                // Level the ground and the sky in front of the player, so nothing but the wall stands there
                for (int x = -7; x <= 7; x++) for (int z = 0; z <= 8; z++) for (int y = 0; y <= 6; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, -5.0F);
            });
            ctx.waitTicks(10);
            CompressedBlocks.Base[] all = CompressedBlocks.Base.values();
            for (int wall = 0; wall < 2; wall++) {
                final int first = wall * 8;
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    for (int column = 0; column < 8; column++) {
                        CompressedBlocks.Base kind = all[first + column];
                        BlockPos at = base[0].offset(column - 4, 0, 6);
                        player.level().setBlockAndUpdate(at, kind.block().defaultBlockState());
                        for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                            player.level().setBlockAndUpdate(at.above(level), CompressedBlocks.block(kind, level).defaultBlockState());
                        }
                    }
                });
                ctx.waitTicks(30);
                System.out.println("[nexus] compressed wall " + (wall + 1) + ": " + ctx.takeScreenshot("nexus_compressed_wall_" + (wall + 1)));
            }
        }
    }
}
