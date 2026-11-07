package net.storagenexus.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuideDataTest {
    private static final String RECIPES = """
            {"recipes": {
              "frame": {"type": "shaped", "pattern": ["PPP", "P P", "PPP"], "key": {"P": "#minecraft:planks"},
                        "result": {"id": "minecraft:item_frame", "count": 2}, "note": "planks"},
              "cable": {"type": "shaped", "pattern": ["IRI"], "key": {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"},
                        "result": {"id": "mod:cable", "count": 4}},
              "card": {"type": "shapeless", "ingredients": ["minecraft:paper", "minecraft:ender_pearl"], "result": {"id": "mod:card"}},
              "upgrade": {"type": "smithing", "template": "a:template", "base": "a:base", "addition": "a:addition",
                          "result": {"id": "a:result"}, "verify": "a:result_smithing"}
            }}
            """;

    private static final String CODEX = """
            {"categories": [
              {"id": "basics", "icon": "mod:book", "topics": ["intro", "cables"]},
              {"id": "more", "icon": "mod:cable", "topics": ["cables"]}
            ],
             "topics": {
              "intro": {"pages": ["start.intro", "start.more"], "icon": "mod:book", "see": ["cables"]},
              "cables": {"pages": ["net.cable"], "icon": "mod:cable", "recipes": ["cable"], "items": ["mod:cable", "#mod:wires"],
                         "aliases": ["cable", "wire"]}
            }}
            """;

    private static GuideData sample() {
        return GuideData.parse(object(CODEX), object(RECIPES));
    }

    private static JsonObject object(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void categoriesTopicsAndLangKeys() {
        GuideData data = sample();
        assertEquals(2, data.categories().size());
        assertEquals("codex.storagenexus.category.basics", data.categories().getFirst().titleKey());
        assertEquals("intro", data.home());
        GuideData.Topic intro = data.topic("intro");
        assertEquals("guide.storagenexus.start.intro", intro.titleKey());
        assertEquals(List.of("start.intro", "start.more"), intro.pages());
        assertEquals(List.of("cables"), intro.see());
        assertEquals(List.of(), intro.recipes());
        GuideData.Topic cables = data.topic("cables");
        assertEquals(List.of("mod:cable", "#mod:wires"), cables.items());
        assertEquals("basics", data.categoryOf("cables").id(), "a topic in two categories belongs to the first for the breadcrumb");
        assertThrows(IllegalArgumentException.class, () -> data.topic("nowhere"));
    }

    @Test
    void shapedRecipesAreLaidOutFromTheTopLeft() {
        GuideData.Recipe frame = sample().recipes().get("frame");
        assertEquals(Arrays.asList("#minecraft:planks", "#minecraft:planks", "#minecraft:planks",
                "#minecraft:planks", null, "#minecraft:planks",
                "#minecraft:planks", "#minecraft:planks", "#minecraft:planks"), frame.cells());
        assertEquals(2, frame.count());
        assertEquals("guide.storagenexus.note.planks", frame.noteKey());

        // A pattern smaller than 3x3 fills the first cells and leaves the rest empty
        GuideData.Recipe cable = sample().recipes().get("cable");
        assertEquals("minecraft:redstone", cable.cells().get(1));
        assertNull(cable.cells().get(3));
        assertNull(cable.note());
        assertEquals(3, cable.rows());
    }

    @Test
    void shapelessAndSmithingRecipes() {
        GuideData.Recipe card = sample().recipes().get("card");
        assertEquals(9, card.cells().size());
        assertEquals("minecraft:ender_pearl", card.cells().get(1));
        assertNull(card.cells().get(2));
        assertEquals("shapeless", card.note());
        assertEquals("mod:card", card.verify());

        GuideData.Recipe upgrade = sample().recipes().get("upgrade");
        assertEquals(1, upgrade.rows());
        assertEquals(Arrays.asList("a:template", "a:base", "a:addition"), upgrade.cells());
        assertEquals("a:result_smithing", upgrade.verify());
    }

    @Test
    void brokenCodexFilesAreRejected() {
        String unknownTopic = "{\"categories\": [{\"id\": \"a\", \"icon\": \"x:y\", \"topics\": [\"ghost\"]}], \"topics\": {}}";
        assertThrows(IllegalArgumentException.class, () -> GuideData.parse(object(unknownTopic), object(RECIPES)));
        String noPages = "{\"categories\": [], \"topics\": {\"a\": {\"pages\": [], \"icon\": \"x:y\"}}}";
        assertThrows(IllegalArgumentException.class, () -> GuideData.parse(object(noPages), object(RECIPES)));
    }

    @Test
    void namesOfOtherTopicsBecomeLinksOnce() {
        List<CodexText.Phrase> phrases = CodexText.sort(List.of(new CodexText.Phrase("Nexus Link", "link"),
                new CodexText.Phrase("Nexus Link Module", "module"), new CodexText.Phrase("tool menu", "menu"),
                new CodexText.Phrase("AIOT", "aiot")));
        Set<String> linked = new HashSet<>();
        List<CodexText.Segment> line = CodexText.links("Put a Nexus Link Module in the Tool menu of an AIOT.", phrases, linked, "self");
        assertEquals(List.of(new CodexText.Segment("Put a ", null), new CodexText.Segment("Nexus Link Module", "module"),
                new CodexText.Segment(" in the ", null), new CodexText.Segment("Tool menu", "menu"),
                new CodexText.Segment(" of an ", null), new CodexText.Segment("AIOT", "aiot"), new CodexText.Segment(".", null)),
                line, "the longest name wins, case doesn't matter, punctuation stays plain");
        List<CodexText.Segment> again = CodexText.links("Every AIOT has a tool menu.", phrases, linked, "self");
        assertEquals(List.of(new CodexText.Segment("Every AIOT has a tool menu.", null)), again, "only the first mention on a page links");
        assertEquals(List.of(new CodexText.Segment("AIOTs and AIOT", null)),
                CodexText.links("AIOTs and AIOT", phrases, new HashSet<>(), "aiot"), "a topic never links to itself");
        assertEquals(List.of(new CodexText.Segment("RAIOT", null)), CodexText.links("RAIOT", phrases, new HashSet<>(), "x"),
                "only whole words link");
    }

    @Test
    void searchPrefersTitles() {
        assertEquals(2, CodexText.match("vein", "Vein Mining", "Hold V while mining."));
        assertEquals(1, CodexText.match("hold v", "Vein Mining", "Hold V while mining."));
        assertEquals(2, CodexText.match("MINING vein", "Vein Mining", ""));
        assertEquals(0, CodexText.match("farm", "Vein Mining", "Hold V while mining."));
        assertEquals(0, CodexText.match("   ", "Vein Mining", ""));
    }
}
