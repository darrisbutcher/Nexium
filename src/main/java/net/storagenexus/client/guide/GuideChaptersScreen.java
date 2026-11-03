package net.storagenexus.client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.storagenexus.guide.GuideData;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide's main screen: one card per chapter, each with its cover. Hovering a card says what is in the chapter, and
 * clicking it opens the chapter as a book; closing the book comes back here.
 */
public class GuideChaptersScreen extends Screen {
    private static final int COLUMNS = 3, CARD_WIDTH = 94, GAP = 3, COVER = 32;
    private static final int CARD_HEIGHT = 38, HEADER_HEIGHT = 34, DETAIL_HEIGHT = 30;

    private final GuideData data;
    private final List<ChapterCard> cards = new ArrayList<>();
    private int titleTop, detailTop;

    public GuideChaptersScreen(GuideData data) {
        super(Component.translatable("screen.storagenexus.guide"));
        this.data = data;
    }

    @Override
    protected void init() {
        cards.clear();
        int rows = (data.chapters().size() + COLUMNS - 1) / COLUMNS;
        int gridWidth = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GAP;
        int gridHeight = rows * CARD_HEIGHT + (rows - 1) * GAP;
        // The title, the cards, the detail box and the Done button, centred as one block (the game keeps the window
        // at least 240 tall in GUI pixels, which is room enough)
        int total = HEADER_HEIGHT + gridHeight + 4 + DETAIL_HEIGHT + 5 + 20;
        int top = Math.max(2, (height - total) / 2);
        int left = (width - gridWidth) / 2;
        int gridTop = top + HEADER_HEIGHT;
        for (int i = 0; i < data.chapters().size(); i++) {
            GuideData.Chapter chapter = data.chapters().get(i);
            int x = left + (i % COLUMNS) * (CARD_WIDTH + GAP);
            int y = gridTop + (i / COLUMNS) * (CARD_HEIGHT + GAP);
            cards.add(addRenderableWidget(new ChapterCard(x, y, CARD_WIDTH, CARD_HEIGHT, chapter,
                    button -> minecraft.gui.setScreen(new GuideBookScreen(data, chapter, this)))));
        }
        detailTop = gridTop + gridHeight + 4;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 98) / 2, detailTop + DETAIL_HEIGHT + 5, 98, 20).build());
        titleTop = top + 8;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title, (width - font.width(title)) / 2, titleTop, 0xFFFFFFFF, true);
        Component pick = Component.translatable("screen.storagenexus.guide.pick");
        g.text(font, pick, (width - font.width(pick)) / 2, titleTop + 11, 0xFFA0A0A0, true);

        // What the hovered (or keyboard-focused) chapter covers
        ChapterCard hovered = cards.stream().filter(ChapterCard::isHoveredOrFocused).findFirst().orElse(null);
        Component topics = hovered == null ? Component.translatable("screen.storagenexus.guide.hint")
                : Component.translatable(hovered.chapter.topicsKey());
        int boxWidth = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GAP;
        int boxLeft = (width - boxWidth) / 2;
        g.fill(boxLeft, detailTop, boxLeft + boxWidth, detailTop + DETAIL_HEIGHT, 0x96000000);
        List<FormattedCharSequence> lines = font.split(topics, boxWidth - 8);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            g.text(font, lines.get(i), boxLeft + 4, detailTop + 3 + i * 9, 0xFFBEBEBE, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A vanilla button with the chapter's cover and name drawn in it. */
    private static final class ChapterCard extends Button {
        private final GuideData.Chapter chapter;
        private final Identifier cover;

        ChapterCard(int x, int y, int width, int height, GuideData.Chapter chapter, OnPress onPress) {
            super(x, y, width, height, Component.translatable(chapter.titleKey()), onPress, DEFAULT_NARRATION);
            this.chapter = chapter;
            this.cover = Identifier.fromNamespaceAndPath("storagenexus", "guide/cover_" + chapter.cover());
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            extractDefaultSprite(g);
            Font font = Minecraft.getInstance().font;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, cover, getX() + 4, getY() + (getHeight() - COVER) / 2, COVER, COVER);
            List<FormattedCharSequence> lines = font.split(getMessage(), getWidth() - 4 - COVER - 4 - 3);
            int y = getY() + (getHeight() - lines.size() * 9) / 2 + 1;
            int color = isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF;
            for (FormattedCharSequence line : lines) {
                g.text(font, line, getX() + 4 + COVER + 4, y, color, true);
                y += 9;
            }
        }
    }
}
