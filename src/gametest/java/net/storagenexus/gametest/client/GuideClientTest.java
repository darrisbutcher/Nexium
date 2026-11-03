package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.storagenexus.client.guide.GuideBookScreen;
import net.storagenexus.client.guide.GuideChaptersScreen;
import net.storagenexus.client.guide.GuideScreens;
import net.storagenexus.guide.GuideBookItem;
import net.storagenexus.guide.GuideData;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nexium Guide on a real client: the item opens the chapter picker, a card opens its chapter as a book, closing the book
 * comes back to the picker, and every page of every chapter fits the book's 14 lines of 114 pixels. Takes screenshots of the
 * picker, text pages, a contents page and recipe cards.
 */
public class GuideClientTest implements FabricClientGameTest {
    private static final int PAGE_LINES = 14, PAGE_WIDTH = 114;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(10);
            GuideData data = ctx.computeOnClient(mc -> GuideScreens.load(mc.getResourceManager()));

            // The item opens the picker, and hovering a card shows what its chapter covers
            ctx.runOnClient(mc -> GuideBookItem.openScreen.run());
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof GuideChaptersScreen), "the guide item opens the chapter picker");
            System.out.println("[nexus] guide picker: " + ctx.takeScreenshot("nexus_guide_picker"));
            int tools = chapterIndex(data, "aiot");
            hover(ctx, tools);
            System.out.println("[nexus] guide picker hover: " + ctx.takeScreenshot("nexus_guide_picker_hover"));

            // Clicking a card opens that chapter; Escape in the book comes back to the picker
            click(ctx, chapterIndex(data, "drawers"));
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof GuideBookScreen), "a card opens its chapter as a book");
            System.out.println("[nexus] guide chapter opened: " + ctx.takeScreenshot("nexus_guide_chapter"));
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof GuideChaptersScreen), "closing a chapter goes back to the picker");
            ctx.setScreen(() -> null);

            // Every page fits the book
            List<String> tooLong = ctx.computeOnClient(mc -> {
                List<String> problems = new ArrayList<>();
                for (GuideData.Chapter chapter : data.chapters()) {
                    List<Component> pages = GuideBookScreen.pageComponents(chapter);
                    for (int i = 0; i < pages.size(); i++) {
                        int lines = mc.font.split(pages.get(i), PAGE_WIDTH).size();
                        if (lines > PAGE_LINES) problems.add(chapter.id() + " page " + (i + 1) + " takes " + lines + " lines");
                        if (chapter.pages().get(i) instanceof GuideData.ContentsPage contents) {
                            // The chapter's title heads the page (and may take two lines); every entry must stay on one
                            Component heading = Component.translatable(chapter.titleKey()).withStyle(ChatFormatting.BOLD);
                            int expected = mc.font.split(heading, PAGE_WIDTH).size() + contents.targets().size();
                            if (lines != expected) problems.add(chapter.id() + " contents wraps a line (" + lines + " lines, expected " + expected + ")");
                        }
                    }
                }
                return problems;
            });
            check(tooLong.isEmpty(), "every guide page fits the book: " + tooLong);

            // Pages worth looking at
            show(ctx, data, "start", "welcome", "nexus_guide_start");
            show(ctx, data, "drawers", "contents", "nexus_guide_drawers_contents");
            show(ctx, data, "drawers", "capacity", "nexus_guide_drawers_capacity");
            show(ctx, data, "drawers", "recipes", "nexus_guide_recipes_drawers");
            show(ctx, data, "network", "recipes2", "nexus_guide_recipes_network");
            show(ctx, data, "links", "recipes5", "nexus_guide_recipes_conduits");
            show(ctx, data, "aiot", "recipes", "nexus_guide_recipes_aiot");
            show(ctx, data, "aiot", "recipes2", "nexus_guide_recipes_smithing");
            show(ctx, data, "armor", "bonuses", "nexus_guide_armor_bonuses");
            show(ctx, data, "mobs", "recipe", "nexus_guide_iron_farm");
            ctx.setScreen(() -> null);
        }
    }

    private static int chapterIndex(GuideData data, String id) {
        for (int i = 0; i < data.chapters().size(); i++) if (data.chapters().get(i).id().equals(id)) return i;
        throw new AssertionError("No chapter " + id);
    }

    private static void show(ClientGameTestContext ctx, GuideData data, String chapterId, String pageId, String screenshot) {
        GuideData.Chapter chapter = data.chapter(chapterId);
        int index = chapter.indexOf(pageId);
        check(index >= 0, chapterId + " has a page " + pageId);
        ctx.setScreen(() -> new GuideBookScreen(data, chapter, null));
        ctx.waitTicks(2);
        // The page can only be turned once the screen has set up its page buttons
        ctx.runOnClient(mc -> ((GuideBookScreen) mc.gui.screen()).setPage(index));
        ctx.waitTicks(3);
        System.out.println("[nexus] " + screenshot + ": " + ctx.takeScreenshot(screenshot));
    }

    /** Moves the mouse onto the picker's card for this chapter. */
    private static void hover(ClientGameTestContext ctx, int card) {
        double[] centre = ctx.computeOnClient(mc -> {
            AbstractWidget widget = (AbstractWidget) mc.gui.screen().children().get(card);
            double scale = mc.getWindow().getGuiScale();
            return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
        });
        ctx.getInput().setCursorPos(centre[0], centre[1]);
        ctx.waitTicks(3);
    }

    private static void click(ClientGameTestContext ctx, int card) {
        hover(ctx, card);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(5);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
