package net.nexium.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.nexium.Nexium;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps Nexium tooltips narrow and all the same width ({@link #MAX_WIDTH}, which is also what vanilla wraps a button's tooltip to). A tooltip line that is wider than {@link #MAX_WIDTH} is split into lines that fit, keeping its colours
 * and styles. Every line of a Nexium item's tooltip is wrapped this way, after all the other tooltip callbacks have added theirs, and
 * the screens that build their own tooltip lines wrap them with {@link #wrapAll}. (Buttons' tooltips wrap by themselves, at 170.)
 */
public final class Tooltips {
    /** The widest a line of a Nexium tooltip is, in GUI pixels, before it wraps. */
    public static final int MAX_WIDTH = 170;

    /** Runs after the callbacks that add the lines (the default phase), so it sees all of them. */
    private static final net.minecraft.resources.Identifier LATE = Nexium.id("tooltip_wrap");

    /** Runs after {@link #LATE}: squeezes a tooltip that is taller than the screen. */
    private static final net.minecraft.resources.Identifier COMPACT = Nexium.id("tooltip_compact");
    /** Pixels one tooltip line takes, and the least of the screen left clear. */
    private static final int LINE_HEIGHT = 10, SCREEN_MARGIN = 12;
    /** Fewest enchantments in a row that are worth joining into one paragraph. */
    private static final int MIN_RUN = 4;

    public static void init() {
        ItemTooltipCallback.EVENT.addPhaseOrdering(LATE, COMPACT);
        ItemTooltipCallback.EVENT.register(COMPACT, (stack, context, flag, lines) -> compactEnchantments(stack, lines));
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE);
        ItemTooltipCallback.EVENT.register(LATE, (stack, context, flag, lines) -> {
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Nexium.MOD_ID)) {
                wrapAll(Minecraft.getInstance().font, lines, MAX_WIDTH, 1);
            }
        });
    }

    /**
     * A tooltip taller than the screen has its top cut off (a helmet with every enchantment, say). So when it would be, the enchantment
     * lines, one to a line, are joined into a comma separated paragraph as wide as the screen allows (up to 330 pixels), colours kept
     * (curses stay red). Nothing changes when the tooltip already fits.
     */
    static void compactEnchantments(net.minecraft.world.item.ItemStack stack, List<Component> lines) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null || lines.size() * LINE_HEIGHT + SCREEN_MARGIN <= mc.getWindow().getGuiScaledHeight()) return;
        java.util.Set<String> names = new java.util.HashSet<>();
        for (var type : List.of(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)) {
            net.minecraft.world.item.enchantment.ItemEnchantments enchantments = stack.getOrDefault(type, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) {
                names.add(net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
            }
        }
        if (names.isEmpty()) return;
        int start = -1, end = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (!names.contains(lines.get(i).getString())) continue;
            int j = i;
            while (j < lines.size() && names.contains(lines.get(j).getString())) j++;
            if (j - i >= MIN_RUN) { start = i; end = j; break; }
            i = j;
        }
        if (start < 0) return;
        MutableComponent joined = Component.empty();
        for (int i = start; i < end; i++) {
            if (i > start) joined.append(Component.literal(", ").withStyle(net.minecraft.ChatFormatting.GRAY));
            joined.append(lines.get(i));
        }
        int width = Math.max(MAX_WIDTH, Math.min(330, mc.getWindow().getGuiScaledWidth() - 60));
        List<Component> paragraph = wrap(mc.font, joined, width);
        lines.subList(start, end).clear();
        lines.addAll(start, paragraph);
        // Still too tall: the blank lines between the parts go too
        if (lines.size() * LINE_HEIGHT + SCREEN_MARGIN > mc.getWindow().getGuiScaledHeight()) lines.removeIf(line -> line.getString().isEmpty());
    }

    /** One line as lines no wider than {@code maxWidth}: the line itself if it fits. Colours and styles stay. */
    public static List<Component> wrap(Font font, Component line, int maxWidth) {
        if (font.width(line) <= maxWidth) return List.of(line);
        List<Component> out = new ArrayList<>();
        for (FormattedText part : font.getSplitter().splitLines(line, maxWidth, Style.EMPTY)) out.add(toComponent(part));
        return out;
    }

    /** Every line of the list wrapped in place, from index {@code from} on (the first line of an item tooltip, its name, is left whole). */
    public static void wrapAll(Font font, List<Component> lines, int maxWidth, int from) {
        List<Component> wrapped = new ArrayList<>(lines.subList(0, Math.min(from, lines.size())));
        for (int i = from; i < lines.size(); i++) wrapped.addAll(wrap(font, lines.get(i), maxWidth));
        lines.clear();
        lines.addAll(wrapped);
    }

    /** Shows a tooltip of component lines, each wrapped to {@link #MAX_WIDTH}. Every Nexium screen shows its tooltips through this. */
    public static void show(GuiGraphicsExtractor g, Font font, List<Component> lines, int mouseX, int mouseY) {
        g.setComponentTooltipForNextFrame(font, wrapped(font, lines), mouseX, mouseY);
    }

    /** A component split for {@code setTooltipForNextFrame}, to {@link #MAX_WIDTH}. */
    public static List<FormattedCharSequence> split(Font font, Component line) {
        return font.split(line, MAX_WIDTH);
    }

    /** The same, to the room there is when that is less than {@link #MAX_WIDTH} (a small screen). */
    public static List<FormattedCharSequence> split(Font font, Component line, int room) {
        return font.split(line, Math.min(MAX_WIDTH, room));
    }

    /** A new list: every line wrapped. */
    public static List<Component> wrapped(Font font, List<Component> lines) {
        List<Component> copy = new ArrayList<>(lines);
        wrapAll(font, copy, MAX_WIDTH, 0);
        return copy;
    }

    private static Component toComponent(FormattedText text) {
        MutableComponent out = Component.empty();
        text.visit((style, string) -> {
            out.append(Component.literal(string).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private Tooltips() {}
}
