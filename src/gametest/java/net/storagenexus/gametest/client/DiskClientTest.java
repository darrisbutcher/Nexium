package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.client.screen.DiskDriveScreen;
import net.storagenexus.disk.DiskDriveBlock;
import net.storagenexus.disk.DiskDriveBlockEntity;
import net.storagenexus.disk.DiskTier;
import net.storagenexus.disk.StorageDiskItem;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModItems;

/**
 * A Disk Drive in a real game: six disks of different sizes and fill (green, yellow and red lights), a close look at
 * its front, its screen, the Move to disks hint, and a press of Move to disks that empties a chest on the network.
 */
public class DiskClientTest implements FabricClientGameTest {
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
                BlockPos drive = base.offset(0, 0, 2);
                at[0] = drive;
                var level = player.level();
                // Terminal on the left, drive in the middle, connector and chest on the right, all facing the player
                level.setBlockAndUpdate(drive.west(), ModBlocks.CRAFTING_TERMINAL.defaultBlockState()
                        .setValue(net.storagenexus.network.TerminalBlock.FACING, Direction.NORTH));
                level.setBlockAndUpdate(drive, ModBlocks.DISK_DRIVE.defaultBlockState().setValue(DiskDriveBlock.FACING, Direction.NORTH));
                level.setBlockAndUpdate(drive.east(2), Blocks.CHEST.defaultBlockState());
                // As a player would place it: with its arms worked out from what is around it
                level.setBlockAndUpdate(drive.east(), net.minecraft.world.level.block.Block.updateFromNeighbourShapes(
                        ModBlocks.STORAGE_CONNECTOR.defaultBlockState(), level, drive.east()));
                if (level.getBlockEntity(drive.east(2)) instanceof ChestBlockEntity chest) {
                    chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
                    chest.setItem(1, new ItemStack(Items.REDSTONE, 40));
                }
                DiskDriveBlockEntity be = (DiskDriveBlockEntity) level.getBlockEntity(drive);
                be.disks().setItem(0, filled(server, ModItems.STORAGE_DISKS.get(DiskTier.K16), Items.COBBLESTONE, 6_000));
                be.disks().setItem(1, filled(server, ModItems.STORAGE_DISKS.get(DiskTier.K64), Items.DIRT, 52_000));
                be.disks().setItem(2, filled(server, ModItems.STORAGE_DISKS.get(DiskTier.K256), Items.STONE, 30_000));
                be.disks().setItem(3, filled(server, ModItems.STORAGE_DISKS.get(DiskTier.M1), Items.SAND, 1_000_000));
                be.disks().setItem(4, filled(server, ModItems.STORAGE_DISKS.get(DiskTier.NEXIUM), Items.DIAMOND, 1_000));
                ItemStack water = new ItemStack(ModItems.FLUID_DISKS.get(DiskTier.K16));
                try (Transaction tx = Transaction.openOuter()) {
                    StorageDiskItem.fluids(server, water).insert(FluidVariant.of(Fluids.WATER), 20 * FluidConstants.BUCKET, tx);
                    tx.commit();
                }
                be.disks().setItem(6, water);
                be.disks().setChanged();
                player.snapTo(drive.getX() + 0.5, drive.getY(), drive.getZ() - 2.1, 0.0F, 25.0F);
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(25.0F);
            });
            ctx.getInput().pressKey(options -> options.keyToggleGui); // F1: hide the HUD for a clear look
            ctx.waitTicks(20);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] disk drive front: " + ctx.takeScreenshot("nexus_disk_drive_front"));
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            // Right-click it with an empty hand: its screen
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[8]);
            ctx.waitTicks(5);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof DiskDriveScreen), "right-click opens the drive's screen");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] disk drive screen: " + ctx.takeScreenshot("nexus_disk_drive_screen"));
            // The Move to disks button is the third widget: hover for its hint, then press it
            double[] move = ctx.computeOnClient(mc -> {
                var w = (AbstractWidget) mc.gui.screen().children().get(2);
                double s = mc.getWindow().getGuiScale();
                return new double[]{(w.getX() + w.getWidth() / 2.0) * s, (w.getY() + w.getHeight() / 2.0) * s};
            });
            ctx.getInput().setCursorPos(move[0], move[1]);
            ctx.waitTicks(3);
            System.out.println("[nexus] disk drive move hint: " + ctx.takeScreenshot("nexus_disk_drive_move_hint"));
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.waitTicks(15);
            boolean emptied = world.getServer().computeOnServer(server ->
                    server.overworld().getBlockEntity(at[0].east(2)) instanceof ChestBlockEntity chest && chest.isEmpty());
            check(emptied, "Move to disks empties the chest");
            System.out.println("[nexus] disk drive after move: " + ctx.takeScreenshot("nexus_disk_drive_moved"));
        }
    }

    private static ItemStack filled(MinecraftServer server, Item diskItem, Item content, long amount) {
        ItemStack disk = new ItemStack(diskItem);
        try (Transaction tx = Transaction.openOuter()) {
            StorageDiskItem.items(server, disk).insert(ItemVariant.of(content), amount, tx);
            tx.commit();
        }
        return disk;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
