package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.nexium.client.screen.BackpackScreen;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Shift-clicking a small stack from the hotbar into a Backpack that has the Restocking module and already holds the same item: in a
 * real game, with the real server ticking, the item stays in the Backpack and is not handed back to the hotbar.
 */
public class RestockShiftClickClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            Item item = ModBlocks.FLUID_INTAKE.asItem();
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ExtraSlots.of(player).clearContent();
                ItemStack worn = new ItemStack(ModItems.BACKPACK);
                List<ItemStack> modules = new ArrayList<>();
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.RESTOCKING)));
                for (int i = modules.size(); i < BackpackModules.SLOTS; i++) modules.add(ItemStack.EMPTY);
                BackpackModules.setSlots(worn, modules);
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(ItemStack.EMPTY);
                contents.set(0, new ItemStack(item, 5));
                BackpackContents.write(worn, contents);
                ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
            });
            ctx.waitTicks(20);
            ctx.getInput().pressKey(NexiumEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "the Backpack's screen opened");
            int total = 5;
            for (int count : new int[]{1, 2, 3}) {
                for (int container : new int[]{3, 15}) {
                    world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).getInventory().setItem(container, new ItemStack(item, count)));
                    ctx.waitTicks(30); // the module watches the stack for a while first
                    ctx.runOnClient(mc -> {
                        var screen = (BackpackScreen) mc.gui.screen();
                        for (var slot : screen.getMenu().slots) {
                            if (slot.container == mc.player.getInventory() && slot.getContainerSlot() == container) {
                                mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
                            }
                        }
                    });
                    ctx.waitTicks(40);
                    total += count;
                    int[] state = new int[2];
                    world.getServer().runOnServer(server -> {
                        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                        state[0] = player.getInventory().getItem(container).getCount();
                        int stored = 0;
                        if (player.containerMenu instanceof net.nexium.essentials.backpack.BackpackMenu menu) {
                            for (var slot : menu.slots) if (slot.container != player.getInventory() && slot.getItem().is(item)) stored += slot.getItem().getCount();
                        }
                        state[1] = stored;
                    });
                    System.out.println("[nexus] shift-click " + count + " from slot " + container + ": " + state[0] + " back in the hotbar, " + state[1] + " in the Backpack screen");
                    check(state[0] == 0, "the " + count + " Fluid Intake(s) shift-clicked from slot " + container + " stay in the Backpack, " + state[0] + " came back");
                    check(state[1] == total, "the Backpack holds " + total + ", it holds " + state[1]);
                }
            }
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
