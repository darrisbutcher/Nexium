package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.client.screen.BackpackScreen;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The Backpack's Crafting module and trash slot with the real mouse: the crafting panel shows only with the module installed and on,
 * items put in the grid by hand give a result, taking the result crafts and the grid refills from the Backpack, shift-click on the
 * result crafts all there is, the trash slot keeps the last item put in it (and a second replaces the first), and with the module
 * out the panel is gone but the trash stays. Takes screenshots of each.
 */
public class BackpackCraftingClientPart {
    /** Runs in the world the group's test made (see the All* test); the window is resized and the world is made there. */
    void part(ClientGameTestContext ctx, TestSingleplayerContext world) {
        {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ExtraSlots.of(player).clearContent();
                player.getFoodData().setFoodLevel(20);
                ItemStack worn = new ItemStack(ModItems.BACKPACK);
                List<ItemStack> modules = new ArrayList<>();
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING)));
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.CRAFTING)));
                for (int i = modules.size(); i < BackpackModules.SLOTS; i++) modules.add(ItemStack.EMPTY);
                BackpackModules.setSlots(worn, modules);
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.OAK_PLANKS, 14));
                contents.set(1, new ItemStack(Items.COBBLESTONE, 64));
                contents.set(2, new ItemStack(Items.IRON_INGOT, 20));
                contents.set(3, new ItemStack(Items.BREAD, 8));
                BackpackContents.write(worn, contents);
                ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
            });
            ctx.waitTicks(20);
            ctx.getInput().pressKey(NexiumEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "B opens the Backpack's screen");
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).craftingShown()), "with a Crafting module on, the crafting panel shows");
            RealMouse.moveTo(ctx, 4, 4); // out of the way, so no hover hint covers the panel
            System.out.println("[nexus] backpack crafting panel: " + shot(ctx, "nexus_backpack_crafting"));

            // The panel's close button hides the grid (the screen goes back to its plain width), a small button brings it back
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).hideCraftingCentre()));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).craftingShown()), "the close button hides the crafting panel");
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).showCraftingCentre() != null), "and a button to show it again appears");
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().craftingModuleOn()), "the Crafting module is still on");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] backpack crafting hidden: " + shot(ctx, "nexus_backpack_crafting_hidden"));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).showCraftingCentre()));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).craftingShown()), "the small button shows the grid again");
            RealMouse.moveTo(ctx, 4, 4);

            // The recipe book's button is under the grid. With room for the book beside the screen (a 1920x1080 window at GUI scale 2 is
            // 960 wide), a real click opens it and the screen slides right to make room
            ctx.getInput().resizeWindow(1920, 1080);
            ctx.runOnClient(mc -> { mc.options.guiScale().set(2); mc.resizeGui(); });
            ctx.waitTicks(10);
            int leftBefore = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).slotCentre(((BackpackScreen) mc.gui.screen()).getMenu().getSlot(0))[0]);
            int[] bookButton = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookButtonCentre());
            check(bookButton != null, "with a Crafting module on there is a recipe book button");
            RealMouse.leftClick(ctx, bookButton);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookVisible()), "clicking it opens the recipe book");
            int leftAfter = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).slotCentre(((BackpackScreen) mc.gui.screen()).getMenu().getSlot(0))[0]);
            check(leftAfter > leftBefore, "and the screen slides right to make room for it: " + leftBefore + " -> " + leftAfter);
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] backpack recipe book: " + shot(ctx, "nexus_backpack_recipe_book"));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookButtonCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).recipeBookVisible()), "clicking it again closes the book");
            // Without room (GUI scale 4 is 480 wide here, and the book, the screen and the crafting panel need more) the open book covers the screen
            ctx.runOnClient(mc -> { mc.options.guiScale().set(4); mc.resizeGui(); });
            ctx.waitTicks(10);
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookButtonCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookVisible()), "the book opens on a narrow screen too");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] backpack recipe book, narrow screen: " + shot(ctx, "nexus_backpack_recipe_book_narrow"));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookButtonCentre() == null ? new int[]{4, 4}
                    : ((BackpackScreen) mc.gui.screen()).recipeBookButtonCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).recipeBookVisible()), "and its button, drawn on top, closes it again");
            // Back to the usual window for the rest
            ctx.getInput().resizeWindow(1280, 720);
            ctx.runOnClient(mc -> { mc.options.guiScale().set(0); mc.resizeGui(); });
            ctx.waitTicks(10);

            // Pick the planks up from the Backpack, put one in each of two grid slots with right clicks, put the rest back
            RealMouse.leftClick(ctx, at(ctx, menu -> menu.getSlot(BackpackModules.SLOTS)));
            RealMouse.rightClick(ctx, at(ctx, menu -> menu.getInputGridSlots().get(0)));
            RealMouse.rightClick(ctx, at(ctx, menu -> menu.getInputGridSlots().get(3)));
            RealMouse.leftClick(ctx, at(ctx, menu -> menu.getSlot(BackpackModules.SLOTS)));
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().getResultSlot().getItem().is(Items.STICK)
                    && ((BackpackScreen) mc.gui.screen()).getMenu().getResultSlot().getItem().getCount() == 4), "two planks, one above the other, show 4 sticks in the result slot");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] backpack crafting, sticks ready: " + shot(ctx, "nexus_backpack_crafting_result"));

            // Take the result: the grid refills from the Backpack
            RealMouse.leftClick(ctx, at(ctx, menu -> menu.getResultSlot()));
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().getCarried().is(Items.STICK)), "clicking the result takes the sticks");
            check(held(world, Items.OAK_PLANKS) == 10, "the 12 planks put back in the Backpack lost two to the grid, which refilled after the craft: " + held(world, Items.OAK_PLANKS));
            // The empty trash slot says what it is when the mouse rests on it (the sticks are in the hand, so put no hint on the cursor yet)
            // Drop the sticks in the trash
            RealMouse.leftClick(ctx, at(ctx, menu -> menu.trashSlot()));
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().trashSlot().getItem().is(Items.STICK)), "sticks put in the trash stay in it");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] backpack trash holding sticks: " + shot(ctx, "nexus_backpack_trash_holding"));

            // Shift-click on the result. The harness's simulated mouse event carries no Shift modifier, so the click packet a shift-click
            // sends (a quick-move click on that slot) is sent directly; the screen turns a real shift-click into exactly this.
            int resultIndex = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().getResultSlot().index);
            ctx.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, resultIndex, 0,
                    net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player));
            ctx.waitTicks(10);
            int sticks = world.getServer().computeOnServer(server -> {
                int n = 0;
                var inv = server.getPlayerList().getPlayers().get(0).getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(Items.STICK)) n += inv.getItem(i).getCount();
                return n;
            });
            check(sticks == 24, "shift-click crafted all the planks (2 in the grid, 10 in the Backpack) into 24 sticks: " + sticks);
            check(held(world, Items.OAK_PLANKS) == 0, "the Backpack has no planks left");

            // The module taken out: the panel goes, the trash stays
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                if (player.containerMenu instanceof BackpackMenu menu) menu.getSlot(1).set(ItemStack.EMPTY);
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).craftingShown()), "without the Crafting module the panel is gone");
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).recipeBookButtonCentre() == null), "and so is the recipe book's button");
            check(ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().trashSlot().isActive()), "but the trash slot is still there");
            // Take the sticks out of the trash (a shift-click gives them back), then rest the mouse on the empty slot for its hint
            int trashIndex = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().trashSlot().index);
            ctx.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, trashIndex, 0,
                    net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> !((BackpackScreen) mc.gui.screen()).getMenu().trashSlot().hasItem()), "a shift-click on the trash gives its item back");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] backpack without the crafting module: " + shot(ctx, "nexus_backpack_crafting_none"));
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).slotCentre(((BackpackScreen) mc.gui.screen()).getMenu().trashSlot())));
            ctx.waitTicks(10);
            System.out.println("[nexus] the empty trash slot's hint: " + shot(ctx, "nexus_backpack_trash_hint"));
            ctx.setScreen(() -> null);
        }
    }

    private static int[] at(ClientGameTestContext ctx, Function<BackpackMenu, Slot> slot) {
        return ctx.computeOnClient(mc -> {
            BackpackScreen screen = (BackpackScreen) mc.gui.screen();
            return screen.slotCentre(slot.apply(screen.getMenu()));
        });
    }

    private static int held(TestSingleplayerContext world, net.minecraft.world.item.Item item) {
        return world.getServer().computeOnServer(server ->
                BackpackContents.count(ExtraSlots.of(server.getPlayerList().getPlayers().get(0)).backpack(), item));
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
