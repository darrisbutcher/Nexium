package net.storagenexus.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The guide's chapters and recipe cards, read from {@code assets/storagenexus/guide/chapters.json} and
 * {@code recipes.json}. Text is not in here: every page, chapter title and recipe name is a lang key built from the ids
 * ({@code guide.storagenexus.<chapter>.<page>}), so wording lives in the lang file like everything else.
 */
public record GuideData(List<Chapter> chapters, Map<String, Recipe> recipes) {
    /** A page of a chapter. A recipe sheet holds up to two recipe cards, drawn over a blank page. */
    public sealed interface Page permits TextPage, ContentsPage, RecipeSheet {}

    public record TextPage(String id) implements Page {}

    /** A clickable list that jumps to the pages with these ids. */
    public record ContentsPage(List<String> targets) implements Page {}

    public record RecipeSheet(String id, List<String> recipeIds) implements Page {}

    public record Chapter(String id, String cover, List<Page> pages) {
        public String titleKey() {
            return "guide.storagenexus.chapter." + id;
        }

        public String topicsKey() {
            return titleKey() + ".topics";
        }

        public String pageKey(String pageId) {
            return "guide.storagenexus." + id + "." + pageId;
        }

        /** Lang key of the contents line that links to the page with this id. */
        public String contentsKey(String pageId) {
            return "guide.storagenexus." + id + ".toc." + pageId;
        }

        /** The 0-based index of the page with this id, or -1. */
        public int indexOf(String pageId) {
            for (int i = 0; i < pages.size(); i++) {
                String id = switch (pages.get(i)) {
                    case TextPage t -> t.id();
                    case RecipeSheet r -> r.id();
                    case ContentsPage c -> "contents";
                };
                if (id.equals(pageId)) return i;
            }
            return -1;
        }
    }

    public enum Kind {
        /** Ingredients where the pattern puts them; the recipe may be smaller than 3x3. */
        SHAPED,
        /** Ingredients in any order; shown in the first cells. */
        SHAPELESS,
        /** Template, base and addition, shown in one row. */
        SMITHING
    }

    /**
     * One recipe card. {@code cells} are item ids ({@code minecraft:chest}) or tags ({@code #minecraft:planks}), nine for a
     * crafting grid (row by row, null for an empty cell) and three for smithing. {@code verify} is the real recipe this card
     * is checked against by a game test.
     */
    public record Recipe(String id, Kind kind, List<String> cells, String result, int count, String note, String verify) {
        public int rows() {
            return kind == Kind.SMITHING ? 1 : 3;
        }

        public String nameKey() {
            return "guide.storagenexus.recipe." + id;
        }

        public String noteKey() {
            return "guide.storagenexus.note." + note;
        }
    }

    public Chapter chapter(String id) {
        for (Chapter c : chapters) if (c.id().equals(id)) return c;
        throw new IllegalArgumentException("No guide chapter " + id);
    }

    public static GuideData parse(JsonObject chaptersJson, JsonObject recipesJson) {
        Map<String, Recipe> recipes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : recipesJson.getAsJsonObject("recipes").entrySet()) {
            recipes.put(e.getKey(), parseRecipe(e.getKey(), e.getValue().getAsJsonObject()));
        }
        List<Chapter> chapters = new ArrayList<>();
        for (JsonElement ce : chaptersJson.getAsJsonArray("chapters")) {
            JsonObject c = ce.getAsJsonObject();
            List<Page> pages = new ArrayList<>();
            for (JsonElement pe : c.getAsJsonArray("pages")) pages.add(parsePage(pe.getAsJsonObject()));
            chapters.add(new Chapter(c.get("id").getAsString(), c.get("cover").getAsString(), List.copyOf(pages)));
        }
        return new GuideData(List.copyOf(chapters), Collections.unmodifiableMap(recipes));
    }

    private static Page parsePage(JsonObject p) {
        return switch (p.get("type").getAsString()) {
            case "text" -> new TextPage(p.get("id").getAsString());
            case "contents" -> new ContentsPage(strings(p.getAsJsonArray("entries")));
            case "recipes" -> new RecipeSheet(p.get("id").getAsString(), strings(p.getAsJsonArray("recipes")));
            default -> throw new IllegalArgumentException("Unknown guide page type " + p.get("type"));
        };
    }

    private static Recipe parseRecipe(String id, JsonObject r) {
        Kind kind = Kind.valueOf(r.get("type").getAsString().toUpperCase(Locale.ROOT));
        List<String> cells = new ArrayList<>();
        switch (kind) {
            case SHAPED -> {
                Map<Character, String> key = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> e : r.getAsJsonObject("key").entrySet()) {
                    key.put(e.getKey().charAt(0), e.getValue().getAsString());
                }
                for (int row = 0; row < 3; row++) {
                    String line = row < r.getAsJsonArray("pattern").size() ? r.getAsJsonArray("pattern").get(row).getAsString() : "";
                    for (int col = 0; col < 3; col++) {
                        char c = col < line.length() ? line.charAt(col) : ' ';
                        cells.add(c == ' ' ? null : key.get(c));
                    }
                }
            }
            case SHAPELESS -> {
                cells.addAll(strings(r.getAsJsonArray("ingredients")));
                while (cells.size() < 9) cells.add(null);
            }
            case SMITHING -> {
                cells.add(r.get("template").getAsString());
                cells.add(r.get("base").getAsString());
                cells.add(r.get("addition").getAsString());
            }
        }
        JsonObject result = r.getAsJsonObject("result");
        String resultId = result.get("id").getAsString();
        int count = result.has("count") ? result.get("count").getAsInt() : 1;
        String note = r.has("note") ? r.get("note").getAsString() : kind == Kind.SHAPELESS ? "shapeless" : null;
        String verify = r.has("verify") ? r.get("verify").getAsString() : resultId;
        return new Recipe(id, kind, Collections.unmodifiableList(cells), resultId, count, note, verify);
    }

    private static List<String> strings(JsonArray array) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : array) out.add(e.getAsString());
        return out;
    }
}
