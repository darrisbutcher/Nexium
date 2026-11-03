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
 * The Nexium Guide's data: every page has its text, every link and recipe id points at something, every cover exists, and
 * every recipe card shows exactly what the real recipe file says, so the book can't go out of date. (That every page fits the
 * book's 14 lines needs the real font, so it is checked by the client test.)
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
        return GuideData.parse(json("/assets/storagenexus/guide/chapters.json"), json("/assets/storagenexus/guide/recipes.json"));
    }

    private static boolean exists(String path) {
        return GuideGameTest.class.getResource(path) != null;
    }

    @GameTest
    public void everyPageHasItsText(GameTestHelper h) {
        GuideData data = data();
        JsonObject lang = json("/assets/storagenexus/lang/en_us.json");
        h.assertTrue(data.chapters().size() == 9, "the guide has 9 chapters, found " + data.chapters().size());
        for (GuideData.Chapter chapter : data.chapters()) {
            requireText(h, lang, chapter.titleKey());
            requireText(h, lang, chapter.topicsKey());
            for (GuideData.Page page : chapter.pages()) {
                switch (page) {
                    case GuideData.TextPage text -> {
                        String full = requireText(h, lang, chapter.pageKey(text.id()));
                        int split = full.indexOf('\n');
                        h.assertTrue(split > 0 && !full.substring(split + 1).isBlank(),
                                chapter.pageKey(text.id()) + " is a heading, a line break and some text");
                    }
                    case GuideData.ContentsPage contents -> {
                        for (String target : contents.targets()) {
                            h.assertTrue(chapter.indexOf(target) >= 0, chapter.id() + " contents link to missing page " + target);
                            requireText(h, lang, chapter.contentsKey(target));
                        }
                    }
                    case GuideData.RecipeSheet sheet -> {
                        h.assertTrue(!sheet.recipeIds().isEmpty() && sheet.recipeIds().size() <= 2,
                                chapter.id() + " " + sheet.id() + " holds one or two recipes");
                        for (String id : sheet.recipeIds()) {
                            h.assertTrue(data.recipes().containsKey(id), chapter.id() + " uses unknown recipe " + id);
                        }
                    }
                }
            }
            h.assertTrue(exists("/assets/storagenexus/textures/gui/sprites/guide/cover_" + chapter.cover() + ".png"),
                    chapter.id() + " has a cover sprite");
        }
        for (GuideData.Recipe recipe : data.recipes().values()) {
            requireText(h, lang, recipe.nameKey());
            if (recipe.note() != null) requireText(h, lang, recipe.noteKey());
        }
        h.assertTrue(lang.keySet().stream().noneMatch(k -> k.startsWith("guide.storagenexus.page.")), "the old one-book pages are gone");
        h.assertTrue(exists("/assets/storagenexus/textures/item/guide.png"), "the guide item has its own icon");
        h.succeed();
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
