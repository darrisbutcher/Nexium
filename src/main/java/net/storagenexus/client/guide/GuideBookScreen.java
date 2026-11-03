package net.storagenexus.client.guide;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.storagenexus.guide.GuideData;

import java.util.ArrayList;
import java.util.List;

/**
 * One chapter of the guide, in the vanilla book screen. A chapter's contents page links to its other pages, and its recipe
 * pages are drawn over a blank page. Closing the book goes back to the chapter picker.
 */
public class GuideBookScreen extends BookViewScreen {
    private static final TextColor LINK_COLOR = TextColor.fromRgb(0x1A2896);

    private final GuideData data;
    private final GuideData.Chapter chapter;
    private final Screen parent;

    public GuideBookScreen(GuideData data, GuideData.Chapter chapter, Screen parent) {
        super(new BookAccess(pageComponents(chapter)));
        this.data = data;
        this.chapter = chapter;
        this.parent = parent;
    }

    /**
     * The text of every page. A page's first line is its heading, in bold; styling is done here rather than with section
     * signs, because a reset in the text would also drop the book's black, shadowless text style.
     */
    public static List<Component> pageComponents(GuideData.Chapter chapter) {
        Language language = Language.getInstance();
        List<Component> pages = new ArrayList<>();
        for (GuideData.Page page : chapter.pages()) {
            pages.add(switch (page) {
                case GuideData.TextPage text -> {
                    String full = language.getOrDefault(chapter.pageKey(text.id()));
                    int split = full.indexOf('\n');
                    yield heading(split < 0 ? full : full.substring(0, split), split < 0 ? "" : full.substring(split));
                }
                case GuideData.ContentsPage contents -> contentsPage(chapter, contents, language);
                case GuideData.RecipeSheet sheet -> Component.empty();
            });
        }
        return pages;
    }

    private static MutableComponent heading(String heading, String body) {
        return Component.literal(heading).withStyle(ChatFormatting.BOLD)
                .append(Component.literal(body).withStyle(style -> style.withBold(false)));
    }

    private static MutableComponent contentsPage(GuideData.Chapter chapter, GuideData.ContentsPage contents, Language language) {
        MutableComponent page = Component.literal(language.getOrDefault(chapter.titleKey())).withStyle(ChatFormatting.BOLD);
        for (String target : contents.targets()) {
            // Book pages are numbered from 1
            Style link = Style.EMPTY.withBold(false).withColor(LINK_COLOR).withUnderlined(true)
                    .withClickEvent(new ClickEvent.ChangePage(chapter.indexOf(target) + 1));
            page.append(Component.literal("\n"));
            page.append(Component.literal(language.getOrDefault(chapter.contentsKey(target))).withStyle(link));
        }
        return page;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (chapter.pages().get(currentPage) instanceof GuideData.RecipeSheet sheet) {
            RecipePainter.draw(g, font, data, sheet, (width - 192) / 2, mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
