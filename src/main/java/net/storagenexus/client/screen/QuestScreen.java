package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.quest.QuestBook;
import net.storagenexus.quest.QuestBook.Chapter;
import net.storagenexus.quest.QuestBook.Quest;
import net.storagenexus.quest.QuestRequestPayload;
import net.storagenexus.quest.QuestStatePayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The quest book: the chapters down the left (a lock on those not open yet, with how many quests are done), and the quests of
 * the chosen chapter on the right, each with its item, what to do, a progress bar, and a tick when done. A chapter opens once
 * every quest of the one before it is done. The mouse wheel scrolls a long chapter. Progress comes from the server once a second.
 */
public class QuestScreen extends Screen {
    private static final int PANEL_W = 400, SIDE = 112, ROW = 40, PAD = 8;
    private static final int GREEN = 0xFF55D86B, GOLD = 0xFFFFBE3C, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98, PANEL = 0xE0101016, EDGE = 0xFF50505F, BAR_BG = 0xFF1A1A22;

    private static int lastChapter;

    private final Map<String, QuestStatePayload.Entry> state = new HashMap<>();
    private int chapter, scroll, left, top, panelH, rows, sinceRequest;
    private final java.util.List<Button> chapterButtons = new java.util.ArrayList<>();

    public QuestScreen(QuestStatePayload data) {
        super(Component.translatable("screen.nexium.quests"));
        chapter = Mth.clamp(lastChapter, 0, Math.max(0, QuestBook.chapters().size() - 1));
        update(data);
    }

    public void update(QuestStatePayload data) {
        state.clear();
        for (QuestStatePayload.Entry e : data.entries()) state.put(e.key(), e);
        refresh();
    }

    // ---- state for tests ---------------------------------------------------------------------------------------------

    public int chapterIndex() {
        return chapter;
    }

    public void showChapter(int index) {
        if (index < 0 || index >= QuestBook.chapters().size()) return;
        chapter = index;
        lastChapter = index;
        scroll = 0;
        refresh();
    }

    public boolean isOpen(int index) {
        if (index <= 0) return true;
        return QuestBook.chapters().get(index - 1).quests().stream().allMatch(q -> done(q));
    }

    public boolean done(Quest q) {
        QuestStatePayload.Entry e = state.get(q.key());
        return e != null && e.done();
    }

    public int current(Quest q) {
        QuestStatePayload.Entry e = state.get(q.key());
        return e == null ? 0 : e.current();
    }

