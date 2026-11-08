package net.storagenexus.client.guide;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.guide.CodexText;
import net.storagenexus.guide.GuideData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Nexium Codex: a small built-in wiki. The sidebar lists every category and its topics (or search results), and the
 * page on the right shows one topic in full: no page breaks, it scrolls. Words that name another topic are links, recipes
 * are drawn as crafting grids (click an ingredient to open its page), and "See also" links to related topics. Back and
 * forward work like a browser, and the Codex reopens on the topic it last showed.
 */
public class CodexScreen extends Screen {
    private static final int PAD = 6, BAR = 22, ROW = 13, LINE = 10, NAME_WIDTH = 92, FOOTER = 12;
    private static final int PANEL = 0xD8101016, EDGE = 0xFF50505F, SIDE_BG = 0x90000000, RULE = 0xFF5A5A6E;
    private static final int GOLD = 0xFFFFBE3C, WHITE = 0xFFF0F0F0, TEXT = 0xFFE0E0E0, GREY = 0xFFA8A8A8, DIM = 0xFF7E7E7E;
    private static final int LINK = 0xFF55E6FF, LINK_HOVER = 0xFFFFFF55, SELECT = 0xE0464682, SELECT_EDGE = 0xFF9696E6, CHIP = 0xE0282840;

    /** The topic the Codex showed last, so opening it again comes back there. */
    private static String lastTopic;

    private final GuideData data;
    private final Screen parent;
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final Map<String, String> bodies = new LinkedHashMap<>();
    private final List<CodexText.Phrase> phrases;
    private final List<String> history = new ArrayList<>();
    private final Set<String> openCategories = new HashSet<>();
    private int historyIndex = -1;
    private String topicId;
    private String query = "";

    private EditBox search;
    private Button back, forward;
    private int sideLeft, sideRight, contentLeft, contentRight, areaTop, areaBottom;
    private double sideScroll, contentScroll;

    private final List<SideRow> sideRows = new ArrayList<>();
    private List<Piece> pieces = List.of();
    private int contentHeight, layoutWidth = -1;
    private String layoutTopic;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    /** One line of the sidebar: a category (which opens or closes), a topic, or a plain label. */
    private record SideRow(int depth, String category, String topic, String label, ItemStack icon) {}

    private enum Kind { WORD, HEADING, ICON, RULE_LINE, RECIPE, CHIP }

    /** Something drawn on the page, in page coordinates (x from the page's left, y from its top before scrolling). */
    private record Piece(Kind kind, int x, int y, int w, String text, int color, String link, ItemStack icon, GuideData.Recipe recipe) {}

    public CodexScreen(GuideData data, String topic, Screen parent) {
        super(Component.translatable("screen.storagenexus.codex"));
        this.data = data;
        this.parent = parent;
        List<CodexText.Phrase> all = new ArrayList<>();
        for (GuideData.Topic t : data.topics().values()) {
            StringBuilder body = new StringBuilder();
            for (String page : t.pages()) body.append(raw(GuideData.Topic.pageKey(page))).append('\n');
            String title = body.toString().split("\n", 2)[0];
            titles.put(t.id(), title);
            bodies.put(t.id(), body.toString());
            all.add(new CodexText.Phrase(title, t.id()));
            for (String alias : t.aliases()) all.add(new CodexText.Phrase(alias, t.id()));
        }
        this.phrases = CodexText.sort(all);
        String start = topic != null ? topic : lastTopic != null && data.topics().containsKey(lastTopic) ? lastTopic : data.home();
        open(start);
    }

    // ---- navigation ------------------------------------------------------------------------------------------------

    /** Shows a topic and adds it to the history (dropping anything after the current place, like a browser). */
    public void open(String id) {
        if (!data.topics().containsKey(id)) return;
        if (id.equals(topicId)) return;
        while (history.size() > historyIndex + 1) history.removeLast();
        history.add(id);
        historyIndex = history.size() - 1;
        show(id);
    }

    public void back() {
        if (historyIndex > 0) show(history.get(--historyIndex));
    }

    public void forward() {
        if (historyIndex < history.size() - 1) show(history.get(++historyIndex));
    }

    private void show(String id) {
        topicId = id;
        lastTopic = id;
        contentScroll = 0;
        // one category open at a time when moving around (the player can still open more by clicking them)
        GuideData.Category category = data.categoryOf(id);
        openCategories.clear();
        if (category != null) openCategories.add(category.id());
        layoutTopic = null;
        rebuildSide();
        updateButtons();
    }

