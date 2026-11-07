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
 * The Nexium Codex: its categories and topics ({@code assets/storagenexus/codex/codex.json}) and the recipe cards topics show
 * ({@code assets/storagenexus/guide/recipes.json}). Text is not in here: a topic is made of text pages, and every page is a
 * lang key ({@code guide.storagenexus.<page>}) whose first line is its heading, so wording lives in the lang file like
 * everything else. The first page's heading is the topic's title.
 */
public record GuideData(List<Category> categories, Map<String, Topic> topics, Map<String, Recipe> recipes) {

    /** A group in the Codex's sidebar. A topic may sit in more than one category. */
    public record Category(String id, String icon, List<String> topics) {
        public String titleKey() {
            return "codex.storagenexus.category." + id;
        }
    }

    /**
     * One page of the Codex. {@code pages} are lang keys without the {@code guide.storagenexus.} prefix; {@code items} are
     * the items (or {@code #tags}) whose lookup key opens this topic, besides {@code icon}; {@code aliases} are other words
     * that link here from other topics' text, besides the title; {@code see} are the "See also" topics.
     */
    public record Topic(String id, List<String> pages, String icon, List<String> recipes, List<String> items,
                        List<String> aliases, List<String> see) {
        public static String pageKey(String page) {
            return "guide.storagenexus." + page;
        }

        /** The lang key of the first page, whose heading is the topic's title. */
        public String titleKey() {
            return pageKey(pages.getFirst());
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

    public Topic topic(String id) {
        Topic topic = topics.get(id);
        if (topic == null) throw new IllegalArgumentException("No Codex topic " + id);
        return topic;
    }

    /** The first category that lists this topic (for the sidebar and the breadcrumb), or null. */
    public Category categoryOf(String topicId) {
        for (Category c : categories) if (c.topics().contains(topicId)) return c;
        return null;
    }

    /** The topic the Codex opens on. */
    public String home() {
        return categories.getFirst().topics().getFirst();
    }

    public static GuideData parse(JsonObject codexJson, JsonObject recipesJson) {
        Map<String, Recipe> recipes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : recipesJson.getAsJsonObject("recipes").entrySet()) {
            recipes.put(e.getKey(), parseRecipe(e.getKey(), e.getValue().getAsJsonObject()));
        }
        Map<String, Topic> topics = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : codexJson.getAsJsonObject("topics").entrySet()) {
            JsonObject t = e.getValue().getAsJsonObject();
            List<String> pages = strings(t.getAsJsonArray("pages"));
            if (pages.isEmpty()) throw new IllegalArgumentException("Codex topic " + e.getKey() + " has no pages");
            topics.put(e.getKey(), new Topic(e.getKey(), pages, t.get("icon").getAsString(), optional(t, "recipes"),
                    optional(t, "items"), optional(t, "aliases"), optional(t, "see")));
        }
        List<Category> categories = new ArrayList<>();
        for (JsonElement ce : codexJson.getAsJsonArray("categories")) {
            JsonObject c = ce.getAsJsonObject();
            List<String> list = strings(c.getAsJsonArray("topics"));
            for (String id : list) {
                if (!topics.containsKey(id)) throw new IllegalArgumentException("Codex category " + c.get("id") + " lists unknown topic " + id);
            }
            categories.add(new Category(c.get("id").getAsString(), c.get("icon").getAsString(), list));
        }
        if (categories.isEmpty() || categories.getFirst().topics().isEmpty()) throw new IllegalArgumentException("The Codex has no topics");
        return new GuideData(List.copyOf(categories), Collections.unmodifiableMap(topics), Collections.unmodifiableMap(recipes));
    }

    private static List<String> optional(JsonObject object, String key) {
        return object.has(key) ? strings(object.getAsJsonArray(key)) : List.of();
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
