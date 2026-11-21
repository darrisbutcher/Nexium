package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.nexium.client.screen.FilterScreen;
import net.nexium.client.screen.TagPickerScreen;
import net.nexium.filter.FilterData;
import net.nexium.filter.FilterMenu;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.util.Menus;

import java.util.List;

/**
 * The Advanced Filter's editor in a real game: a Match button that steps through items and tags, smeltable items and furnace fuel, a
 * Tags button that opens the tag picker, where tags are searched for and chosen with the mouse and the filter in hand keeps what was
 * chosen. A Basic Filter's editor is as it was. Takes screenshots of each.
 */
public class FilterTagsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            ctx.waitTicks(20);

            // A Basic Filter: its editor is as it was, with no Match or Tags button
            open(world, ctx, ModItems.BASIC_FILTER);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FilterScreen s && !s.menuAdvanced()), "a Basic Filter's editor is the plain one");
            System.out.println("[nexus] basic filter: " + shot(ctx, "nexus_filter_basic"));
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
            ctx.waitTicks(5);

            // An Advanced Filter
            open(world, ctx, ModItems.ADVANCED_FILTER);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FilterScreen s && s.menuAdvanced()), "an Advanced Filter's editor has the extra buttons");
            System.out.println("[nexus] advanced filter: " + shot(ctx, "nexus_filter_advanced"));

            // The Match button steps through the special matches, and the filter in hand follows (each a real click on the button)
            clickMatch(ctx);
            ctx.waitTicks(5);
            check(held(world).special() == FilterData.Special.SMELTABLE, "Match goes to smeltable: " + held(world).special());
            clickMatch(ctx);
            ctx.waitTicks(5);
            check(held(world).special() == FilterData.Special.FUEL, "then to furnace fuel: " + held(world).special());
            clickMatch(ctx);
            clickMatch(ctx);
            ctx.waitTicks(5);
            check(held(world).special() == FilterData.Special.SMELTABLE, "and round again to smeltable: " + held(world).special());
            System.out.println("[nexus] advanced filter, smeltable: " + shot(ctx, "nexus_filter_advanced_smeltable"));

            // The tag picker, opened with a real click on the Tags button
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((FilterScreen) mc.gui.screen()).tagsCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof TagPickerScreen), "the Tags button opens the tag picker");
            check(world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).containerMenu instanceof FilterMenu),
                    "the filter's menu stays open behind the picker");
            check(ctx.computeOnClient(mc -> ((TagPickerScreen) mc.gui.screen()).shown().size()) > 50, "it lists plenty of tags");
            System.out.println("[nexus] tag picker, all tags: " + shot(ctx, "nexus_tag_picker"));

            // Search for a tag and choose it with a click; then another
            choose(ctx, "minecraft:logs", "minecraft:logs");
            choose(ctx, "minecraft:planks", "minecraft:planks");
            check(ctx.computeOnClient(mc -> ((TagPickerScreen) mc.gui.screen()).selected()).size() == 2, "two tags are chosen");
            List<Identifier> saved = held(world).tags();
            check(saved.contains(Identifier.parse("minecraft:logs")) && saved.contains(Identifier.parse("minecraft:planks")),
                    "the filter in hand holds both: " + saved);
            ctx.runOnClient(mc -> ((TagPickerScreen) mc.gui.screen()).setSearch("minecraft:planks"));
            ctx.waitTicks(3);
            System.out.println("[nexus] tag picker, chosen: " + shot(ctx, "nexus_tag_picker_chosen"));

            // Clicking a chosen tag takes it off again
            int row = indexOf(ctx, "minecraft:planks");
            click(ctx, row);
            check(held(world).tags().equals(List.of(Identifier.parse("minecraft:logs"))), "a second click takes the tag off: " + held(world).tags());
            click(ctx, row);

            // Done goes back to the filter, which shows how many tags are chosen and is still open on the server
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((TagPickerScreen) mc.gui.screen()).doneCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FilterScreen), "Done goes back to the filter");
            check(ctx.computeOnClient(mc -> ((FilterScreen) mc.gui.screen()).tagCount()) == 2, "which shows the two chosen tags");
            check(world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).containerMenu instanceof FilterMenu),
                    "and its menu is still open");
            System.out.println("[nexus] advanced filter with tags: " + shot(ctx, "nexus_filter_advanced_tags"));
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
        }
    }

    /** Puts a filter in the player's hand and opens its editor, as a right-click does. */
    private static void open(TestSingleplayerContext world, ClientGameTestContext ctx, net.minecraft.world.item.Item filter) {
        // The filter is in the hand before its editor opens, as it is when a player right-clicks with it (the client reads the hand to build the menu)
        world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(filter)));
        ctx.waitTicks(10);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            Menus.open(player, stack.getHoverName(), InteractionHand.MAIN_HAND.ordinal(), (id, inv, p) -> new FilterMenu(id, inv, InteractionHand.MAIN_HAND.ordinal()));
        });
        ctx.waitFor(mc -> mc.gui.screen() instanceof FilterScreen);
        ctx.waitTicks(10);
    }

    private static FilterData held(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> {
            FilterData d = server.getPlayerList().getPlayers().get(0).getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER);
            return d == null ? FilterData.empty(true) : d;
        });
    }

    /** Types the search and clicks the row of that tag. */
    private static void choose(ClientGameTestContext ctx, String search, String tag) {
        ctx.runOnClient(mc -> ((TagPickerScreen) mc.gui.screen()).setSearch(search));
        ctx.waitTicks(3);
        int row = indexOf(ctx, tag);
        check(row >= 0, "the search for '" + search + "' lists " + tag);
        click(ctx, row);
    }

    private static int indexOf(ClientGameTestContext ctx, String tag) {
        return ctx.computeOnClient(mc -> {
            var shown = ((TagPickerScreen) mc.gui.screen()).shown();
            for (int i = 0; i < shown.size(); i++) if (shown.get(i).id().toString().equals(tag)) return i;
            return -1;
        });
    }

    /** A real left click (cursor moved there, left button pressed) in the middle of that row. */
    private static void click(ClientGameTestContext ctx, int row) {
        RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((TagPickerScreen) mc.gui.screen()).rowCentre(row)));
        ctx.waitTicks(2);
    }

    /** A real left click on the Match button. */
    private static void clickMatch(ClientGameTestContext ctx) {
        RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((FilterScreen) mc.gui.screen()).matchCentre()));
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
