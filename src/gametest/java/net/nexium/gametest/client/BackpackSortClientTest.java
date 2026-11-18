package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.client.SortClient;
import net.nexium.client.screen.BackpackScreen;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.backpack.Backpacks;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sort key over a Backpack's screen in a real game: over its 36 slots it sorts only those (A to Z, stacks merged to 1,024, the
 * shulker box last, the modules where they were); over a module slot it does nothing; over the inventory it sorts only the
 * inventory, and a Backpack opened from an inventory slot stays in that slot with the screen still open.
 */
public class BackpackSortClientTest implements FabricClientGameTest {
    private static final String SORTED = "apple x5, bread x20, cobblestone x94, diamond x12, ender_pearl x16, iron_pickaxe x1, torch x600";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ExtraSlots.of(player).clearContent();
                ItemStack worn = new ItemStack(ModItems.BACKPACK);
                List<ItemStack> modules = new ArrayList<>();
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.RESTOCKING)));
                ItemStack feeding = new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING));
                feeding.set(ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
                modules.add(feeding);
                for (int i = modules.size(); i < BackpackModules.SLOTS; i++) modules.add(ItemStack.EMPTY);
                BackpackModules.setSlots(worn, modules);
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.IRON_PICKAXE));
                contents.set(3, new ItemStack(Items.TORCH, 300));
                contents.set(4, new ItemStack(Items.DIAMOND, 12));
                contents.set(8, new ItemStack(Items.COBBLESTONE, 64));
                contents.set(10, new ItemStack(Items.BREAD, 20));
                contents.set(14, new ItemStack(Items.TORCH, 300));
                contents.set(19, new ItemStack(Items.APPLE, 5));
                contents.set(22, new ItemStack(Items.COBBLESTONE, 30));
                contents.set(30, new ItemStack(Items.SHULKER_BOX));
                contents.set(35, new ItemStack(Items.ENDER_PEARL, 16));
                BackpackContents.write(worn, contents);
                ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
                // Inventory: some items to sort, and a second Backpack in a main slot for the "opened from the inventory" part
                player.getInventory().setItem(9, new ItemStack(Items.STONE, 20));
                player.getInventory().setItem(11, new ItemStack(Items.COAL, 4));
                player.getInventory().setItem(20, new ItemStack(Items.APPLE, 2));
                player.getInventory().setItem(0, new ItemStack(Items.BREAD, 3));
            });
            ctx.waitTicks(20);

            // Open the worn Backpack with its key
            ctx.getInput().pressKey(NexiumEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "B opens the Backpack's screen");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] backpack sort before: " + ctx.takeScreenshot("nexus_backpack_sort_before"));
            String before = state(world);

            // Over a module slot: nothing is an area, nothing changes
            moveTo(ctx, BackpackMenu.MODULE_X + 8, BackpackMenu.MODULE_Y + 8);
            press(ctx);
            check(state(world).equals(before), "the sort key over a module slot changes nothing");

            // Over an empty one of its storage slots: only the Backpack's 36 slots sort
            moveTo(ctx, BackpackMenu.GRID_X + 7 * 18 + 8, BackpackMenu.GRID_Y + 3 * 18 + 8);
            press(ctx);
            ctx.waitTicks(4);
            String sorted = world.getServer().computeOnServer(server -> describe(BackpackContents.read(ExtraSlots.of(server.getPlayerList().getPlayers().get(0)).backpack())));
            check(sorted.startsWith(SORTED + ", -") && sorted.endsWith(", shulker_box x1"), "the Backpack sorted A to Z, stacks merged to 1,024, the shulker box last: " + sorted);
            String after = state(world);
            check(inventoryPart(after).equals(inventoryPart(before)), "the inventory did not change: " + inventoryPart(after));
            check(modulePart(after).equals(modulePart(before)), "the modules did not move: " + modulePart(after));
            // The client sees the same thing
            String shown = ctx.computeOnClient(mc -> {
                BackpackMenu menu = ((BackpackScreen) mc.gui.screen()).getMenu();
                return menu.getSlot(BackpackModules.SLOTS).getItem().getHoverName().getString() + " / " + menu.getSlot(BackpackModules.SLOTS + 6).getItem().getCount()
                        + " / " + menu.getSlot(BackpackModules.SLOTS + 7).getItem().getCount();
            });
            check(shown.equals("Apple / 600 / 0"), "the open screen shows the sorted Backpack (first slot, then the torches): " + shown);
            System.out.println("[nexus] backpack sorted: " + ctx.takeScreenshot("nexus_backpack_sorted"));

            // Over the inventory: only the inventory sorts, never the hotbar
            moveTo(ctx, BackpackMenu.GRID_X + 3 * 18 + 8, BackpackMenu.INVENTORY_Y + 18 + 8);
            press(ctx);
            ctx.waitTicks(4);
            String last = state(world);
            check(contentsPart(last).equals(contentsPart(after)), "the Backpack did not change");
            check(last.contains("9=2 apple;10=4 coal;11=20 stone;"), "the inventory sorted: " + inventoryPart(last));
            check(last.contains("0=3 bread;"), "the hotbar did not change: " + last);

            // A Backpack opened from an inventory slot stays in that slot when the inventory sorts
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ItemStack spare = new ItemStack(ModItems.BACKPACK);
                BackpackContents.write(spare, List.of(new ItemStack(Items.GOLD_INGOT, 3)));
                player.getInventory().setItem(12, spare);
                player.getInventory().setItem(30, new ItemStack(Items.ACACIA_LOG, 6));
                Backpacks.open(player, 12);
            });
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen s && s.getMenu().source() == 12), "the Backpack in slot 12 opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            moveTo(ctx, BackpackMenu.GRID_X + 3 * 18 + 8, BackpackMenu.INVENTORY_Y + 2 * 18 + 8);
            press(ctx);
            ctx.waitTicks(4);
            String locked = world.getServer().computeOnServer(server -> {
                var inv = server.getPlayerList().getPlayers().get(0).getInventory();
                return name(inv.getItem(12)) + " | " + name(inv.getItem(9)) + " | " + name(inv.getItem(10)) + " | " + name(inv.getItem(11)) + " | " + name(inv.getItem(13));
            });
            check(locked.equals("1 backpack | 6 acacia_log | 2 apple | 4 coal | 20 stone"),
                    "the Backpack is still in slot 12 and the items around it sorted (slot 12, 9, 10, 11, 13): " + locked);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "and its screen is still open");
            System.out.println("[nexus] backpack from the inventory, inventory sorted: " + ctx.takeScreenshot("nexus_backpack_sort_locked"));
        }
    }

    private static String name(ItemStack s) {
        return s.isEmpty() ? "-" : s.getCount() + " " + s.getItem().toString().replaceAll("^[a-z_]+:", "");
    }

    private static String describe(List<ItemStack> stacks) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : stacks) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.isEmpty() ? "-" : s.getItem().toString().replace("minecraft:", "") + " x" + s.getCount());
        }
        // Shorten the run of empty slots to one dash so the check reads the order of what is there
        return sb.toString().replaceAll("(, -)+", ", -");
    }

    /** Everything the test compares: modules, contents and the main inventory with the hotbar, as saved on the server. */
    private static String state(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ItemStack backpack = ExtraSlots.of(player).backpack();
            StringBuilder sb = new StringBuilder("modules:");
            for (ItemStack m : BackpackModules.slots(backpack)) sb.append(m.isEmpty() ? "-" : m.getItem().toString()).append(',');
            sb.append("|contents:");
            for (ItemStack s : BackpackContents.read(backpack)) sb.append(s.isEmpty() ? "-" : s.getCount() + " " + s.getItem().toString().replace("minecraft:", "")).append(';');
            sb.append("|inv:");
            for (int i = 0; i < 36; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty()) sb.append(i).append('=').append(s.getCount()).append(' ').append(s.getItem().toString().replace("minecraft:", "")).append(';');
            }
            return sb.toString();
        });
    }

    private static String modulePart(String s) { return s.substring(0, s.indexOf("|contents:")); }
    private static String contentsPart(String s) { return s.substring(s.indexOf("|contents:"), s.indexOf("|inv:")); }
    private static String inventoryPart(String s) { return s.substring(s.indexOf("|inv:")); }

    /** Puts the mouse on a point given relative to the screen's top-left corner. */
    private static void moveTo(ClientGameTestContext ctx, int x, int y) {
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
