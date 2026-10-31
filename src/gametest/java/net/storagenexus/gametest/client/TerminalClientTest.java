package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.barrel.BarrelTier;
import net.storagenexus.terminal.TerminalMenu;

/** Opens a real terminal in a real client and clicks the grid the way a player does. */
public class TerminalClientTest implements FabricClientGameTest {
    /** Ticks the button is held: a real click lasts several, long enough for the server's answer to arrive before release. */
    private static final int HOLD = 5;
    private static boolean DRAG;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1920, 1080);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().get(0);
                BlockPos base = player.blockPosition().offset(0, 0, 3);
                level.setBlockAndUpdate(base, ModBlocks.CRAFTING_TERMINAL.defaultBlockState());
                level.setBlockAndUpdate(base.east(), ModBlocks.STORAGE_CONNECTOR.defaultBlockState());
                level.setBlockAndUpdate(base.east(2), Blocks.CHEST.defaultBlockState());
                ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(base.east(2));
                chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
                chest.setItem(1, new ItemStack(Items.IRON_INGOT, 40));
                chest.setItem(2, new ItemStack(Items.DIAMOND, 7));
                player.getInventory().clearContent();
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> {
                BlockPos pos = mc.player.blockPosition().offset(0, 0, 3);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> s && s.getMenu() instanceof TerminalMenu);
            ctx.waitTicks(40);
            ctx.takeScreenshot("terminal_open");

            for (int pass = 0; pass < 3; pass++) {
                System.out.println("=== pass " + pass + (pass == 1 ? " (recipe book open)" : pass == 2 ? " (gui scale 2, drag)" : ""));
                if (pass == 2) { DRAG = true; ctx.runOnClient(mc -> { mc.options.guiScale().set(2); mc.resizeGui(); }); ctx.waitTicks(10); }
                if (pass == 1) {
                    ctx.runOnClient(mc -> {
                        try {
                            var s = (net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen<?>) mc.gui.screen();
                            var m = net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen.class.getDeclaredMethod("onRecipeBookButtonClick");
                            m.setAccessible(true);
                            var book = net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen.class.getDeclaredField("recipeBookComponent");
                            book.setAccessible(true);
                            ((net.minecraft.client.gui.screens.recipebook.RecipeBookComponent<?>) book.get(s)).toggleVisibility();
                            m.invoke(s);
                        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                    });
                    ctx.waitTicks(10);
                }
                click(ctx, 0, 0, 0, false, "left on planks (take stack)");
                click(ctx, 5, 0, 0, false, "left on empty cell (deposit all)");
                click(ctx, 0, 0, 1, false, "right on planks (take one)");
                click(ctx, 0, 0, 1, false, "right on planks (deposit one)");
                click(ctx, 1, 0, 0, true, "shift-left on iron (to inventory)");
                click(ctx, 2, 0, 1, false, "right on diamonds (take one)");
                click(ctx, 8, 0, 0, false, "left on empty cell holding diamond (deposit)");
                click(ctx, 2, 1, 0, false, "left, 2nd row empty");
                if (pass == 2) { click(ctx, 0, 0, 0, false, "scale2: take planks"); click(ctx, 4, 7, 0, false, "scale2: deposit deep row"); click(ctx, 0, 0, 1, false, "scale2: take one iron/planks"); click(ctx, 4, 7, 1, false, "scale2: deposit one deep row"); }
            }
            // ---- the other screens must open and render without crashing
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
            ctx.waitTicks(5);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().get(0);
                BlockPos base = player.blockPosition().offset(-5, 0, 2);
                level.setBlockAndUpdate(base, ModBlocks.NETWORK_BUS.defaultBlockState());
                level.setBlockAndUpdate(base.east(2), ModBlocks.LEVEL_EMITTER.defaultBlockState());
                level.setBlockAndUpdate(base.east(4), ModBlocks.BARRELS.get(net.storagenexus.barrel.BarrelTier.NETHERITE).defaultBlockState());
                level.setBlockAndUpdate(base.east(6), ModBlocks.BARRELS.get(net.storagenexus.barrel.BarrelTier.IRON).defaultBlockState());
                level.setBlockAndUpdate(base.east(8), ModBlocks.DIMENSIONAL_INPUT.defaultBlockState());
                level.setBlockAndUpdate(base.east(10), ModBlocks.COMPACTING_DRAWER.defaultBlockState());
            });
            ctx.waitTicks(5);
            for (int dx : new int[]{0, 2, 4, 6, 8}) {
                final int fx = dx;
                ctx.runOnClient(mc -> {
                    BlockPos pos = mc.player.blockPosition().offset(fx - 5, 0, 2);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                });
                ctx.waitFor(mc -> mc.gui.screen() != null);
                ctx.waitTicks(15);
                String name = ctx.computeOnClient(mc -> mc.gui.screen().getClass().getSimpleName());
                System.out.println("SCREEN at +" + dx + ": " + name);
                ctx.takeScreenshot("screen_" + name + "_" + dx);
                ctx.runOnClient(mc -> mc.gui.setScreen(null));
                ctx.waitTicks(5);
            }
        }
    }

    /** Clicks grid cell (col,row) with the given mouse button and prints the carried stack and the inventory. */
    private static void click(ClientGameTestContext ctx, int col, int row, int button, boolean shift, String what) {
        double[] p = ctx.computeOnClient(mc -> {
            var ac = (AbstractContainerScreen<?>) mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            double x = (field(ac, "leftPos") + TerminalMenu.GRID_X + col * 18 + 8) * scale;
            double y = (field(ac, "topPos") + TerminalMenu.GRID_Y + row * 18 + 8) * scale;
            return new double[]{x, y};
        });
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(2);
        if (shift) ctx.getInput().holdShift();
        if (DRAG) {
            ctx.getInput().holdMouse(button);
            ctx.getInput().moveCursor(1, 1);
            ctx.waitTicks(HOLD);
            ctx.getInput().releaseMouse(button);
        } else {
            ctx.getInput().holdMouseFor(button, HOLD);
        }
        if (shift) ctx.getInput().releaseShift();
        ctx.waitTicks(12);
        String state = ctx.computeOnClient(mc -> {
            var inv = new StringBuilder();
            for (ItemStack s : mc.player.getInventory().getNonEquipmentItems()) if (!s.isEmpty()) inv.append(s).append(' ');
            var ac = (AbstractContainerScreen<?>) mc.gui.screen();
            return "carried=[" + mc.player.containerMenu.getCarried() + "] inv=[" + inv + "] rows=" + ((TerminalMenu) ac.getMenu()).rows()
                    + " gui=" + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight();
        });
        System.out.println("CLICK " + what + " -> " + state);
    }

    private static int field(Object o, String name) {
        try {
            var f = AbstractContainerScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
