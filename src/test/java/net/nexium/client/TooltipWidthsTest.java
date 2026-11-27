package net.nexium.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every Nexium tooltip wraps to the one width, {@code Tooltips.MAX_WIDTH}. A screen that showed its own lines with its own number (the
 * Backpack's trash hint was 160, the grindstone's 150, the conduit scanner's 230) made some tooltips wider than others, and one that
 * showed its lines without wrapping let a long line run off the screen. So screens show component tooltips through
 * {@code Tooltips.show}, and split text for {@code setTooltipForNextFrame} with {@code Tooltips.split}, never with a number of their own.
 * (Buttons' tooltips wrap by themselves, to the same 170.)
 */
class TooltipWidthsTest {
    /** These show a real item's tooltip (its lines come from the item, and a Nexium item wraps its own lines). */
    private static final Set<String> ITEM_TOOLTIPS = Set.of("TerminalView.java", "RecipePainter.java", "AutocraftRequestScreen.java");

    private static final Pattern NUMERIC_SPLIT = Pattern.compile("\\.split\\([^;]*,\\s*\\d+\\)");

    @Test
    void everyTooltipUsesTheSharedWidth() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("Tooltips.java")) continue;
                String text = Files.readString(file);
                if (text.contains(".setComponentTooltipForNextFrame(")) {
                    found.add(file + ": shows a component tooltip without Tooltips.show");
                }
                if (text.contains(".setTooltipForNextFrame(") && !ITEM_TOOLTIPS.contains(name)) {
                    if (!text.contains("Tooltips.")) found.add(file + ": shows lines without Tooltips.split");
                    if (NUMERIC_SPLIT.matcher(text).find()) found.add(file + ": splits text to a width of its own (use Tooltips.split)");
                }
            }
        }
        assertTrue(found.isEmpty(), "show tooltips through Tooltips (one width for all):\n" + String.join("\n", found));
    }
}