    @Override
    protected void init() {
        rows = Mth.clamp((height - 8 - 36 - 28) / ROW, 2, 6);
        panelH = 36 + rows * ROW + 28;
        left = (width - PANEL_W) / 2;
        top = Math.max(4, (height - panelH) / 2);
        chapterButtons.clear();
        List<Chapter> chapters = QuestBook.chapters();
        for (int i = 0; i < chapters.size(); i++) {
            final int index = i;
            chapterButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> showChapter(index))
                    .bounds(left + PAD, top + 30 + i * 22, SIDE, 20).build()));
        }
        refresh();
    }

    private void refresh() {
        List<Chapter> chapters = QuestBook.chapters();
        for (int i = 0; i < chapterButtons.size() && i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            long done = c.quests().stream().filter(this::done).count();
            boolean open = isOpen(i);
            Button b = chapterButtons.get(i);
            b.active = open && i != chapter;
            Component name = Component.translatable(c.nameKey());
            b.setMessage(open ? Component.literal(name.getString() + "  " + done + "/" + c.quests().size())
                    : Component.literal(name.getString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        int total = QuestBook.chapters().get(chapter).quests().size();
        scroll = Mth.clamp(scroll, 0, Math.max(0, total - rows));
    }

    @Override
    public void tick() {
        if (++sinceRequest >= 20) {
            sinceRequest = 0;
            ClientPlayNetworking.send(new QuestRequestPayload(false));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int total = QuestBook.chapters().get(chapter).quests().size();
        if (total > rows) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, total - rows);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        refresh();
        return handled;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + PANEL_W, top + panelH, EDGE);
        g.fill(left + 1, top + 1, left + PANEL_W - 1, top + panelH - 1, PANEL);
        g.fill(left + PAD + SIDE + 4, top + 26, left + PAD + SIDE + 5, top + panelH - 8, EDGE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + PAD, top + 10, GOLD, false);
        List<Chapter> chapters = QuestBook.chapters();
        if (chapters.isEmpty()) return;
        for (int i = 0; i < chapters.size(); i++) g.item(new ItemStack(chapters.get(i).icon()), left + PAD + SIDE - 18, top + 32 + i * 22);

        Chapter c = chapters.get(chapter);
        int x = left + PAD + SIDE + 12, w = PANEL_W - PAD - SIDE - 12 - PAD - 8;
        g.text(font, Component.translatable(c.nameKey()).withStyle(ChatFormatting.BOLD), x, top + 10, TEXT, false);
        long done = c.quests().stream().filter(this::done).count();
        Component count = Component.translatable("screen.nexium.quests.progress", done, c.quests().size());
        g.text(font, count, left + PANEL_W - PAD - 8 - font.width(count), top + 10, done == c.quests().size() ? GREEN : DIM, false);
        g.text(font, font.plainSubstrByWidth(Component.translatable(c.descKey()).getString(), w), x, top + 22, DIM, false);

        for (int i = 0; i < rows && scroll + i < c.quests().size(); i++) {
            Quest q = c.quests().get(scroll + i);
            int y = top + 36 + i * ROW;
            boolean isDone = done(q);
            g.fill(x - 2, y, x + w + 2, y + ROW - 4, isDone ? 0x40305A38 : 0x30FFFFFF);
            g.item(new ItemStack(q.item()), x + 2, y + 4);
            g.text(font, Component.translatable(q.nameKey()).withStyle(isDone ? ChatFormatting.GREEN : ChatFormatting.WHITE), x + 24, y + 3, TEXT, false);
            List<FormattedCharSequence> desc = font.split(Component.translatable(q.descKey()), w - 28);
            for (int l = 0; l < desc.size() && l < 2; l++) g.text(font, desc.get(l), x + 24, y + 13 + l * 9, DIM, false);
            // Progress bar and count
            int barX = x + 24, barY = y + ROW - 9, barW = w - 70;
            int cur = isDone ? q.count() : current(q);
            g.fill(barX, barY, barX + barW, barY + 3, BAR_BG);
            g.fill(barX, barY, barX + barW * cur / Math.max(1, q.count()), barY + 3, isDone ? GREEN : 0xFF3FB8FF);
            String label = isDone ? Component.translatable("screen.nexium.quests.done").getString() : cur + " / " + q.count();
            g.text(font, label, x + w - font.width(label) - 2, barY - 3, isDone ? GREEN : DIM, false);
        }
        if (c.quests().size() > rows) {
            int trackH = rows * ROW, thumb = Math.max(10, trackH * rows / c.quests().size());
            int thumbY = top + 36 + (trackH - thumb) * scroll / Math.max(1, c.quests().size() - rows);
            g.fill(left + PANEL_W - 8, top + 36, left + PANEL_W - 5, top + 36 + trackH, 0xFF2A2A36);
            g.fill(left + PANEL_W - 8, thumbY, left + PANEL_W - 5, thumbY + thumb, 0xFF8C8CAA);
        }
        // Footer: the reward, and that nothing is locked but later chapters
        g.text(font, Component.translatable("screen.nexium.quests.reward", c.tier()), x, top + panelH - 22, GOLD, false);
        g.text(font, font.plainSubstrByWidth(Component.translatable("screen.nexium.quests.hint").getString(), PANEL_W - PAD * 2), left + PAD, top + panelH - 12, DIM, false);
        // A locked chapter's button explains itself on hover
        for (int i = 0; i < chapterButtons.size(); i++) {
            Button b = chapterButtons.get(i);
            if (!isOpen(i) && b.isHovered() && i > 0) {
                g.setTooltipForNextFrame(font, Component.translatable("screen.nexium.quests.locked", Component.translatable(chapters.get(i - 1).nameKey())), mouseX, mouseY);
            }
        }
    }
}
