package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.client.screen.BackpackScreen;
import net.storagenexus.essentials.backpack.BackpackContents;
import net.storagenexus.essentials.backpack.BackpackMenu;
import net.storagenexus.essentials.backpack.BackpackModuleType;
import net.storagenexus.essentials.backpack.BackpackModules;
import net.storagenexus.essentials.backpack.Backpacks;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.slots.ExtraSlotMenuSlot;
import net.storagenexus.essentials.slots.ExtraSlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Backpack in a real game: its slot above the magnet slot in the inventory, the Open Backpack key (B, listed in the key
 * binds) opening the worn one and closing it again, right-click opening the one in the hand, the screen with its modules
 * and their switches, a stack of 256 torches sent to the client in full, and the Torch Stack switch putting the extra torches
 * back. Takes screenshots of the inventory and of the screen.
 */
public class BackpackClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            check(Arrays.asList(mc.options.keyMappings).contains(NexusEssentialsClient.OPEN_BACKPACK), "the Open Backpack key is in the game's key list");
            check(NexusEssentialsClient.OPEN_BACKPACK.getDefaultKey().getValue() == InputConstants.KEY_B, "it defaults to B");
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            // Nothing worn yet: the backpack slot shows its silhouette, above the magnet slot's
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ExtraSlots.of(player).clearContent();
            });
            ctx.waitTicks(10);
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> {
                var slot = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_BACKPACK);
                return slot.getItem().isEmpty() && ExtraSlotMenuSlot.EMPTY_BACKPACK_ICON.equals(slot.getNoItemIcon());
            }), "an empty backpack slot shows the backpack silhouette");
            System.out.println("[nexus] empty backpack slot: " + ctx.takeScreenshot("nexus_backpack_slot_empty"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ExtraSlots.of(player).clearContent();
                ItemStack worn = new ItemStack(ModItems.BACKPACK);
                List<ItemStack> modules = new ArrayList<>();
                // Auto Torch starts switched off so it doesn't spend torches on the ground around the player before the counts are checked
                ItemStack autoTorch = new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.AUTO_TORCH));
                autoTorch.set(net.storagenexus.registry.ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
                modules.add(autoTorch);
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.TORCH_STACK)));
                for (int i = modules.size(); i < BackpackModules.SLOTS; i++) modules.add(ItemStack.EMPTY);
                BackpackModules.setSlots(worn, modules);
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.TORCH, 256));
                contents.set(1, new ItemStack(Items.DIAMOND, 12));
                contents.set(2, new ItemStack(Items.IRON_PICKAXE));
                contents.set(9, new ItemStack(Items.COBBLESTONE, 64));
                contents.set(10, new ItemStack(Items.BREAD, 20));
                contents.set(35, new ItemStack(Items.ENDER_PEARL, 16));
                BackpackContents.write(worn, contents);
                ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
                ExtraSlots.of(player).setItem(ExtraSlots.TRINKET, new ItemStack(ModItems.MAGNET));
                ItemStack spare = new ItemStack(ModItems.BACKPACK);
                BackpackContents.write(spare, List.of(new ItemStack(Items.GOLD_INGOT, 3)));
                player.getInventory().setItem(3, spare);
                player.getInventory().setItem(0, new ItemStack(Items.TORCH, 64));
                player.getInventory().setItem(1, new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.AUTO_TORCH)));
            });
            ctx.waitTicks(20);

            // The inventory: the backpack slot is right above the magnet slot and shows the backpack
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            String slots = ctx.computeOnClient(mc -> {
                var backpack = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_BACKPACK);
                var trinket = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_TRINKET);
                return backpack.x + "," + backpack.y + " " + backpack.getItem().getItem() + " / " + trinket.x + "," + trinket.y;
            });
            check(slots.startsWith("77,8 ") && slots.contains("nexusessentials:backpack") && slots.endsWith("77,26"), "the backpack slot is above the magnet slot and holds the backpack: " + slots);
            System.out.println("[nexus] backpack in the inventory: " + ctx.takeScreenshot("nexus_backpack_inventory"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            // The key opens the worn backpack; the same key closes it
            ctx.getInput().pressKey(NexusEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "B opens the backpack's screen");
            String shown = ctx.computeOnClient(mc -> {
                BackpackMenu menu = ((BackpackScreen) mc.gui.screen()).getMenu();
                var torches = menu.getSlot(BackpackModules.SLOTS).getItem();
                return menu.source() + " " + torches.getCount() + " " + torches.getItem() + " | " + menu.getSlot(BackpackModules.SLOTS + 1).getItem().getCount() + " | "
                        + menu.getSlot(0).getItem().getItem() + " " + menu.getSlot(1).getItem().getItem() + " " + menu.getSlot(2).getItem().isEmpty() + " | " + menu.torchStack();
            });
            check(shown.equals(Backpacks.EQUIPPED + " 256 minecraft:torch | 12 | nexusessentials:auto_torch_module nexusessentials:torch_stack_module true | true"),
                    "the client sees 256 torches in one slot, the diamonds, the modules and Torch Stack on: " + shown);
            // Switch Auto Torch on from inside the screen (it doesn't work while the screen is open)
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().getSlot(0).getItem().has(net.storagenexus.registry.ModComponents.MODULE_OFF)), "Auto Torch starts off");
            ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, BackpackMenu.BTN_TOGGLE_MODULE));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).getMenu().getSlot(0).getItem().has(net.storagenexus.registry.ModComponents.MODULE_OFF)), "its switch turns it on");
            System.out.println("[nexus] backpack screen: " + ctx.takeScreenshot("nexus_backpack_screen"));

            // The Torch Stack switch: off, the 256 torches become stacks of 64 (nothing is lost); on again, they can be put back
            ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, BackpackMenu.BTN_TOGGLE_MODULE + 1));
            ctx.waitTicks(10);
            String off = ctx.computeOnClient(mc -> {
                BackpackMenu menu = ((BackpackScreen) mc.gui.screen()).getMenu();
                int torches = 0, biggest = 0;
                for (int i = 0; i < BackpackContents.SIZE; i++) {
                    var stack = menu.getSlot(BackpackModules.SLOTS + i).getItem();
                    if (stack.is(Items.TORCH)) {
                        torches += stack.getCount();
                        biggest = Math.max(biggest, stack.getCount());
                    }
                }
                return torches + " " + biggest + " " + menu.torchStack();
            });
            check(off.equals("256 64 false"), "with Torch Stack off the 256 torches are stacks of 64, none lost (total, biggest, stacking): " + off);
            System.out.println("[nexus] backpack, Torch Stack off: " + ctx.takeScreenshot("nexus_backpack_torch_stack_off"));
            ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, BackpackMenu.BTN_TOGGLE_MODULE + 1));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().torchStack()), "the switch turns Torch Stack back on");

            ctx.getInput().pressKey(NexusEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "B closes it again");

            // Right-click with a backpack in the hand opens that one (its slot is outlined: it can't be moved)
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[3]);
            ctx.waitTicks(5);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(15);
            String held = ctx.computeOnClient(mc -> {
                if (!(mc.gui.screen() instanceof BackpackScreen screen)) return "no screen";
                return screen.getMenu().source() + " " + screen.getMenu().getSlot(BackpackModules.SLOTS).getItem().getItem();
            });
            check(held.equals("3 minecraft:gold_ingot"), "right-click opens the backpack in the hand (hotbar slot 3, holding 3 gold ingots): " + held);
            System.out.println("[nexus] backpack opened from the hand: " + ctx.takeScreenshot("nexus_backpack_held"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            // Sneak + right-click wears it, and the one that was worn comes to the hand
            ctx.getInput().holdShift();
            ctx.waitTicks(3);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.getInput().releaseShift();
            ctx.waitTicks(10);
            // (The backpack that was worn has Auto Torch on and was working while it was worn, so it may be a torch or two short of 256)
            String swapped = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                return BackpackContents.count(ExtraSlots.of(player).backpack(), Items.GOLD_INGOT) + " "
                        + !BackpackModules.find(inHand, BackpackModuleType.AUTO_TORCH).isEmpty() + " " + (BackpackContents.count(inHand, Items.TORCH) > 200);
            });
            check(swapped.equals("3 true true"), "sneak + right-click swapped the backpack in the hand (3 gold ingots) with the worn one (Auto Torch, most of its torches): " + swapped);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
