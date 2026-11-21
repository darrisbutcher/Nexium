package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.client.screen.BackpackScreen;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackFeeding;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;

import java.util.ArrayList;
import java.util.List;

/**
 * The Feeding module's settings in the Backpack's screen, with the real mouse: the panel under the module column shows when a Feeding
 * module is installed, its level button lowers the level a click at a time (shift-click raises it), its waste button switches between
 * "No waste" and "May waste", the module in the Backpack on the server follows each click, and the panel is gone without the module.
 * Takes screenshots of the panel as it starts, as it is set, and with a hint showing.
 */
public class FeedingSettingClientTest implements FabricClientGameTest {
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
                player.getFoodData().setFoodLevel(20); // full: the module has nothing to do while the settings are tried
                ItemStack worn = new ItemStack(ModItems.BACKPACK);
                List<ItemStack> modules = new ArrayList<>();
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.AUTO_TORCH)));
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING)));
                modules.get(0).set(net.nexium.registry.ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
                for (int i = modules.size(); i < BackpackModules.SLOTS; i++) modules.add(ItemStack.EMPTY);
                BackpackModules.setSlots(worn, modules);
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.BREAD, 12));
                contents.set(1, new ItemStack(Items.COOKED_BEEF, 8));
                contents.set(2, new ItemStack(Items.APPLE, 6));
                BackpackContents.write(worn, contents);
                ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
            });
            ctx.waitTicks(20);
            ctx.getInput().pressKey(NexiumEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "B opens the Backpack's screen");
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).feedingPanelShown()), "with a Feeding module in it the Feeding panel shows");
            check(level(world) == 19 && !waste(world), "a new module is at 19 and never wastes");
            System.out.println("[nexus] feeding panel, as it starts: " + shot(ctx, "nexus_feeding_default"));

            // The level button, clicked with the real mouse: one step lower each click
            for (int expected : new int[]{18, 16, 14, 12}) {
                click(ctx, true);
                check(level(world) == expected, "a click lowers the level to " + expected + ": " + level(world));
            }
            // Shift-click goes the other way
            ctx.getInput().holdShift();
            click(ctx, true);
            ctx.getInput().releaseShift();
            check(level(world) == 14, "a shift-click raises it a step: " + level(world));
            // The screen shows what the module holds
            check(ctx.computeOnClient(mc -> BackpackFeeding.level(((BackpackScreen) mc.gui.screen()).getMenu().getSlot(1).getItem())) == 14, "and the screen's module slot shows the same");

            // The waste button
            click(ctx, false);
            check(waste(world), "the waste button lets it waste");
            System.out.println("[nexus] feeding panel, set: " + shot(ctx, "nexus_feeding_set"));
            // A hint beside the panel while the mouse rests on the level button
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).feedingLevelCentre()));
            ctx.waitTicks(10);
            System.out.println("[nexus] feeding level hint: " + shot(ctx, "nexus_feeding_level_hint"));
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).feedingWasteCentre()));
            ctx.waitTicks(10);
            System.out.println("[nexus] feeding waste hint: " + shot(ctx, "nexus_feeding_waste_hint"));
            click(ctx, false);
            check(!waste(world), "a second click on the waste button switches it off again");

            // Taking the module out hides the panel
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ItemStack backpack = ExtraSlots.of(player).backpack();
                if (player.containerMenu instanceof net.nexium.essentials.backpack.BackpackMenu menu) menu.getSlot(1).set(ItemStack.EMPTY);
                else BackpackModules.toggle(backpack, 1);
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).feedingPanelShown()), "without the Feeding module the panel is gone");
            System.out.println("[nexus] no feeding module: " + shot(ctx, "nexus_feeding_none"));
            ctx.setScreen(() -> null);
        }
    }

    /** One real left click on the level button (or the waste button): the cursor moved there, the button pressed. */
    private static void click(ClientGameTestContext ctx, boolean level) {
        RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> level ? ((BackpackScreen) mc.gui.screen()).feedingLevelCentre()
                : ((BackpackScreen) mc.gui.screen()).feedingWasteCentre()));
        ctx.waitTicks(5);
    }

    private static int level(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> BackpackFeeding.level(
                BackpackModules.find(ExtraSlots.of(server.getPlayerList().getPlayers().get(0)).backpack(), BackpackModuleType.FEEDING)));
    }

    private static boolean waste(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> BackpackFeeding.allowsWaste(
                BackpackModules.find(ExtraSlots.of(server.getPlayerList().getPlayers().get(0)).backpack(), BackpackModuleType.FEEDING)));
    }

    /** A screenshot with the recipe toasts cleared away, so they are not in the way. */
    private static java.nio.file.Path shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        ctx.waitTicks(2);
        return ctx.takeScreenshot(name);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
