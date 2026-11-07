package net.storagenexus.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.storagenexus.guide.GuideData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Nexium Codex's data: every topic has its text, every link, recipe and item id points at something, every item in the
 * mod has a page, and every recipe card shows exactly what the real recipe file says, so the Codex can't go out of date.
 */
public class GuideGameTest {
    private static JsonObject json(String path) {
        try (InputStream in = GuideGameTest.class.getResourceAsStream(path)) {
            if (in == null) throw new AssertionError("Missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("Couldn't read " + path, e);
        }
    }

    private static GuideData data() {
        return GuideData.parse(json("/assets/storagenexus/codex/codex.json"), json("/assets/storagenexus/guide/recipes.json"));
    }

    private static boolean exists(String path) {
        return GuideGameTest.class.getResource(path) != null;
    }

    @GameTest
    public void everyTopicHasItsText(GameTestHelper h) {
        GuideData data = data();
        JsonObject lang = json("/assets/storagenexus/lang/en_us.json");
        h.assertTrue(data.categories().size() == 11, "the Codex has 11 categories, found " + data.categories().size());
        java.util.Set<String> listed = new java.util.HashSet<>();
        for (GuideData.Category category : data.categories()) {
            requireText(h, lang, category.titleKey());
            requireItem(h, category.icon(), "category " + category.id());
            listed.addAll(category.topics());
        }
        java.util.Set<String> usedPages = new java.util.HashSet<>();
        for (GuideData.Topic topic : data.topics().values()) {
            h.assertTrue(listed.contains(topic.id()), "topic " + topic.id() + " is in a category");
            for (String page : topic.pages()) {
                String key = GuideData.Topic.pageKey(page);
                String full = requireText(h, lang, key);
                int split = full.indexOf('\n');
                h.assertTrue(split > 0 && !full.substring(split + 1).isBlank(), key + " is a heading, a line break and some text");
                usedPages.add(key);
            }
            for (String id : topic.recipes()) h.assertTrue(data.recipes().containsKey(id), topic.id() + " uses unknown recipe " + id);
            for (String id : topic.see()) h.assertTrue(data.topics().containsKey(id), topic.id() + " sees unknown topic " + id);
            requireItem(h, topic.icon(), "topic " + topic.id());
            for (String ref : topic.items()) requireItem(h, ref, "topic " + topic.id());
        }
        for (String key : lang.keySet()) {
            // page keys are guide.storagenexus.<chapter>.<page>; recipe names, notes and one-word UI strings are not pages
            if (!key.startsWith("guide.storagenexus.") || key.startsWith("guide.storagenexus.recipe.") || key.startsWith("guide.storagenexus.note.")
                    || key.substring("guide.storagenexus.".length()).split("\\.").length != 2) continue;
            h.assertTrue(usedPages.contains(key), key + " is shown in a Codex topic");
        }
        for (GuideData.Recipe recipe : data.recipes().values()) {
            requireText(h, lang, recipe.nameKey());
            if (recipe.note() != null) requireText(h, lang, recipe.noteKey());
        }
        for (String key : List.of("screen.storagenexus.codex", "screen.storagenexus.codex.search", "screen.storagenexus.codex.tip",
                "screen.storagenexus.codex.see_also", "screen.storagenexus.codex.recipes", "key.storagenexus.codex_lookup", "tooltip.storagenexus.codex")) {
            requireText(h, lang, key);
        }
        h.assertTrue(exists("/assets/storagenexus/textures/item/guide.png"), "the guide item has its own icon");
        h.succeed();
    }

    /** Every item of the mod (both namespaces) opens a page with the lookup key: no new item can be left out of the Codex. */
    @GameTest
    public void everyItemHasAPage(GameTestHelper h) {
        GuideData data = data();
        List<String> missing = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("storagenexus") && !id.getNamespace().equals("nexusessentials")) continue;
            boolean found = false;
            for (GuideData.Topic topic : data.topics().values()) {
                for (String ref : topic.items()) found |= refers(ref, item);
                found |= refers(topic.icon(), item);
            }
            if (!found) missing.add(id.toString());
        }
        h.assertTrue(missing.isEmpty(), "items with no Codex page: " + missing);
        h.succeed();
    }

    private static boolean refers(String ref, Item item) {
        if (ref.startsWith("#")) return item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, Identifier.parse(ref.substring(1))));
        return BuiltInRegistries.ITEM.getKey(item).toString().equals(ref);
    }

    private static void requireItem(GameTestHelper h, String ref, String where) {
        if (ref.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(ref.substring(1)));
            h.assertTrue(BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator().hasNext(), where + ": tag " + ref + " has items");
        } else {
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(ref)), where + ": unknown item " + ref);
        }
    }

    private static String requireText(GameTestHelper h, JsonObject lang, String key) {
        h.assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), "missing lang text " + key);
        return lang.get(key).getAsString();
    }

    @GameTest
    public void everyRecipeCardUsesRealItems(GameTestHelper h) {
        for (GuideData.Recipe recipe : data().recipes().values()) {
            for (String ref : recipe.cells()) {
                if (ref == null) continue;
                if (ref.startsWith("#")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(ref.substring(1)));
                    h.assertTrue(BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator().hasNext(), recipe.id() + ": tag " + ref + " has items");
                } else {
                    h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(ref)), recipe.id() + ": unknown item " + ref);
                }
            }
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(recipe.result())), recipe.id() + ": unknown result " + recipe.result());
        }
        h.succeed();
    }

    @GameTest
    public void recipeCardsMatchTheRealRecipes(GameTestHelper h) {
        for (GuideData.Recipe card : data().recipes().values()) {
            Identifier verify = Identifier.parse(card.verify());
            JsonObject real = json("/data/" + verify.getNamespace() + "/recipe/" + verify.getPath() + ".json");
            String type = real.get("type").getAsString();

            JsonElement result = real.get("result");
            String resultId = result.isJsonObject() ? result.getAsJsonObject().get("id").getAsString() : result.getAsString();
            int count = result.isJsonObject() && result.getAsJsonObject().has("count") ? result.getAsJsonObject().get("count").getAsInt() : 1;
            h.assertTrue(resultId.equals(card.result()) && count == card.count(),
                    card.id() + ": the card shows " + card.count() + " x " + card.result() + ", the recipe makes " + count + " x " + resultId);

            switch (card.kind()) {
                case SHAPED -> {
                    h.assertTrue(type.equals("minecraft:crafting_shaped"), card.id() + " is shaped but " + verify + " is " + type);
                    Map<String, JsonElement> key = real.getAsJsonObject("key").asMap();
                    JsonArray pattern = real.getAsJsonArray("pattern");
                    for (int row = 0; row < 3; row++) {
                        String line = row < pattern.size() ? pattern.get(row).getAsString() : "";
                        for (int col = 0; col < 3; col++) {
                            char c = col < line.length() ? line.charAt(col) : ' ';
                            String expected = c == ' ' ? null : ingredient(key.get(String.valueOf(c)));
                            String shown = card.cells().get(row * 3 + col);
                            h.assertTrue(sameIngredient(shown, expected),
                                    card.id() + " cell " + row + "," + col + ": the card shows " + shown + ", the recipe wants " + expected);
                        }
                    }
                }
                case SHAPELESS -> {
                    h.assertTrue(type.equals("minecraft:crafting_shapeless") || type.equals("storagenexus:aiot_merge"),
                            card.id() + " is shapeless but " + verify + " is " + type);
                    List<String> wanted = new ArrayList<>();
                    for (JsonElement e : real.getAsJsonArray("ingredients")) wanted.add(ingredient(e));
                    List<String> shown = card.cells().stream().filter(c -> c != null).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                    h.assertTrue(shown.size() == wanted.size(), card.id() + ": the card shows " + shown.size() + " ingredients, the recipe wants " + wanted.size());
                    for (String ref : shown) {
                        Optional<String> match = wanted.stream().filter(w -> sameIngredient(ref, w)).findFirst();
                        h.assertTrue(match.isPresent(), card.id() + ": the recipe has no " + ref);
                        wanted.remove(match.get());
                    }
                }
                case SMITHING -> {
                    h.assertTrue(type.equals("minecraft:smithing_transform"), card.id() + " is smithing but " + verify + " is " + type);
                    List<String> wanted = List.of(ingredient(real.get("template")), ingredient(real.get("base")), ingredient(real.get("addition")));
                    for (int i = 0; i < 3; i++) {
                        h.assertTrue(sameIngredient(card.cells().get(i), wanted.get(i)),
                                card.id() + " smithing slot " + i + ": the card shows " + card.cells().get(i) + ", the recipe wants " + wanted.get(i));
                    }
                }
            }
        }
        h.succeed();
    }

    /** An ingredient as the guide writes it: an item id, or {@code #tag}. */
    private static String ingredient(JsonElement element) {
        if (element.isJsonPrimitive()) return element.getAsString();
        JsonObject object = element.getAsJsonObject();
        return object.has("item") ? object.get("item").getAsString() : "#" + object.get("tag").getAsString();
    }

    /** The same ingredient, or a card that shows a tag where the real recipe names one of its items. */
    private static boolean sameIngredient(String shown, String real) {
        if (shown == null || real == null) return shown == null && real == null;
        if (shown.equals(real)) return true;
        if (!shown.startsWith("#") || real.startsWith("#")) return false;
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(shown.substring(1)));
        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(Identifier.parse(real));
        return item.isPresent() && item.get().is(tag);
    }
}
