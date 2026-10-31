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
    /** Mouse buttons as 26.x numbers them (SDL): left 1, right 3. */
    private static final int LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
    private static final int RIGHT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

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
                expect(click(ctx, 0, 0, LEFT, false, "left on planks (take stack)"), "carried=[64 minecraft:oak_planks]");
                expect(click(ctx, 5, 0, LEFT, false, "left on empty cell (deposit all)"), "carried=[0 minecraft:air]");
                expect(click(ctx, 0, 0, RIGHT, false, "right on planks (take one)"), "carried=[1 minecraft:oak_planks]");
                expect(click(ctx, 0, 0, RIGHT, false, "right on planks (deposit one)"), "carried=[0 minecraft:air]");
                click(ctx, 1, 0, LEFT, true, "shift-left on iron (to inventory)");
                click(ctx, 2, 0, RIGHT, false, "right on diamonds (take one)");
                click(ctx, 8, 0, LEFT, false, "left on empty cell holding diamond (deposit)");
                click(ctx, 2, 1, LEFT, false, "left, 2nd row empty");
                if (pass == 2) { click(ctx, 0, 0, LEFT, false, "scale2: take planks"); click(ctx, 4, 7, LEFT, false, "scale2: deposit deep row"); click(ctx, 0, 0, RIGHT, false, "scale2: take one iron/planks"); click(ctx, 4, 7, RIGHT, false, "scale2: deposit one deep row"); }
            }
            // ---- a stack of mod items (Gold Barrel Upgrades), alone in the network
            ctx.runOnClient(mc -> mc.player.containerMenu.setCarried(ItemStack.EMPTY));
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                player.containerMenu.setCarried(ItemStack.EMPTY);
                player.getInventory().clearContent();
                ChestBlockEntity chest = (ChestBlockEntity) server.overworld().getBlockEntity(player.blockPosition().offset(2, 0, 3));
                chest.clearContent();
                chest.setItem(5, new ItemStack(net.storagenexus.registry.ModItems.BARREL_UPGRADES.get(BarrelTier.GOLD), 64));
            });
            ctx.waitTicks(30);
            DRAG = false;
            expect(click(ctx, 0, 0, LEFT, false, "left on 64 gold barrel upgrades (take stack)"), "carried=[64 storagenexus:gold_barrel_upgrade]");
            DRAG = true;
            expect(click(ctx, 0, 0, LEFT, false, "left on gold barrel upgrades again, holding them (deposit all)"), "carried=[0 minecraft:air]");
            expect(click(ctx, 0, 0, RIGHT, false, "right on gold barrel upgrades (take one)"), "carried=[1 storagenexus:gold_barrel_upgrade]");
            expect(click(ctx, 0, 0, RIGHT, false, "right holding one (deposit one)"), "carried=[0 minecraft:air]");
            click(ctx, 0, 0, LEFT, false, "left with drag on gold barrel upgrades (take stack)");
            // ---- a recipe viewer's "+" button: fill the grid for oak planks -> crafting table from the network
            ctx.runOnClient(mc -> mc.player.containerMenu.setCarried(ItemStack.EMPTY));
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                player.containerMenu.setCarried(ItemStack.EMPTY);
                ChestBlockEntity chest = (ChestBlockEntity) server.overworld().getBlockEntity(player.blockPosition().offset(2, 0, 3));
                chest.clearContent();
                chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
            });
            ctx.waitTicks(30);
            ctx.runOnClient(mc -> net.storagenexus.client.compat.RecipeTransfer.fill(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                            net.minecraft.resources.Identifier.withDefaultNamespace("crafting_table")), false));
            ctx.waitTicks(10);
            String grid = ctx.computeOnClient(mc -> {
                StringBuilder b = new StringBuilder();
                for (var slot : ((TerminalMenu) mc.player.containerMenu).getInputGridSlots()) b.append(slot.getItem().getCount()).append(' ');
                return b.toString().trim() + " result=" + ((TerminalMenu) mc.player.containerMenu).getResultSlot().getItem();
            });
            System.out.println("FILLGRID " + grid);
            if (!grid.contains("result=1 minecraft:crafting_table")) throw new AssertionError("grid not filled for a crafting table: " + grid);
            boolean jei = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei");
            System.out.println("FILLGRID jei loaded: " + jei);
            if (jei) {
                // Empty the grid back into the network, then press JEI's own "+" for the crafting table
                ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, TerminalMenu.CLEAR_GRID));
                ctx.waitTicks(10);
                String handler = ctx.computeOnClient(JeiCheck::pressPlus);
                System.out.println("FILLGRID jei handler: " + handler);
                if (!handler.contains("StorageNexusJeiPlugin")) throw new AssertionError("JEI did not use our transfer handler: " + handler);
                ctx.waitTicks(10);
                String grid2 = ctx.computeOnClient(mc -> ((TerminalMenu) mc.player.containerMenu).getResultSlot().getItem().toString());
                System.out.println("FILLGRID after JEI +: result=" + grid2);
                if (!grid2.equals("1 minecraft:crafting_table")) throw new AssertionError("JEI + did not fill the grid: " + grid2);
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
                BlockPos drawerPos = player.blockPosition().offset(1, 0, -2);
                level.setBlockAndUpdate(drawerPos, ModBlocks.DRAWERS.get(net.storagenexus.drawer.DrawerWood.OAK)[2].defaultBlockState());
                if (level.getBlockEntity(drawerPos) instanceof net.storagenexus.drawer.DrawerBlockEntity d) {
                    d.tryInstall(new ItemStack(net.storagenexus.registry.ModItems.NETHERITE_UPGRADE));
                    d.tryInstall(new ItemStack(net.storagenexus.registry.ModItems.EMERALD_UPGRADE));
                }
            });
            ctx.waitTicks(5);
            for (int dx : new int[]{0, 2, 4, 6, 8, 10, 12}) {
                final int fx = dx;
                ctx.runOnClient(mc -> {
                    BlockPos pos = fx == 12 ? mc.player.blockPosition().offset(1, 0, -2) : mc.player.blockPosition().offset(fx - 5, 0, 2);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                });
                ctx.waitFor(mc -> mc.gui.screen() != null);
                ctx.waitTicks(15);
                String name = ctx.computeOnClient(mc -> mc.gui.screen().getClass().getSimpleName());
                System.out.println("SCREEN at +" + dx + ": " + name);
                String info = ctx.computeOnClient(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> sc
                        && sc.getMenu() instanceof net.storagenexus.upgrade.UpgradeMenu um
                        ? "x" + um.multiplier() + " total " + um.totalCapacity() + " slots " + um.capacitySlots() : "-");
                System.out.println("SCREEN info +" + dx + ": " + info);
                ctx.takeScreenshot("screen_" + name + "_" + dx);
                ctx.runOnClient(mc -> mc.gui.setScreen(null));
                ctx.waitTicks(5);
            }
        }
    }

    /** Clicks grid cell (col,row) with the given mouse button and prints the carried stack and the inventory. */
    private static String click(ClientGameTestContext ctx, int col, int row, int button, boolean shift, String what) {
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
        return state;
    }

    private static void expect(String state, String wanted) {
        if (!state.startsWith(wanted)) throw new AssertionError("expected " + wanted + " but got " + state);
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
