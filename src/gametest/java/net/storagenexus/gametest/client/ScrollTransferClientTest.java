package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The scroll wheel over a stack moves one item at a time: down sends it to the other inventory, up pulls a matching one
 * in. It works between a chest and the player's inventory and hotbar, and not in the player's own inventory screen.
 */
public class ScrollTransferClientTest implements FabricClientGameTest {
    private SimpleContainer chest;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            openChest(ctx, world);

            // Scroll down over the torches in the main inventory: one at a time into the chest
            moveTo(ctx, 8, 85);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(6);
            check(inChest(Items.TORCH) == 1 && inInventory(world, Items.TORCH) == 19, "one torch went to the chest: chest " + inChest(Items.TORCH) + ", inventory " + inInventory(world, Items.TORCH));
            ctx.getInput().scroll(-1);
            ctx.waitTicks(6);
            check(inChest(Items.TORCH) == 2 && inInventory(world, Items.TORCH) == 18, "and a second one: chest " + inChest(Items.TORCH));
            check(chestStacks(Items.TORCH) == 1, "on the same stack in the chest");

            // Scroll up over the same stack: one comes back from the chest
            ctx.getInput().scroll(1);
            ctx.waitTicks(6);
            check(inChest(Items.TORCH) == 1 && inInventory(world, Items.TORCH) == 19, "scrolling up pulled one back: chest " + inChest(Items.TORCH) + ", inventory " + inInventory(world, Items.TORCH));

            // Scroll up over the torch in the chest: one more comes in from the inventory
            moveTo(ctx, 8 + 18, 18);
            ctx.getInput().scroll(1);
            ctx.waitTicks(6);
            check(inChest(Items.TORCH) == 2 && inInventory(world, Items.TORCH) == 18, "scrolling up over the chest stack pulled one in: chest " + inChest(Items.TORCH));

            // Scroll down over apples in the chest: one into the player's inventory
            moveTo(ctx, 8, 18);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(6);
            check(inChest(Items.APPLE) == 63 && inInventory(world, Items.APPLE) == 1, "one apple went to the player: chest " + inChest(Items.APPLE));
            System.out.println("[nexus] scroll chest: " + ctx.takeScreenshot("nexus_scroll_chest"));

            // The hotbar works too: five scrolls empty the stack of bread, and a sixth does nothing
            moveTo(ctx, 8, 143);
            for (int i = 0; i < 6; i++) {
                ctx.getInput().scroll(-1);
                ctx.waitTicks(3);
            }
            ctx.waitTicks(4);
            check(inChest(Items.BREAD) == 5 && inInventory(world, Items.BREAD) == 0, "the hotbar bread went to the chest: " + inChest(Items.BREAD));
            check(ctx.computeOnClient(mc -> ((AbstractContainerScreen<?>) mc.gui.screen()).getMenu().getCarried().isEmpty()), "nothing is left on the cursor");

            // The player's own inventory screen keeps vanilla's wheel: nothing moves
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.closeContainer();
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.BREAD, 5));
            });
            ctx.waitTicks(5);
            ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            ctx.waitTicks(5);
            moveTo(ctx, 8, 142);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(6);
            check(inInventory(world, Items.BREAD) == 5, "the wheel moves nothing in the inventory screen");
        }
    }

    private void openChest(ClientGameTestContext ctx, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().setItem(9, new ItemStack(Items.TORCH, 20));
            player.getInventory().setItem(0, new ItemStack(Items.BREAD, 5));
            chest = new SimpleContainer(27);
            chest.setItem(0, new ItemStack(Items.APPLE, 64));
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

    private int chestStacks(Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n++;
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