    public String topic() {
        return topicId;
    }

    public String title(String id) {
        return titles.get(id);
    }

    /** The topics this page links to, in reading order (text links, then "See also"). */
    public List<String> links() {
        layout();
        List<String> out = new ArrayList<>();
        for (Piece p : pieces) if (p.link() != null && !out.contains(p.link())) out.add(p.link());
        return out;
    }

    /** The topics the sidebar lists right now (search results while searching). */
    public List<String> sidebarTopics() {
        return sideRows.stream().filter(r -> r.topic() != null).map(SideRow::topic).toList();
    }

    /** The search text (for the client test). */
    public String searchText() {
        return query;
    }

    /** The search box's place: x, y, width, height in GUI pixels (for the client test). */
    public int[] searchBounds() {
        return new int[]{search.getX(), search.getY(), search.getWidth(), search.getHeight()};
    }

    public void setSearch(String text) {
        query = text;
        if (search != null) search.setValue(text);
        sideScroll = 0;
        rebuildSide();
    }

    /** How tall the current page is, and whether any word sticks out past the right edge. For tests. */
    public int contentHeight() {
        layout();
        return contentHeight;
    }

    /** Where (in GUI pixels) the first link to a topic is drawn, or null. For tests that click it with the mouse. */
    public double[] linkSpot(String target) {
        layout();
        int ox = contentLeft, oy = areaTop + 2 - (int) contentScroll;
        for (Piece p : pieces) {
            if (target.equals(p.link())) return new double[]{ox + p.x() + p.w() / 2.0, oy + p.y() + (p.kind() == Kind.CHIP ? 8 : 4)};
        }
        return null;
    }

    public boolean fitsWidth() {
        layout();
        int width = contentRight - contentLeft - 6;
        return pieces.stream().filter(p -> p.kind() == Kind.WORD || p.kind() == Kind.CHIP).allMatch(p -> p.x() + p.w() <= width);
    }

    // ---- widgets ---------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        areaTop = PAD + BAR;
        areaBottom = height - PAD;
        int sideWidth = Mth.clamp(width / 3, 112, 180);
        sideLeft = PAD + 2;
        sideRight = sideLeft + sideWidth;
        contentLeft = sideRight + 10;
        contentRight = width - PAD - 8;

