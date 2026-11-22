package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.nexium.client.screen.ConduitConfigScreen;
import net.nexium.client.screen.DiskDriveScreen;
import net.nexium.client.screen.FilterScreen;
import net.nexium.client.screen.NetworkBusScreen;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitConfigMenu;
import net.nexium.conduit.ConduitType;
import net.nexium.disk.DiskDriveBlockEntity;
import net.nexium.filter.FilterData;
import net.nexium.network.NetworkBusBlock;
import net.nexium.network.NetworkBusBlockEntity;
import net.nexium.network.NetworkBusMenu;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.util.Menus;

import java.util.List;

/**
 * The Edit button beside a filter slot: on the Import/Export Bus, the Disk Drive and a conduit side, a real click on it opens the filter
 * that sits in the slot (it never leaves the slot), a click on one of its settings changes it, and Escape comes back to the screen it was
 * opened from.
 */
public class FilterEditInBlockClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[3];
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                var level = server.overworld();
                BlockPos base = player.blockPosition();
                at[0] = base.offset(3, 2, 3);
                at[1] = base.offset(6, 2, 3);
                at[2] = base.offset(9, 2, 3);
                level.setBlockAndUpdate(at[0].west(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(at[0], ModBlocks.NETWORK_BUS.defaultBlockState().setValue(NetworkBusBlock.FACING, Direction.WEST));
                ((NetworkBusBlockEntity) level.getBlockEntity(at[0])).filterContainer().setItem(0, filter(true));
                level.setBlockAndUpdate(at[1], ModBlocks.DISK_DRIVE.defaultBlockState());
                ((DiskDriveBlockEntity) level.getBlockEntity(at[1])).filterContainer().setItem(0, filter(false));
                level.setBlockAndUpdate(at[2].east(), Blocks.BARREL.defaultBlockState());
                level.setBlockAndUpdate(at[2], ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                var conduit = (ConduitBundleBlockEntity) level.getBlockEntity(at[2]);
                conduit.addConduit(ConduitType.ITEM, 1);
                conduit.config(ConduitType.ITEM, Direction.EAST).filter = filter(true);
                conduit.changed();
            });

            // Import/Export Bus
            world.getServer().runOnServer(server -> {
                var bus = (NetworkBusBlockEntity) server.overworld().getBlockEntity(at[0]);
                Menus.open(server.getPlayerList().getPlayers().get(0), Component.literal("Import/Export Bus"), at[0],
                        (id, inv, p) -> new NetworkBusMenu(id, inv, bus));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof NetworkBusScreen);
            ctx.waitTicks(10);
            roundTrip(ctx, world, NetworkBusScreen.class, "bus", () -> filterIn(world, at[0], 0));

            // Disk Drive
            world.getServer().runOnServer(server -> {
                var drive = (DiskDriveBlockEntity) server.overworld().getBlockEntity(at[1]);
                Menus.open(server.getPlayerList().getPlayers().get(0), Component.literal("Disk Drive"), at[1],
                        (id, inv, p) -> new net.nexium.disk.DiskDriveMenu(id, inv, drive));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof DiskDriveScreen);
            ctx.waitTicks(10);
            roundTrip(ctx, world, DiskDriveScreen.class, "disk_drive", () -> filterIn(world, at[1], 1));

            // Conduit side
            world.getServer().runOnServer(server -> {
                var conduit = (ConduitBundleBlockEntity) server.overworld().getBlockEntity(at[2]);
                Menus.open(server.getPlayerList().getPlayers().get(0), Component.literal("Conduit"),
                        new ConduitConfigMenu.OpenData(at[2], Direction.EAST.get3DDataValue()),
                        (id, inv, p) -> new ConduitConfigMenu(id, inv, conduit, Direction.EAST.get3DDataValue()));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof ConduitConfigScreen);
            ctx.waitTicks(10);
            roundTrip(ctx, world, ConduitConfigScreen.class, "conduit", () -> filterIn(world, at[2], 2));
        }
    }

    private interface Reader { FilterData read(); }

    /** Clicks Edit on the screen that is open, checks the editor and its change, then Escape and the way back. */
    private static void roundTrip(ClientGameTestContext ctx, TestSingleplayerContext world, Class<? extends Screen> parent, String name, Reader read) {
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        ctx.getInput().setCursorPos(0, 0);
        ctx.waitTicks(3);
        System.out.println("[nexus] " + name + " screen with Edit: " + ctx.takeScreenshot("nexus_filter_edit_" + name + "_screen"));
        double[] edit = ctx.computeOnClient(mc -> {
            for (var child : mc.gui.screen().children()) {
                if (child instanceof Button b && b.getMessage().equals(Component.translatable("screen.nexium.edit_filter"))) {
                    check(b.active, "the Edit button is usable with a filter in the slot");
                    double s = mc.getWindow().getGuiScale();
                    return new double[]{(b.getX() + b.getWidth() / 2.0) * s, (b.getY() + b.getHeight() / 2.0) * s};
                }
            }
            throw new AssertionError("no Edit button on the " + name + " screen");
        });
        ctx.getInput().setCursorPos(edit[0], edit[1]);
        ctx.waitTicks(3);
        ctx.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);
        ctx.waitFor(mc -> mc.gui.screen() instanceof FilterScreen);
        ctx.waitTicks(10);
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        System.out.println("[nexus] " + name + " filter editor: " + ctx.takeScreenshot("nexus_filter_edit_" + name + "_editor"));

        // The whitelist/blacklist button, with a real click, changes the filter in the block's slot
        boolean before = read.read().whitelist();
        double[] white = ctx.computeOnClient(mc -> {
            for (var child : mc.gui.screen().children()) {
                if (child instanceof Button b) {
                    double s = mc.getWindow().getGuiScale();
                    return new double[]{(b.getX() + b.getWidth() / 2.0) * s, (b.getY() + b.getHeight() / 2.0) * s};
                }
            }
            throw new AssertionError("no button on the editor");
        });
        ctx.getInput().setCursorPos(white[0], white[1]);
        ctx.waitTicks(3);
        ctx.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);
        ctx.waitTicks(8);
        check(read.read().whitelist() != before, "the editor's first button changed the filter that sits in the " + name + " slot");
        check(!read.read().toList().get(0).isEmpty(), "the filter's items are untouched");

        // Escape comes back to the screen it was opened from, with the filter still in the slot
        ctx.getInput().pressKey(options -> options.keyInventory);
        ctx.waitFor(mc -> parent.isInstance(mc.gui.screen()));
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> parent.isInstance(mc.gui.screen())), "closing the editor returns to the " + name + " screen");
        check(read.read().toList().get(0).is(Items.STONE), "and the filter is still in its slot");
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        ctx.getInput().setCursorPos(0, 0);
        ctx.waitTicks(3);
        System.out.println("[nexus] " + name + " screen after: " + ctx.takeScreenshot("nexus_filter_edit_" + name + "_back"));
        ctx.runOnClient(mc -> mc.gui.setScreen(null));
        ctx.waitTicks(5);
    }

    private static FilterData filterIn(TestSingleplayerContext world, BlockPos pos, int kind) {
        return world.getServer().computeOnServer(server -> {
            var be = server.overworld().getBlockEntity(pos);
            ItemStack s = be instanceof NetworkBusBlockEntity bus ? bus.filterContainer().getItem(0)
                    : be instanceof DiskDriveBlockEntity d ? d.filterContainer().getItem(0)
                    : ((ConduitBundleBlockEntity) be).config(ConduitType.ITEM, Direction.EAST).filter;
            return s.getOrDefault(ModComponents.FILTER, FilterData.empty(false));
        });
    }

    private static ItemStack filter(boolean advanced) {
        ItemStack stack = new ItemStack(advanced ? ModItems.ADVANCED_FILTER : ModItems.BASIC_FILTER);
        stack.set(ModComponents.FILTER, FilterData.empty(advanced).withEntries(List.of(new ItemStack(Items.STONE))));
        return stack;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
