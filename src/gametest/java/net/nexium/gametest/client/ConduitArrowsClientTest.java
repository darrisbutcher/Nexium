package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.SideConfig;
import net.nexium.registry.ModBlocks;

import java.util.function.Consumer;

/**
 * The direction arrows on a conduit's arm: an extracting side shows an arrow pointing in to the conduit, an inserting side one pointing
 * out to the block, and a side that does both shows the two. Sets up each case beside a barrel and takes a screenshot of it close up.
 */
public class ConduitArrowsClientTest implements FabricClientGameTest {
    private static final int ORANGE = 1, LIGHT_BLUE = 3, LIME = 5;

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
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().get(0);
                BlockPos c = player.blockPosition().offset(2, 0, -2);
                at[0] = c;
                level.setBlockAndUpdate(c.east(), Blocks.BARREL.defaultBlockState());
                level.setBlockAndUpdate(c, ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                ((ConduitBundleBlockEntity) level.getBlockEntity(c)).addConduit(ConduitType.ITEM, 1);
            });
            world.getServer().runCommand("tp @p " + (at[0].getX() + 1.0) + " " + at[0].getY() + " " + (at[0].getZ() + 1.8) + " 180 40");
            ctx.waitTicks(20);
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            shot(ctx, world, at[0], "insert", be -> {
                SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
                cfg.mode = SideConfig.Mode.INSERT;
                cfg.channel = ORANGE;
            });
            shot(ctx, world, at[0], "extract", be -> {
                SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
                cfg.mode = SideConfig.Mode.EXTRACT;
                cfg.setExtractChannel(ORANGE);
            });
            shot(ctx, world, at[0], "both", be -> {
                SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
                cfg.mode = SideConfig.Mode.BOTH;
                cfg.channel = LIME;
                cfg.setExtractChannel(ORANGE);
            });
            shot(ctx, world, at[0], "extract_three_colours", be -> {
                SideConfig cfg = be.config(ConduitType.ITEM, Direction.EAST);
                cfg.mode = SideConfig.Mode.EXTRACT;
                cfg.extractChannels = 1 << ORANGE | 1 << LIME | 1 << LIGHT_BLUE;
            });
            // A block that takes items and fluids, so one arm carries both: the item extract arrow and the fluid insert arrow
            shot(ctx, world, at[0], "item_and_fluid", be -> {
                be.getLevel().setBlockAndUpdate(at[0].east(), ModBlocks.DRAWER_CONTROLLER.defaultBlockState());
                be.addConduit(ConduitType.FLUID, 1);
                SideConfig item = be.config(ConduitType.ITEM, Direction.EAST);
                item.mode = SideConfig.Mode.EXTRACT;
                item.setExtractChannel(ORANGE);
                SideConfig fluid = be.config(ConduitType.FLUID, Direction.EAST);
                fluid.mode = SideConfig.Mode.INSERT;
                fluid.channel = LIGHT_BLUE;
            });

            // The other directions, on two more conduits with three barrels each, seen from the side: the arrows must point the same way
            // (out to the block for inserting, in to the conduit for extracting) whichever way the arm points
            scene(ctx, world, at[0].offset(0, 0, -6), new Direction[]{Direction.EAST, Direction.UP, Direction.SOUTH},
                    new SideConfig.Mode[]{SideConfig.Mode.BOTH, SideConfig.Mode.EXTRACT, SideConfig.Mode.INSERT}, -1.6, -2.0, -39, "positive_sides");
            scene(ctx, world, at[0].offset(0, 0, -12), new Direction[]{Direction.WEST, Direction.DOWN, Direction.NORTH},
                    new SideConfig.Mode[]{SideConfig.Mode.BOTH, SideConfig.Mode.EXTRACT, SideConfig.Mode.INSERT}, 1.6, 2.0, 141, "negative_sides");
            ctx.getInput().pressKey(options -> options.keyToggleGui);
        }
    }

    /** A conduit with a barrel on each of three sides, set to the modes given, seen from {@code dx, dz} away. */
    private static void scene(ClientGameTestContext ctx, TestSingleplayerContext world, BlockPos c, Direction[] sides, SideConfig.Mode[] modes,
                              double dx, double dz, int yaw, String name) {
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            level.setBlockAndUpdate(c, ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
            for (Direction d : sides) level.setBlockAndUpdate(c.relative(d), Blocks.BARREL.defaultBlockState());
            ((ConduitBundleBlockEntity) level.getBlockEntity(c)).addConduit(ConduitType.ITEM, 1);
        });
        world.getServer().runCommand("tp @p " + (c.getX() + 0.5 + dx) + " " + c.getY() + " " + (c.getZ() + 0.5 + dz) + " " + yaw + " 14");
        shot(ctx, world, c, name, be -> {
            for (int i = 0; i < sides.length; i++) set(be, sides[i], modes[i]);
        });
    }

    private static void set(ConduitBundleBlockEntity be, Direction d, SideConfig.Mode mode) {
        SideConfig cfg = be.config(ConduitType.ITEM, d);
        cfg.mode = mode;
        cfg.channel = LIME;
        cfg.setExtractChannel(ORANGE);
    }

    /** Sets the conduit's east side up, lets the markers refresh, and takes a screenshot. */
    private static void shot(ClientGameTestContext ctx, TestSingleplayerContext world, BlockPos at, String name, Consumer<ConduitBundleBlockEntity> setup) {
        world.getServer().runOnServer(server -> {
            var be = (ConduitBundleBlockEntity) server.overworld().getBlockEntity(at);
            setup.accept(be);
            be.changed();
        });
        ctx.waitTicks(25);
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        System.out.println("[nexus] conduit arrows " + name + ": " + ctx.takeScreenshot("nexus_arrows_" + name));
    }
}
