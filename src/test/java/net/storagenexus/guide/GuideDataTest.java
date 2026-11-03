package net.storagenexus.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    private static final String CHAPTERS = """
            {"chapters": [
              {"id": "things", "cover": "blue", "pages": [
                {"type": "contents", "entries": ["intro", "recipes"]},
                {"type": "text", "id": "intro"},
                {"type": "recipes", "id": "recipes", "recipes": ["frame", "cable"]}
              ]}
            ]}
            """;

    private static GuideData sample() {
        return GuideData.parse(object(CHAPTERS), object(RECIPES));
    }

    private static JsonObject object(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void chapterPagesAndLangKeys() {
        GuideData.Chapter chapter = sample().chapter("things");
        assertEquals(3, chapter.pages().size());
        assertEquals(1, chapter.indexOf("intro"));
        assertEquals(2, chapter.indexOf("recipes"));
        assertEquals(0, chapter.indexOf("contents"));
        assertEquals(-1, chapter.indexOf("nowhere"));
        assertEquals("guide.storagenexus.chapter.things", chapter.titleKey());
        assertEquals("guide.storagenexus.things.intro", chapter.pageKey("intro"));
        assertEquals("guide.storagenexus.things.toc.intro", chapter.contentsKey("intro"));
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
    void unknownPageTypesAreRejected() {
        String bad = "{\"chapters\": [{\"id\": \"x\", \"cover\": \"y\", \"pages\": [{\"type\": \"video\"}]}]}";
        assertThrows(IllegalArgumentException.class, () -> GuideData.parse(object(bad), object(RECIPES)));
    }
}
