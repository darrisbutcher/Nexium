package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.client.guide.CodexScreen;
import net.storagenexus.client.guide.GuideScreens;
import net.storagenexus.guide.GuideBookItem;
import net.storagenexus.guide.GuideData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nexium Codex on a real client: the guide item opens it, every topic lays out without a word sticking out of the page,
 * a link clicked with the mouse opens its topic, back and forward work, search finds topics, and the lookup key over an item
 * in the inventory opens that item's page (Escape comes back to the inventory). Takes screenshots along the way.
 */
public class GuideClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(10);
            GuideData data = ctx.computeOnClient(mc -> GuideScreens.load(mc.getResourceManager()));

            // The guide item opens the Codex on its first topic
            ctx.runOnClient(mc -> GuideBookItem.openScreen.run());
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof CodexScreen), "the guide item opens the Codex");
            check(data.home().equals(codex(ctx, CodexScreen::topic)), "it opens on the first topic");
            System.out.println("[nexus] codex home: " + ctx.takeScreenshot("nexus_codex_home"));

            // Every topic lays out inside the page, and has something on it
            List<String> problems = new ArrayList<>();
            for (String id : data.topics().keySet()) {
                ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).open(id));
                boolean fits = codex(ctx, CodexScreen::fitsWidth);
                int height = codex(ctx, CodexScreen::contentHeight);
                if (!fits || height < 60) problems.add(id + (fits ? " is empty" : " has a word past the right edge"));
                String title = codex(ctx, s -> s.title(id));
                if (title == null || title.isBlank() || title.contains("Format error")) problems.add(id + " has a broken title: " + title);
            }
            check(problems.isEmpty(), "every topic fits the page: " + problems);

            // A link in the text, clicked with the mouse, opens its topic; back and forward walk the history
            ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).open("area"));
            ctx.waitTicks(3);
            List<String> links = codex(ctx, CodexScreen::links);
            check(links.contains("aiot") && links.contains("tunnels"), "the 3x3 Mining page links to the AIOT and Tunnels pages, got " + links);
            System.out.println("[nexus] codex topic: " + ctx.takeScreenshot("nexus_codex_topic"));
            clickAt(ctx, codex(ctx, s -> s.linkSpot("aiot")));
            check("aiot".equals(codex(ctx, CodexScreen::topic)), "clicking the AIOT link opens that page");
            ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).back());
            check("area".equals(codex(ctx, CodexScreen::topic)), "back returns to 3x3 Mining");
            ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).forward());
            check("aiot".equals(codex(ctx, CodexScreen::topic)), "forward goes to the AIOT page again");

            // A page with recipe cards
            ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).open("barrels"));
            ctx.waitTicks(3);
            System.out.println("[nexus] codex recipes: " + ctx.takeScreenshot("nexus_codex_recipes"));

            // Search lists matching topics, best (title) matches first
            ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).setSearch("freezer"));
            ctx.waitTicks(3);
            List<String> found = codex(ctx, CodexScreen::sidebarTopics);
            check(!found.isEmpty() && found.getFirst().equals("freezer"), "searching 'freezer' finds the Freezer first, got " + found);
            System.out.println("[nexus] codex search: " + ctx.takeScreenshot("nexus_codex_search"));
            // A right-click on the search box clears it
            double[] box = ctx.computeOnClient(mc -> {
                int[] b = ((CodexScreen) mc.gui.screen()).searchBounds();
                double scale = mc.getWindow().getGuiScale();
                return new double[]{(b[0] + b[2] / 2.0) * scale, (b[1] + b[3] / 2.0) * scale};
            });
            ctx.getInput().setCursorPos(box[0], box[1]);
            ctx.waitTicks(2);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(3);
            check(codex(ctx, CodexScreen::searchText).isEmpty(), "a right-click on the search box clears it");
            ctx.runOnClient(mc -> ((CodexScreen) mc.gui.screen()).setSearch(""));
            ctx.setScreen(() -> null);
            ctx.waitTicks(3);

            // Items find their pages (vanilla items don't)
            check("freezer".equals(ctx.computeOnClient(mc -> GuideScreens.topicFor(stack("storagenexus:freezer")))), "the Freezer's page");
            check("drawers".equals(ctx.computeOnClient(mc -> GuideScreens.topicFor(stack("storagenexus:oak_drawer_1x1")))), "a drawer's page");
            check("aiot".equals(ctx.computeOnClient(mc -> GuideScreens.topicFor(stack("storagenexus:diamond_aiot")))), "an AIOT's page");
            check(ctx.computeOnClient(mc -> GuideScreens.topicFor(new ItemStack(Items.DIAMOND))) == null, "a diamond has no page");

            // The lookup key over an item in the inventory opens its page; Escape comes back to the inventory
            world.getServer().runCommand("clear @a");
            world.getServer().runCommand("give @a storagenexus:freezer");
            ctx.waitTicks(5);
            ctx.setScreen(() -> new InventoryScreen(ctx.computeOnClient(mc -> mc.player)));
            ctx.waitTicks(5);
            double[] slot = ctx.computeOnClient(mc -> {
                InventoryScreen screen = (InventoryScreen) mc.gui.screen();
                Slot hotbar = screen.getMenu().slots.get(36);
                int left = (screen.width - 176) / 2, top = (screen.height - 166) / 2;
                return new double[]{left + hotbar.x + 8, top + hotbar.y + 8};
            });
            moveTo(ctx, slot);
            ctx.getInput().pressKey(InputConstants.KEY_H);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof CodexScreen), "H over the Freezer opens the Codex");
            check("freezer".equals(codex(ctx, CodexScreen::topic)), "on the Freezer's page");
            System.out.println("[nexus] codex lookup: " + ctx.takeScreenshot("nexus_codex_lookup"));
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof InventoryScreen), "Escape comes back to the inventory");
            ctx.setScreen(() -> null);
        }
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    private static <T> T codex(ClientGameTestContext ctx, java.util.function.Function<CodexScreen, T> get) {
        return ctx.computeOnClient(mc -> get.apply((CodexScreen) mc.gui.screen()));
    }

    private static void moveTo(ClientGameTestContext ctx, double[] guiSpot) {
        check(guiSpot != null, "the spot to point at exists");
        double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(guiSpot[0] * scale, guiSpot[1] * scale);
        ctx.waitTicks(3);
    }

    private static void clickAt(ClientGameTestContext ctx, double[] guiSpot) {
        moveTo(ctx, guiSpot);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(4);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
