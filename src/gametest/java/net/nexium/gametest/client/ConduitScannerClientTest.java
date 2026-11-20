package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.client.ScannerOutline;
import net.nexium.client.screen.ConduitScannerScreen;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitScan;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.SideConfig;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;

import java.util.List;

/**
 * The Conduit Scanner in a real game: right-clicking a conduit with it opens the window for that network, with a tab for each conduit
 * type the conduit carries, connections grouped under their channel colours (or in a list), warnings, and a click that outlines a
 * connection in the world. Takes screenshots of each.
 */
public class ConduitScannerClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                var level = player.level();
                base[0] = player.blockPosition();
                for (int x = -6; x <= 8; x++) for (int z = -2; z <= 9; z++) {
                    level.setBlockAndUpdate(base[0].offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                // Three conduits in a row, carrying items and energy, with chests and barrels around them
                BlockPos a = base[0].offset(0, 0, 4), b = a.east(), c = b.east();
                for (BlockPos p : List.of(a, b, c)) {
                    level.setBlockAndUpdate(p, ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                    ConduitBundleBlockEntity be = (ConduitBundleBlockEntity) level.getBlockEntity(p);
                    be.addConduit(ConduitType.ITEM, 1);
                    be.addConduit(ConduitType.ENERGY, 1);
                }
                level.setBlockAndUpdate(a.west(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(c.east(), Blocks.BARREL.defaultBlockState());
                level.setBlockAndUpdate(a.south(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(b.south(), Blocks.BARREL.defaultBlockState());
                level.setBlockAndUpdate(c.south(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(a.above(), ModBlocks.IRON_BATTERY.defaultBlockState());
                level.setBlockAndUpdate(c.above(), ModBlocks.SOLAR_PANEL.defaultBlockState());
                ConduitBundleBlockEntity ba = (ConduitBundleBlockEntity) level.getBlockEntity(a);
                ConduitBundleBlockEntity bb = (ConduitBundleBlockEntity) level.getBlockEntity(b);
                ConduitBundleBlockEntity bc = (ConduitBundleBlockEntity) level.getBlockEntity(c);
                // Red channel (14): the west chest is taken from, the east barrel is filled first
                SideConfig w = ba.config(ConduitType.ITEM, Direction.WEST);
                w.mode = SideConfig.Mode.EXTRACT;
                w.extractChannel = 14;
                SideConfig e = bc.config(ConduitType.ITEM, Direction.EAST);
                e.mode = SideConfig.Mode.INSERT;
                e.channel = 14;
                e.priority = 5;
                // Lime (5): a chest that something is meant to fill, but nothing extracts on lime
                SideConfig sa = ba.config(ConduitType.ITEM, Direction.SOUTH);
                sa.mode = SideConfig.Mode.INSERT;
                sa.channel = 5;
                // Cyan in, red out: a barrel that both takes red and gives cyan
                SideConfig sb = bb.config(ConduitType.ITEM, Direction.SOUTH);
                sb.mode = SideConfig.Mode.BOTH;
                sb.channel = 9;
                sb.extractChannel = 14;
                // Orange (1): extracts, but only with a redstone signal (there is none), and nothing inserts on orange
                SideConfig sc = bc.config(ConduitType.ITEM, Direction.SOUTH);
                sc.mode = SideConfig.Mode.EXTRACT;
                sc.extractChannel = 1;
                sc.redstone = SideConfig.RedstoneControl.WITH_SIGNAL;
                for (ConduitBundleBlockEntity be : List.of(ba, bb, bc)) be.changed();
                player.getInventory().clearContent();
                player.getInventory().add(new ItemStack(ModItems.CONDUIT_SCANNER));
                player.snapTo(base[0].getX() + 1.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 25.0F);
            });
            ctx.waitTicks(10);
            ctx.getInput().pressKey(options -> options.keyToggleGui);   // hidden HUD for the world shots
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            // A smaller GUI scale than the 720p window's default, so the whole window shows
            ctx.runOnClient(mc -> {
                mc.options.guiScale().set(2);
                mc.resizeGui();
            });
            ctx.waitTicks(3);

            // Right-click the middle conduit with the Scanner, as a player does
            InteractionResult[] used = new InteractionResult[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONDUIT_SCANNER));
                BlockPos middle = base[0].offset(1, 0, 4);
                used[0] = player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(middle), Direction.UP, middle, false));
            });
            ctx.waitTicks(6);
            check(used[0].consumesAction(), "using the Scanner on a conduit is accepted");
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof ConduitScannerScreen), "and opens the Scanner window");
            ConduitScan.Report report = ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).report());
            check(report.type() == ConduitType.ITEM && report.carries(ConduitType.ENERGY), "it opens on the item network, with an energy tab too");
            check(report.links().size() == 5 && report.conduits() == 3, "the item network has 3 conduits and 5 connections: " + report.links().size());
            check(report.links().stream().filter(l -> l.flags() != 0).count() == 3, "three of them have warnings");

            // Grouped: a heading per channel, red has the extractor, and the Both barrel shows under red (out) and cyan (in)
            List<ConduitScannerScreen.Row> rows = ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).rows());
            check(ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).grouped()), "it opens grouped by channel");
            check(rows.stream().filter(ConduitScannerScreen.Row::header).count() == 4, "four channels have a heading: red, lime, cyan and orange");
            check(rows.stream().filter(r -> !r.header()).count() == 6, "six rows: the Both barrel appears under two channels");
            ctx.getInput().setCursorPos(640 * 2 / 2.0, 330);
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] scanner grouped: " + ctx.takeScreenshot("nexus_scanner_grouped"));

            // A tooltip on a row with a warning
            int[] point = ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).rowPoint(1));
            double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
            ctx.getInput().setCursorPos(point[0] * scale, point[1] * scale);
            ctx.waitTicks(3);
            System.out.println("[nexus] scanner tooltip: " + ctx.takeScreenshot("nexus_scanner_tooltip"));

            // The list layout
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).toggleLayout());
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> !((ConduitScannerScreen) mc.gui.screen()).grouped()), "the button switches to the list");
            check(ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).rows().size()) == 5, "one row for each connection");
            ctx.getInput().setCursorPos(10, 10);
            ctx.waitTicks(2);
            System.out.println("[nexus] scanner list: " + ctx.takeScreenshot("nexus_scanner_list"));
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).toggleLayout());

            // The energy tab: no channels, the battery and the solar panel
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).selectTab(ConduitType.ENERGY));
            ctx.waitTicks(30);
            check(ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).shownType()) == ConduitType.ENERGY, "the energy tab opens");
            check(ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).report().links().size()) == 2, "with the battery and the solar panel");
            System.out.println("[nexus] scanner energy: " + ctx.takeScreenshot("nexus_scanner_energy"));
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).selectTab(ConduitType.ITEM));
            ctx.waitTicks(30);

            // Clicking a row outlines the connection in the world; the window can be closed and the outline stays
            List<ConduitScannerScreen.Row> now = ctx.computeOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).rows());
            int first = -1, second = -1;
            for (int i = 0; i < now.size(); i++) {
                if (now.get(i).header()) continue;
                if (first < 0) first = i;
                else if (second < 0 && !now.get(i).link().target().equals(now.get(first).link().target())) second = i;
            }
            int firstRow = first, secondRow = second;
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).clickRow(firstRow));
            check(ScannerOutline.outlined().size() == 2, "clicking a row outlines its block and its conduit: " + ScannerOutline.outlined());
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).clickRow(secondRow));
            java.util.Set<BlockPos> both = new java.util.HashSet<>(List.of(now.get(firstRow).link().conduit(), now.get(firstRow).link().target(),
                    now.get(secondRow).link().conduit(), now.get(secondRow).link().target()));
            check(ScannerOutline.outlined().size() == both.size(), "a second connection adds its block and its conduit: " + ScannerOutline.outlined());
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).clickRow(secondRow));
            check(ScannerOutline.outlined().size() == 2, "clicking it again takes its block and conduit away and leaves the first connection: " + ScannerOutline.outlined());
            ctx.runOnClient(mc -> ((ConduitScannerScreen) mc.gui.screen()).clickRow(secondRow));
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
            ctx.waitTicks(5);
            check(ScannerOutline.outlined().size() >= 3, "the outlines stay after the window closes: " + ScannerOutline.outlined());
            world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 0 25", base[0].getX() + 1.5, base[0].getY() + 0.0, base[0].getZ() + 0.5));
            ctx.waitTicks(10);
            System.out.println("[nexus] scanner outlines: " + ctx.takeScreenshot("nexus_scanner_outlines"));
            ScannerOutline.clear();
            check(ScannerOutline.outlined().isEmpty(), "Clear outlines takes them away");
            ctx.runOnClient(mc -> {
                mc.options.guiScale().set(0);
                mc.resizeGui();
            });
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
