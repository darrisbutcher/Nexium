package net.storagenexus;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every game test class is listed in the test mod's fabric.mod.json; one that isn't would never run. */
class GameTestRegistrationTest {
    private static final Path SOURCES = Path.of("src/gametest/java");
    private static final Path MOD_JSON = Path.of("src/gametest/resources/fabric.mod.json");

    @Test
    void everyGameTestClassIsRegistered() throws IOException {
        String json = Files.readString(MOD_JSON);
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (!source.contains("@GameTest") && !source.contains("implements FabricClientGameTest")) continue;
                String name = SOURCES.relativize(file).toString().replace(java.io.File.separatorChar, '.').replaceAll("\\.java$", "");
                if (!json.contains("\"" + name + "\"")) missing.add(name);
            }
        }
        assertTrue(missing.isEmpty(), "not in src/gametest/resources/fabric.mod.json, so they never run: " + missing);
    }
}
