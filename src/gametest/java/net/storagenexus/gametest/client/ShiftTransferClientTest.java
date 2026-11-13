package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shift + left click on an item sends every stack of that item to the other inventory, and shift + left click and
 * drag moves every item the cursor passes over.
 */
public class ShiftTransferClientTest implements FabricClientGameTest {
    private SimpleContainer chest;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            open(ctx, world);

            // Shift + click on one stack of apples: all three stacks go, the bread and torches stay
            moveTo(ctx, 8 + 18 * 1, 18);
            ctx.getInput().holdShift();
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.getInput().releaseShift();
            ctx.waitTicks(6);
            check(inChest(Items.APPLE) == 0, "every stack of apples left the chest, " + inChest(Items.APPLE) + " left");
            check(inInventory(world, Items.APPLE) == 64 + 64 + 10, "and all of them are in the inventory, " + inInventory(world, Items.APPLE));
            check(inChest(Items.BREAD) == 5 + 7 && inChest(Items.TORCH) == 22, "the bread and torches stayed");
            System.out.println("[nexus] shift click all: " + ctx.takeScreenshot("nexus_shift_click_all"));

            // Shift + click and drag along the top row (bread, torches, an empty slot): everything the cursor crosses moves
            open(ctx, world);
            moveTo(ctx, 8 + 18 * 8, 18 + 18 * 2); // an empty slot to start from
            ctx.getInput().holdShift();
            ctx.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            for (int col = 7; col >= 0; col--) {
                moveTo(ctx, 8 + 18 * col, 18 + 18 * 2);
            }
            for (int row = 1; row >= 0; row--) {
                for (int col = 0; col < 9; col++) moveTo(ctx, 8 + 18 * col, 18 + 18 * row);
            }
            ctx.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.getInput().releaseShift();
            ctx.waitTicks(6);
            int left = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) if (!chest.getItem(i).isEmpty()) left++;
            check(left == 0, "the drag emptied every slot it crossed, " + left + " stacks left");
            check(inInventory(world, Items.BREAD) == 12 && inInventory(world, Items.TORCH) == 22 && inInventory(world, Items.APPLE) == 138,
                    "all the items arrived: bread " + inInventory(world, Items.BREAD) + ", torches " + inInventory(world, Items.TORCH));
            System.out.println("[nexus] shift drag: " + ctx.takeScreenshot("nexus_shift_drag"));

        }
    }

    private void open(ClientGameTestContext ctx, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.closeContainer();
            player.getInventory().clearContent();
            chest = new SimpleContainer(27);
            chest.setItem(1, new ItemStack(Items.APPLE, 64));
            chest.setItem(5, new ItemStack(Items.APPLE, 64));
            chest.setItem(20, new ItemStack(Items.APPLE, 10));
            chest.setItem(3, new ItemStack(Items.BREAD, 5));
            chest.setItem(9, new ItemStack(Items.BREAD, 7));
            chest.setItem(12, new ItemStack(Items.TORCH, 22));
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest),
                    Component.translatable("container.chest")));
        });
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?>), "the chest opens");
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
    }

    private int inChest(Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    private static int inInventory(TestSingleplayerContext world, Item item) {
        int[] n = new int[1];
        world.getServer().runOnServer(server -> n[0] = server.getPlayerList().getPlayers().get(0).getInventory().countItem(item));
        return n[0];
    }

    /** Puts the mouse on a point given relative to the screen's top-left corner. */
    private static void moveTo(ClientGameTestContext ctx, int x, int y) {
        double[] at = ctx.computeOnClient(mc -> {
            var screen = (AbstractContainerScreen<?>) mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            return new double[]{(screen.leftPos + x + 8) * scale, (screen.topPos + y + 8) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(1);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
