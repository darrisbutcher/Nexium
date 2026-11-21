package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.nexium.client.screen.ConduitConfigScreen;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitConfigMenu;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.SideConfig;
import net.nexium.registry.ModBlocks;
import net.nexium.util.Menus;

/**
 * The conduit settings screen with several extract colours: under Out there is a grid of the 16 colours, a click switches a colour on
 * or off (the last one left on stays), the server keeps the set, and a side that never extracts ignores the clicks. Takes a screenshot.
 */
public class ConduitExtractColoursClientTest implements FabricClientGameTest {
    private static final int ORANGE = 1, LIGHT_BLUE = 3, LIME = 5;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().get(0);
                BlockPos c = player.blockPosition().offset(2, 0, -2);
                at[0] = c;
                level.setBlockAndUpdate(c.east(), Blocks.BARREL.defaultBlockState());
                level.setBlockAndUpdate(c, ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                var be = (ConduitBundleBlockEntity) level.getBlockEntity(c);
                be.addConduit(ConduitType.ITEM, 1);
                SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
                cfg.mode = SideConfig.Mode.EXTRACT;
                cfg.setExtractChannel(ORANGE);
                be.changed();
                Menus.open(player, Component.literal("Conduit"), new ConduitConfigMenu.OpenData(c, Direction.EAST.get3DDataValue()),
                        (id, inv, p) -> new ConduitConfigMenu(id, inv, be, Direction.EAST.get3DDataValue()));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof ConduitConfigScreen);
            ctx.waitTicks(10);
            check(colours(ctx) == 1 << ORANGE, "the screen starts with the side's one extract colour on: " + colours(ctx));

            // Two more colours, by clicking their squares
            click(ctx, LIME);
            click(ctx, LIGHT_BLUE);
            check(colours(ctx) == (1 << ORANGE | 1 << LIME | 1 << LIGHT_BLUE), "clicking two more switches them on: " + colours(ctx));
            check(serverColours(world, at[0]) == (1 << ORANGE | 1 << LIME | 1 << LIGHT_BLUE), "and the server holds the same set: " + serverColours(world, at[0]));
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] conduit extract colours: " + ctx.takeScreenshot("nexus_conduit_extract_colours"));

            // A click on a colour that is on switches it off; the last one left is never switched off
            click(ctx, ORANGE);
            click(ctx, LIME);
            check(colours(ctx) == 1 << LIGHT_BLUE, "two switched off, one left: " + colours(ctx));
            click(ctx, LIGHT_BLUE);
            check(colours(ctx) == 1 << LIGHT_BLUE, "the last colour stays on: " + colours(ctx));
            check(serverColours(world, at[0]) == 1 << LIGHT_BLUE, "on the server too: " + serverColours(world, at[0]));

            // A side that only inserts has no extract colours to pick: clicks change nothing
            world.getServer().runOnServer(server -> {
                var be = (ConduitBundleBlockEntity) server.overworld().getBlockEntity(at[0]);
                be.config(ConduitType.ITEM, Direction.EAST).mode = SideConfig.Mode.INSERT;
                be.changed();
            });
            ctx.waitTicks(5);
            click(ctx, LIME);
            check(colours(ctx) == 1 << LIGHT_BLUE && serverColours(world, at[0]) == 1 << LIGHT_BLUE, "an insert-only side ignores the grid");
            System.out.println("[nexus] conduit extract colours (insert only): " + ctx.takeScreenshot("nexus_conduit_extract_colours_insert_only"));
            ctx.runOnClient(mc -> mc.gui.setScreen(null));

            // In the world: the extract side's ring is drawn once for each colour that is on, stacked along the arm
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runOnServer(server -> {
                var be = (ConduitBundleBlockEntity) server.overworld().getBlockEntity(at[0]);
                SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
                cfg.mode = SideConfig.Mode.EXTRACT;
                cfg.extractChannels = 1 << ORANGE | 1 << LIME | 1 << LIGHT_BLUE;
                be.changed();
            });
            world.getServer().runCommand("tp @p " + (at[0].getX() + 1.0) + " " + at[0].getY() + " " + (at[0].getZ() + 1.8) + " 180 40");
            ctx.waitTicks(30);
            ctx.getInput().pressKey(options -> options.keyToggleGui);
            ctx.waitTicks(10);
            System.out.println("[nexus] conduit extract rings: " + ctx.takeScreenshot("nexus_conduit_extract_rings"));
            ctx.getInput().pressKey(options -> options.keyToggleGui);
        }
    }

    private static int colours(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> ((ConduitConfigScreen) mc.gui.screen()).extractColours());
    }

    private static int serverColours(TestSingleplayerContext world, BlockPos at) {
        return world.getServer().computeOnServer(server -> ((ConduitBundleBlockEntity) server.overworld().getBlockEntity(at))
                .config(ConduitType.ITEM, Direction.EAST).extractChannels);
    }

    /** A real left click (cursor moved there, left button pressed) in the middle of that colour's square. */
    private static void click(ClientGameTestContext ctx, int channel) {
        RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((ConduitConfigScreen) mc.gui.screen()).extractCellCentre(channel)));
        ctx.waitTicks(2);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
