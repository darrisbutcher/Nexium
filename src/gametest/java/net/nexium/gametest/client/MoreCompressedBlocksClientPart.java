package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.nexium.essentials.compressed.CompressedBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The compressed blocks in a real game: every one stands in the world as the right block, and every one's item has its name and
 * its picture in the inventory (a missing texture or model shows up in the screenshots). Takes screenshots of the wall of blocks
 * and of the inventory.
 */
public class MoreCompressedBlocksClientPart {
    private static final CompressedBlocks.Base[] NEW = {CompressedBlocks.Base.CALCITE, CompressedBlocks.Base.DRIPSTONE_BLOCK, CompressedBlocks.Base.SMOOTH_BASALT,
            CompressedBlocks.Base.TERRACOTTA, CompressedBlocks.Base.SANDSTONE, CompressedBlocks.Base.RED_SANDSTONE, CompressedBlocks.Base.OBSIDIAN,
            CompressedBlocks.Base.MAGMA_BLOCK, CompressedBlocks.Base.MUD, CompressedBlocks.Base.CLAY, CompressedBlocks.Base.SOUL_SAND,
            CompressedBlocks.Base.SOUL_SOIL, CompressedBlocks.Base.SNOW_BLOCK};

    /** Runs in the world the group's test made (see the All* test); the window is resized and the world is made there. */
    void part(ClientGameTestContext ctx, TestSingleplayerContext world) {
        {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                base[0] = player.blockPosition();
                // A wall 13 wide and 3 high seven blocks ahead (south): a column for each new block, its levels going up
                for (int x = -8; x <= 8; x++) for (int z = 1; z <= 9; z++) for (int y = 0; y <= 4; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                for (int i = 0; i < NEW.length; i++) {
                    for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                        player.level().setBlockAndUpdate(base[0].offset(i - 6, level - 1, 7), CompressedBlocks.block(NEW[i], level).defaultBlockState());
                    }
                }
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 0.0F);
            });
            ctx.waitTicks(20);
            int wrong = ctx.computeOnClient(mc -> {
                int bad = 0;
                for (int i = 0; i < NEW.length; i++) {
                    for (int level = 1; level <= CompressedBlocks.LEVELS; level++) {
                        if (mc.level.getBlockState(base[0].offset(i - 6, level - 1, 7)).getBlock() != CompressedBlocks.block(NEW[i], level)) bad++;
                    }
                }
                return bad;
            });
            check(wrong == 0, "all 39 new blocks stand in the world as the right block on the client: " + wrong + " wrong");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] compressed blocks wall: " + ctx.takeScreenshot("nexus_compressed_new_wall"));

            // The items: 36 of them fill the inventory, the other three go in after
            List<ItemStack> all = new ArrayList<>();
            for (CompressedBlocks.Base b : NEW) for (int level = 1; level <= CompressedBlocks.LEVELS; level++) all.add(new ItemStack(CompressedBlocks.item(b, level)));
            for (int page = 0; page < 2; page++) {
                final int from = page * 36;
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    player.getInventory().clearContent();
                    for (int i = 0; i < 36 && from + i < all.size(); i++) player.getInventory().setItem(i, all.get(from + i).copy());
                });
                ctx.waitTicks(10);
                ctx.getInput().pressKey(options -> options.keyInventory);
                ctx.waitTicks(10);
                ctx.runOnClient(mc -> mc.gui.toastManager().clear());
                if (page == 0) {
                    String names = ctx.computeOnClient(mc -> mc.player.getInventory().getItem(0).getHoverName().getString() + " / "
                            + mc.player.getInventory().getItem(1).getHoverName().getString() + " / " + mc.player.getInventory().getItem(2).getHoverName().getString()
                            + " / " + mc.player.getInventory().getItem(33).getHoverName().getString());
                    check(names.equals("Compressed Calcite / Double Compressed Calcite / Triple Compressed Calcite / Compressed Soul Soil"),
                            "the names read right in the inventory: " + names);
                }
                System.out.println("[nexus] compressed blocks inventory " + page + ": " + ctx.takeScreenshot("nexus_compressed_new_inventory_" + page));
                ctx.setScreen(() -> null);
                ctx.waitTicks(5);
            }
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