        int x = PAD + 22 + font.width(boldTitle()) + 10;
        back = addRenderableWidget(Button.builder(Component.literal("<"), b -> back()).bounds(x, PAD + 2, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.codex.back"))).build());
        forward = addRenderableWidget(Button.builder(Component.literal(">"), b -> forward()).bounds(x + 20, PAD + 2, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.storagenexus.codex.forward"))).build());
        Component home = Component.translatable("screen.storagenexus.codex.home");
        addRenderableWidget(Button.builder(home, b -> open(data.home())).bounds(x + 40, PAD + 2, font.width(home) + 14, 18).build());

        int searchWidth = Mth.clamp(width / 5, 80, 180);
        search = new EditBox(font, width - PAD - 4 - searchWidth, PAD + 3, searchWidth, 16, Component.translatable("screen.storagenexus.codex.search"));
        search.setHint(Component.translatable("screen.storagenexus.codex.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(50);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            sideScroll = 0;
            rebuildSide();
        });
        addRenderableWidget(search);
        layoutTopic = null;
        rebuildSide();
        updateButtons();
    }

    private Component boldTitle() {
        return title.copy().withStyle(ChatFormatting.BOLD);
    }

    private void updateButtons() {
        if (back == null) return;
        back.active = historyIndex > 0;
        forward.active = historyIndex < history.size() - 1;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- sidebar ---------------------------------------------------------------------------------------------------

    private void rebuildSide() {
        sideRows.clear();
        String q = query.trim();
        if (!q.isEmpty()) {
            sideRows.add(new SideRow(0, null, null, I18n.get("screen.storagenexus.codex.results"), ItemStack.EMPTY));
            List<String> hits = new ArrayList<>();
            for (String id : data.topics().keySet()) if (CodexText.match(q, titles.get(id), bodies.get(id)) > 0) hits.add(id);
            hits.sort(Comparator.comparingInt((String id) -> -CodexText.match(q, titles.get(id), bodies.get(id))));
            for (String id : hits) sideRows.add(new SideRow(1, null, id, titles.get(id), icon(data.topic(id).icon())));
            if (hits.isEmpty()) sideRows.add(new SideRow(1, null, null, I18n.get("screen.storagenexus.codex.nothing"), ItemStack.EMPTY));
            return;
        }
        for (GuideData.Category c : data.categories()) {
            sideRows.add(new SideRow(0, c.id(), null, I18n.get(c.titleKey()), icon(c.icon())));
            if (!openCategories.contains(c.id())) continue;
            for (String id : c.topics()) sideRows.add(new SideRow(1, c.id(), id, titles.get(id), ItemStack.EMPTY));
        }
    }

    /** A page's text exactly as written: pages are not format strings, so "7.5%" stays "7.5%". */
    private static String raw(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    private static ItemStack icon(String id) {
        return RecipePainter.stackOf(id, 1);
    }

    private void clickSide(SideRow row) {
        if (row.topic() != null) {
            open(row.topic());
        } else if (row.category() != null) {
            if (!openCategories.remove(row.category())) openCategories.add(row.category());
            rebuildSide();
        }
    }

    // ---- page layout -----------------------------------------------------------------------------------------------

    private void layout() {
        int width = contentRight - contentLeft - 6;
        if (topicId.equals(layoutTopic) && width == layoutWidth) return;
        layoutTopic = topicId;
        layoutWidth = width;
        List<Piece> out = new ArrayList<>();
        GuideData.Topic topic = data.topic(topicId);
        int y = 2;
        GuideData.Category category = data.categoryOf(topicId);
        if (category != null) {
            out.add(new Piece(Kind.WORD, 0, y, 0, I18n.get(category.titleKey()) + "  >", GREY, null, null, null));
            y += 13;
        }
        out.add(new Piece(Kind.ICON, 0, y, 32, null, 0, null, icon(topic.icon()), null));
        out.add(new Piece(Kind.HEADING, 40, y + 12, 0, titles.get(topicId), WHITE, null, null, null));
        y += 38;
        out.add(new Piece(Kind.RULE_LINE, 0, y, width, null, RULE, null, null, null));
        y += 7;

        Set<String> linked = new HashSet<>();
        for (int p = 0; p < topic.pages().size(); p++) {
            String[] lines = raw(GuideData.Topic.pageKey(topic.pages().get(p))).split("\n", -1);
            if (p > 0) {
                y += 5;
                out.add(new Piece(Kind.HEADING, 0, y, 0, lines[0], GOLD, null, null, null));
                y += 13;
            }
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].isBlank()) {
                    y += 5;
                    continue;
                }
                y = flow(out, CodexText.links(lines[i], phrases, linked, topicId), width, y);
            }
        }

        if (!topic.recipes().isEmpty()) {
            y += 9;
            out.add(new Piece(Kind.HEADING, 0, y, 0, I18n.get("screen.storagenexus.codex.recipes"), GOLD, null, null, null));
            y += 14;
            int cardWidth = RecipePainter.GRID_WIDTH + 22 + NAME_WIDTH, perRow = Math.max(1, (width + 10) / (cardWidth + 10));
            for (int i = 0; i < topic.recipes().size(); i++) {
                int col = i % perRow;
                if (col == 0 && i > 0) y += RecipePainter.CARD_HEIGHT;
                GuideData.Recipe recipe = data.recipes().get(topic.recipes().get(i));
                if (recipe != null) out.add(new Piece(Kind.RECIPE, col * (cardWidth + 10), y, cardWidth, null, 0, null, null, recipe));
            }
            y += RecipePainter.CARD_HEIGHT;
        }

        if (!topic.see().isEmpty()) {
            y += 6;
            out.add(new Piece(Kind.HEADING, 0, y, 0, I18n.get("screen.storagenexus.codex.see_also"), GOLD, null, null, null));
            y += 14;
            int x = 0;
            for (String id : topic.see()) {
                String label = titles.get(id);
                int w = font.width(label) + 24;
                if (x > 0 && x + w > width) {
                    x = 0;
                    y += 19;
                }
                out.add(new Piece(Kind.CHIP, x, y, w, label, LINK, id, icon(data.topic(id).icon()), null));
                x += w + 5;
            }
            y += 20;
        }
        pieces = out;
        contentHeight = y + 4;
    }

    /** Lays out a line of text (with its links) word by word, wrapping at the page width. Returns the y below it. */
    private int flow(List<Piece> out, List<CodexText.Segment> segments, int width, int y) {
        int x = 0, space = font.width(" ");
        for (CodexText.Segment s : segments) {
            String[] words = s.text().split(" ", -1);
            for (int k = 0; k < words.length; k++) {
                if (k > 0) x += space;
                String word = words[k];
                if (word.isEmpty()) continue;
                int w = font.width(word);
                if (x > 0 && x + w > width) {
                    x = 0;
                    y += LINE;
                }
                out.add(new Piece(Kind.WORD, x, y, w, word, s.link() != null ? LINK : TEXT, s.link(), null, null));
                x += w;
            }
        }
        return y + LINE;
    }

    // ---- drawing ---------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(PAD - 2, PAD - 2, width - PAD + 2, height - PAD + 2, PANEL);
        g.outline(PAD - 2, PAD - 2, width - 2 * PAD + 4, height - 2 * PAD + 4, EDGE);
        g.fill(sideLeft, areaTop, sideRight, areaBottom, SIDE_BG);
        g.outline(sideLeft, areaTop, sideRight - sideLeft, areaBottom - areaTop, EDGE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.item(icon("storagenexus:guide"), PAD + 2, PAD + 3);
        g.text(font, boldTitle(), PAD + 22, PAD + 7, GOLD, true);
        extractSidebar(g, mouseX, mouseY);
        extractPage(g, mouseX, mouseY);
        Component tip = Component.translatable("screen.storagenexus.codex.tip", CodexClient.LOOKUP.getTranslatedKeyMessage());
        g.text(font, font.plainSubstrByWidth(tip.getString(), contentRight - contentLeft), contentLeft, areaBottom - 9, DIM, false);
    }

    private void extractSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int visible = areaBottom - areaTop - 4;
        int total = sideRows.size() * ROW;
        sideScroll = Mth.clamp(sideScroll, 0, Math.max(0, total - visible));
        g.enableScissor(sideLeft + 1, areaTop + 1, sideRight - 1, areaBottom - 1);
        int y = areaTop + 3 - (int) sideScroll;
        for (SideRow row : sideRows) {
            boolean hovered = mouseX >= sideLeft && mouseX < sideRight - 5 && mouseY >= y - 1 && mouseY < y + ROW - 1 && inSide(mouseX, mouseY);
            boolean selected = row.topic() != null && row.topic().equals(topicId);
            if (selected) {
                g.fill(sideLeft + 2, y - 2, sideRight - 6, y + ROW - 2, SELECT);
                g.outline(sideLeft + 2, y - 2, sideRight - 8 - sideLeft, ROW, SELECT_EDGE);
            } else if (hovered && (row.topic() != null || row.category() != null)) {
                g.fill(sideLeft + 2, y - 2, sideRight - 6, y + ROW - 2, 0x30FFFFFF);
            }
            int x = sideLeft + 4 + row.depth() * 10;
            if (row.depth() == 0 && row.category() != null) {
                g.text(font, openCategories.contains(row.category()) ? "v" : ">", x, y + 1, DIM, false);
                x += 7;
            }
            if (!row.icon().isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(x, y - 2);
                g.pose().scale(0.75f, 0.75f);
                g.item(row.icon(), 0, 0);
                g.pose().popMatrix();
                x += 14;
            }
            int color = selected ? WHITE : row.depth() == 0 && row.category() != null ? GOLD : row.topic() != null ? GREY : DIM;
            if (hovered && !selected && row.topic() != null) color = WHITE;
            g.text(font, font.plainSubstrByWidth(row.label(), sideRight - 8 - x), x, y + 1, color, true);
            y += ROW;
        }
        g.disableScissor();
        scrollbar(g, sideRight - 4, areaTop + 1, areaBottom - 1, sideScroll, total, visible);
    }

    private void extractPage(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        layout();
        int top = areaTop + 2, bottom = areaBottom - FOOTER;
        int visible = bottom - top;
        contentScroll = Mth.clamp(contentScroll, 0, Math.max(0, contentHeight - visible));
        int ox = contentLeft, oy = top - (int) contentScroll;
        String hoveredLink = inContent(mouseX, mouseY) ? linkAt(mouseX, mouseY) : null;
        hoveredStack = ItemStack.EMPTY;
        g.enableScissor(contentLeft - 2, top, contentRight + 1, bottom);
        for (Piece p : pieces) {
            int x = ox + p.x(), y = oy + p.y();
            if (y > bottom + 70 || y + 70 < top) continue;
            switch (p.kind()) {
                case WORD -> {
                    int color = p.link() != null && p.link().equals(hoveredLink) ? LINK_HOVER : p.color();
                    g.text(font, p.text(), x, y, color, true);
                    if (p.link() != null) g.fill(x, y + 9, x + p.w(), y + 10, color);
                }
                case HEADING -> g.text(font, Component.literal(p.text()).withStyle(ChatFormatting.BOLD), x, y, p.color(), true);
                case ICON -> {
                    g.pose().pushMatrix();
                    g.pose().translate(x, y);
                    g.pose().scale(2f, 2f);
                    g.item(p.icon(), 0, 0);
                    g.pose().popMatrix();
                }
                case RULE_LINE -> g.fill(x, y, x + p.w(), y + 1, p.color());
                case RECIPE -> {
                    boolean mouseIn = mouseY >= top && mouseY < bottom;
                    ItemStack h = RecipePainter.card(g, font, p.recipe(), x + 1, y, NAME_WIDTH, TEXT, DIM,
                            mouseIn ? mouseX : -1000, mouseIn ? mouseY : -1000);
                    if (!h.isEmpty()) hoveredStack = h;
                }
                case CHIP -> {
                    boolean hot = p.link().equals(hoveredLink);
                    g.fill(x, y, x + p.w(), y + 16, hot ? 0xE0404070 : CHIP);
                    g.outline(x, y, p.w(), 16, hot ? 0xFFB0B0FF : 0xFF6E6EAA);
                    g.pose().pushMatrix();
                    g.pose().translate(x + 3, y + 2);
                    g.pose().scale(0.75f, 0.75f);
                    g.item(p.icon(), 0, 0);
                    g.pose().popMatrix();
                    g.text(font, p.text(), x + 19, y + 4, hot ? LINK_HOVER : LINK, true);
                }
            }
        }
        g.disableScissor();
        scrollbar(g, contentRight + 3, top, bottom, contentScroll, contentHeight, visible);
        if (hoveredLink != null) {
            g.setTooltipForNextFrame(Component.translatable("screen.storagenexus.codex.open", titles.get(hoveredLink)), mouseX, mouseY);
        }
    }

    private void scrollbar(GuiGraphicsExtractor g, int x, int top, int bottom, double scroll, int total, int visible) {
        if (total <= visible) return;
        g.fill(x, top, x + 3, bottom, 0x60000000);
        int barHeight = Math.max(12, (bottom - top) * visible / total);
        int barTop = top + (int) ((bottom - top - barHeight) * scroll / Math.max(1, total - visible));
        g.fill(x, barTop, x + 3, barTop + barHeight, 0xFF9A9A9A);
    }

    // ---- mouse -----------------------------------------------------------------------------------------------------

    private boolean inSide(double x, double y) {
        return x >= sideLeft && x < sideRight && y >= areaTop && y < areaBottom;
    }

    private boolean inContent(double x, double y) {
        return x >= contentLeft - 2 && x < contentRight + 6 && y >= areaTop + 2 && y < areaBottom - FOOTER;
    }

    private String linkAt(double mouseX, double mouseY) {
        layout();
        int ox = contentLeft, oy = areaTop + 2 - (int) contentScroll;
        for (Piece p : pieces) {
            if (p.link() == null) continue;
            int x = ox + p.x(), y = oy + p.y(), h = p.kind() == Kind.CHIP ? 16 : 10;
            if (mouseX >= x && mouseX < x + p.w() && mouseY >= y && mouseY < y + h) return p.link();
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // A right-click on the search box clears it
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && search.isMouseOver(event.x(), event.y())) {
            search.setValue("");
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        double x = event.x(), y = event.y();
        if (inSide(x, y)) {
            int index = (int) Math.floor((y - areaTop - 1 + sideScroll) / ROW);
            if (index >= 0 && index < sideRows.size()) clickSide(sideRows.get(index));
            return true;
        }
        if (inContent(x, y)) {
            String link = linkAt(x, y);
            if (link != null) {
                open(link);
                return true;
            }
            if (!hoveredStack.isEmpty()) {
                String target = GuideScreens.topicFor(hoveredStack);
                if (target != null) {
                    open(target);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inSide(mouseX, mouseY)) {
            sideScroll -= scrollY * ROW * 2;
            return true;
        }
        if (inContent(mouseX, mouseY)) {
            contentScroll -= scrollY * LINE * 3;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
