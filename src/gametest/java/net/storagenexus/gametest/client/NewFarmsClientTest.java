package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.assembler.AssemblyTarget;
import net.storagenexus.essentials.assembler.FarmAssemblerBlockEntity;
import net.storagenexus.essentials.assembler.FarmAssemblerMenu;
import net.storagenexus.essentials.client.FarmAssemblerScreen;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.util.Menus;

/**
 * The Witch, Sugar Cane, Bamboo and Nether Wart farms in a real game: their faces, and the Farm Assembler's list, which
 * now has more rows than fit and scrolls: the chosen row is kept in view, the mouse wheel scrolls, and a click on a
 * scrolled row picks that target.
 */
public class NewFarmsClientTest implements FabricClientGameTest {
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
                at[0] = base;
                var level = player.level();
                FarmKind[] kinds = {FarmKind.WITCH, FarmKind.SUGAR_CANE, FarmKind.BAMBOO, FarmKind.NETHER_WART};
                for (int i = 0; i < kinds.length; i++) {
                    level.setBlockAndUpdate(base.offset(i * 2 - 3, 0, 3), ModBlocks.FARMS.get(kinds[i]).defaultBlockState());
                }
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 1.5, 0.0F, 14.0F);
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(14.0F);
            });
            ctx.getInput().pressKey(options -> options.keyToggleGui);
            ctx.waitTicks(20);
            System.out.println("[nexus] new farms: " + ctx.takeScreenshot("nexus_new_farms"));
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            // The Assembler on the Nether Wart Farm (the last of the farms): the list scrolls so it is in view
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = at[0].offset(0, 0, 6);
                player.level().setBlockAndUpdate(pos, ModBlocks.FARM_ASSEMBLER.defaultBlockState());
                FarmAssemblerBlockEntity assembler = (FarmAssemblerBlockEntity) player.level().getBlockEntity(pos);
                assembler.select(AssemblyTarget.NETHER_WART, null);
                assembler.accept(new ItemStack(Items.NETHER_WART, 20));
                assembler.accept(new ItemStack(Items.SOUL_SAND, 16));
                assembler.accept(new ItemStack(Items.LAVA_BUCKET, 2));
                for (int i = 0; i < 6; i++) assembler.accept(new ItemStack(Items.NETHERRACK, 64));
                Menus.open(player, net.minecraft.network.chat.Component.translatable("block.nexusessentials.farm_assembler"), pos,
                        (id, inventory, p) -> new FarmAssemblerMenu(id, inventory, assembler));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmAssemblerScreen), "the Assembler opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            int scroll = ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).listScroll());
            check(scroll == AssemblyTarget.NETHER_WART.ordinal() - FarmAssemblerScreen.VISIBLE_ROWS + 1,
                    "the list scrolled to keep the Nether Wart Farm in view, first row " + scroll);
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] assembler scrolled: " + ctx.takeScreenshot("nexus_assembler_scrolled"));

            // The wheel scrolls it back up, and the top shows the Iron Farm again
            ctx.runOnClient(mc -> {
                var screen = (FarmAssemblerScreen) mc.gui.screen();
                double scale = mc.getWindow().getGuiScale();
                int left = (screen.width - FarmAssemblerMenu.WIDTH) / 2, top = (screen.height - FarmAssemblerMenu.HEIGHT) / 2;
                for (int i = 0; i < 6; i++) screen.mouseScrolled(left + FarmAssemblerMenu.LIST_X + 20, top + FarmAssemblerMenu.LIST_Y + 20, 0, 1);
            });
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).listScroll()) == 0, "the wheel scrolls the list to the top");
            // Click the 7th row (the Witch Farm) there
            clickAt(ctx, FarmAssemblerMenu.LIST_X + 30, FarmAssemblerMenu.LIST_Y + 1 + 6 * FarmAssemblerMenu.LIST_ROW + 8);
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().target()) == AssemblyTarget.WITCH, "a click picks the Witch Farm");
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] assembler witch: " + ctx.takeScreenshot("nexus_assembler_witch"));
        }
    }

    private static void clickAt(ClientGameTestContext ctx, int guiX, int guiY) {
        double[] at = ctx.computeOnClient(mc -> {
            var screen = mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            int left = (screen.width - FarmAssemblerMenu.WIDTH) / 2, top = (screen.height - FarmAssemblerMenu.HEIGHT) / 2;
            return new double[]{(left + guiX) * scale, (top + guiY) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(6);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
