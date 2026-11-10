package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.client.SortClient;

/**
 * The sort key in a real game: R over the chest sorts only the chest, R over the inventory sorts only the main
 * inventory, R over the hotbar does nothing, and a second press steps to the next order.
 */
public class SortClientTest implements FabricClientGameTest {
    private SimpleContainer chest;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                var inv = player.getInventory();
                inv.setItem(0, new ItemStack(Items.STONE, 12));
                inv.setItem(3, new ItemStack(Items.APPLE, 4));
                inv.setItem(8, new ItemStack(Items.COAL, 7));
                inv.setItem(9, new ItemStack(Items.IRON_INGOT, 30));
                inv.setItem(11, new ItemStack(Items.BREAD, 2));
                inv.setItem(14, new ItemStack(Items.DIAMOND, 5));
                inv.setItem(17, new ItemStack(Items.BUNDLE));
                inv.setItem(20, new ItemStack(Items.COOKIE, 9));
                inv.setItem(22, new ItemStack(Items.IRON_INGOT, 40));
                inv.setItem(30, new ItemStack(Items.ACACIA_LOG, 20));
                inv.setItem(34, new ItemStack(Items.SHULKER_BOX));
                chest = new SimpleContainer(27);
                chest.setItem(1, new ItemStack(Items.SPRUCE_PLANKS, 40));
                chest.setItem(4, new ItemStack(Items.WHEAT, 11));
                chest.setItem(7, new ItemStack(Items.SHULKER_BOX));
                chest.setItem(10, new ItemStack(Items.APPLE, 3));
                chest.setItem(13, new ItemStack(Items.TORCH, 22));
                chest.setItem(18, new ItemStack(Items.SPRUCE_PLANKS, 40));
                chest.setItem(22, new ItemStack(Items.DIRT, 64));
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest),
                        Component.translatable("container.chest")));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?>), "the chest opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] sort before: " + ctx.takeScreenshot("nexus_sort_before"));

            // Over the hotbar: nothing happens
            String before = snapshot(world);
            moveTo(ctx, 8 + 18, 142 + 0 - 0, true);
            press(ctx);
            check(snapshot(world).equals(before), "R over the hotbar changes nothing");

            // Over an empty gap outside every area: nothing
            moveTo(ctx, 170, 30, false);
            press(ctx);
            check(snapshot(world).equals(before), "R outside the areas changes nothing");

            // Over an empty chest slot: only the chest is sorted
            moveTo(ctx, 8 + 18 * 8, 18 + 18 * 2, false);
            press(ctx);
            ctx.waitTicks(4);
            String afterChest = snapshot(world);
            check(chest.getItem(0).is(Items.DIRT) || chest.getItem(0).is(Items.SPRUCE_PLANKS) || chest.getItem(0).is(Items.APPLE),
                    "the chest sorted, first slot " + chest.getItem(0));
            check(chest.getItem(0).is(Items.APPLE) && chest.getItem(26).is(Items.SHULKER_BOX), "A to Z with the shulker box last");
            check(playerPart(afterChest).equals(playerPart(before)), "the inventory did not change");
            ctx.takeScreenshot("nexus_sort_chest");
            System.out.println("[nexus] sort chest: " + ctx.takeScreenshot("nexus_sort_chest_az"));

            // Pressing again steps to creative order
            press(ctx);
            ctx.waitTicks(4);
            check(chest.getItem(0).is(Items.DIRT) || !chest.getItem(0).is(Items.APPLE), "the second press changes the order, first slot " + chest.getItem(0));

            // Over the main inventory: only the inventory (never the hotbar)
            String chestNow = chestPart(snapshot(world));
            moveTo(ctx, 8 + 18 * 3, 84 + 18, true);
            press(ctx);
            ctx.waitTicks(4);
            String after = snapshot(world);
            check(chestPart(after).equals(chestNow), "the chest did not change");
            String hotbar = hotbarPart(before);
            check(hotbarPart(after).equals(hotbar), "the hotbar did not change: " + hotbarPart(after));
            check(!playerPart(after).equals(playerPart(before)), "the inventory was sorted");
            System.out.println("[nexus] sort inventory: " + ctx.takeScreenshot("nexus_sort_inventory"));
        }
    }

    private static String snapshot(TestSingleplayerContext world) {
        StringBuilder sb = new StringBuilder("chest:");
        world.getServer().runOnServer(server -> {});
        return sb.toString() + "|" + invString(world);
    }

    private static String invString(TestSingleplayerContext world) {
        String[] out = new String[1];
        world.getServer().runOnServer(server -> {
            var inv = server.getPlayerList().getPlayers().get(0).getInventory();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 36; i++) sb.append(i).append('=').append(inv.getItem(i)).append(';');
            out[0] = sb.toString();
        });
        return out[0];
    }

    private static String playerPart(String s) { return s.substring(s.indexOf('|') + 1).replaceAll("\\b[0-8]=[^;]*;", ""); }
    private static String hotbarPart(String s) {
        StringBuilder sb = new StringBuilder();
        for (String part : s.substring(s.indexOf('|') + 1).split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0 && Integer.parseInt(part.substring(0, eq)) < 9) sb.append(part).append(';');
        }
        return sb.toString();
    }
    private String chestPart(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 27; i++) sb.append(chest.getItem(i)).append(';');
        return sb.toString();
    }

    /** Puts the mouse on a point given relative to the screen's top-left corner. */
    private static void moveTo(ClientGameTestContext ctx, int x, int y, boolean unused) {
        double[] at = ctx.computeOnClient(mc -> {
            var screen = (AbstractContainerScreen<?>) mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            return new double[]{(screen.leftPos + x + 0.5) * scale, (screen.topPos + y + 0.5) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(3);
    }

    private static void press(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(options -> SortClient.SORT);
        ctx.waitTicks(6);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
