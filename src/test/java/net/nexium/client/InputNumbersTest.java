package net.nexium.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game numbers its mouse buttons and keys the SDL way (the left button is 1, Enter is 40), not the GLFW way the numbers in older
 * code and tutorials are (0 and 257). A screen that tested {@code event.button() == 0} ignored every real left click and still passed
 * a test that built the click by hand with that same 0. So the code never compares a button or key with a bare number: it uses the
 * game's constants ({@code InputConstants.MOUSE_BUTTON_LEFT}, {@code KEY_RETURN}) or the event's own tests ({@code isConfirmation()}).
 */
class InputNumbersTest {
    private static final Pattern BARE_NUMBER = Pattern.compile("\\.(button|key|input|scancode)\\(\\)\\s*(==|!=|<|>|<=|>=)\\s*\\d|\\d\\s*(==|!=)\\s*\\w+(\\.\\w+)*\\.(button|key|input|scancode)\\(\\)");

    @Test
    void noButtonOrKeyIsComparedWithABareNumber() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.trim().startsWith("//") || line.trim().startsWith("*")) continue;
                    if (BARE_NUMBER.matcher(line).find()) found.add(file + ":" + (i + 1) + ": " + line.trim());
                }
            }
        }
        assertTrue(found.isEmpty(), "compare buttons and keys with InputConstants constants, not numbers:\n" + String.join("\n", found));
    }
}
