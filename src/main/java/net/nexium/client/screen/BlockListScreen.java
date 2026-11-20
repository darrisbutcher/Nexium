package net.nexium.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.nexium.essentials.config.ConfigSchema.Entry;
import net.nexium.essentials.config.NexusConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Edits one of the settings that is a list of blocks, such as the vein mining blacklist: type a block id ({@code diamond_ore} or
 * {@code minecraft:diamond_ore}) or a tag ({@code #minecraft:logs}) and press Add, or add the block you last looked at; each
 * entry has a button that takes it off. Changes are saved as they are made.
 */
public class BlockListScreen extends Screen {
    private static final int PAD = 8, ROW = 20, WIDTH = 260;
    private static final int PANEL = 0xD8101016, EDGE = 0xFF50505F, GOLD = 0xFFFFBE3C, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98,
            GOOD = 0xFF7CE08C, BAD = 0xFFFF7070;

    private final Screen parent;
    private final Entry entry;
    private EditBox input;
    private Button add, addLooked;
    private final List<Button> removeButtons = new ArrayList<>();
    private int firstRow;
    private int listLeft, listTop, listBottom;
    private Component message = Component.empty();
    private int messageColor = DIM;

    public BlockListScreen(Screen parent, Entry entry) {
        super(Component.translatable(entry.nameKey()));
        this.parent = parent;
        this.entry = entry;
    }

    // ---- state for tests -------------------------------------------------------------------------------------------

    public List<String> entries() {
        return List.copyOf(entry.getList(NexusConfig.get()));
    }

    /** Types {@code text} in the box and presses Add, as a player would. */
    public void typeAndAdd(String text) {
        input.setValue(text);
        addTyped();
    }

    // ---- widgets ---------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        listLeft = left;
        int y = 34;
        input = addRenderableWidget(new EditBox(font, left, y, WIDTH - 54, 18, Component.translatable("screen.nexium.blocklist.input")));
        input.setMaxLength(120);
        input.setHint(Component.translatable("screen.nexium.blocklist.hint"));
        setInitialFocus(input);
        add = addRenderableWidget(Button.builder(Component.translatable("screen.nexium.blocklist.add"), b -> addTyped())
                .bounds(left + WIDTH - 50, y, 50, 18).build());
        addLooked = addRenderableWidget(Button.builder(Component.translatable("screen.nexium.blocklist.add_looked"), b -> addLooked())
                .bounds(left, y + 22, WIDTH, 18).build());
        listTop = y + 74;
        listBottom = height - 34;
        addRenderableWidget(Button.builder(Component.translatable("screen.nexium.config.done"), b -> onClose())
                .bounds(left + WIDTH / 2 - 50, height - 28, 100, 20).build());
        rebuild();
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW);
    }

    private void rebuild() {
        for (Button b : removeButtons) removeWidget(b);
        removeButtons.clear();
        List<String> list = entry.getList(NexusConfig.get());
        firstRow = Mth.clamp(firstRow, 0, Math.max(0, list.size() - visibleRows()));
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            removeButtons.add(addRenderableWidget(Button.builder(Component.literal("x"), b -> remove(index))
                    .bounds(0, 0, 18, 16).build()));
        }
        layout();
        addLooked.active = lookedAt() != null;
    }

    private void layout() {
        int visible = visibleRows();
        for (int i = 0; i < removeButtons.size(); i++) {
            Button b = removeButtons.get(i);
            boolean shown = i >= firstRow && i < firstRow + visible;
            b.visible = shown;
            b.active = shown;
            b.setPosition(listLeft + WIDTH - 18, listTop + (i - firstRow) * ROW + 1);
        }
    }

    // ---- actions ---------------------------------------------------------------------------------------------------

    private String lookedAt() {
        if (minecraft == null || minecraft.level == null) return null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(hit.getBlockPos()).getBlock()).toString();
    }

    private void addTyped() {
        String typed = input.getValue();
        String normal = NexusConfig.blacklistEntry(typed);
        if (normal == null) {
            say(Component.translatable("screen.nexium.blocklist.unknown", typed.trim()), BAD);
            return;
        }
        if (put(normal)) input.setValue("");
    }

    private void addLooked() {
        String id = lookedAt();
        if (id != null) put(id);
    }

    private boolean put(String normal) {
        List<String> list = entry.getList(NexusConfig.get());
        if (list.contains(normal)) {
            say(Component.translatable("screen.nexium.blocklist.have", normal), BAD);
            return false;
        }
        list.add(normal);
        NexusConfig.save();
        say(Component.translatable("screen.nexium.blocklist.added", normal), GOOD);
        rebuild();
        return true;
    }

    private void remove(int index) {
        List<String> list = entry.getList(NexusConfig.get());
        if (index < 0 || index >= list.size()) return;
        String gone = list.remove(index);
        NexusConfig.save();
        say(Component.translatable("screen.nexium.blocklist.removed", gone), DIM);
        rebuild();
    }

    private void say(Component text, int color) {
        message = text;
        messageColor = color;
    }

    // ---- input and drawing -----------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (input != null && input.isFocused() && (event.key() == 257 || event.key() == 335)) { // Enter
            addTyped();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<String> list = entry.getList(NexusConfig.get());
        if (list.size() > visibleRows()) {
            firstRow = Mth.clamp(firstRow - (int) Math.signum(scrollY), 0, list.size() - visibleRows());
            layout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        NexusConfig.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int left = (width - WIDTH) / 2 - PAD;
        g.fill(left, 4, left + WIDTH + 2 * PAD, height - 4, PANEL);
        g.fill(left, 4, left + WIDTH + 2 * PAD, 5, EDGE);
        g.fill(left, height - 5, left + WIDTH + 2 * PAD, height - 4, EDGE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), listLeft, 12, GOLD, false);
        addLooked.active = lookedAt() != null;
        List<String> list = entry.getList(NexusConfig.get());
        g.text(font, font.plainSubstrByWidth(message.getString(), WIDTH), listLeft, listTop - 24, messageColor, false);
        g.text(font, Component.translatable("screen.nexium.blocklist.count", list.size()), listLeft, listTop - 12, DIM, false);
        if (list.isEmpty()) {
            g.text(font, Component.translatable("screen.nexium.blocklist.empty"), listLeft, listTop + 4, DIM, false);
        }
        int visible = visibleRows();
        for (int i = firstRow; i < list.size() && i < firstRow + visible; i++) {
            int y = listTop + (i - firstRow) * ROW;
            g.fill(listLeft, y, listLeft + WIDTH - 22, y + ROW - 2, 0x40FFFFFF);
            g.text(font, font.plainSubstrByWidth(list.get(i), WIDTH - 32), listLeft + 4, y + 6, TEXT, false);
        }
    }
}
