package net.nexium.quest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.nexium.Nexium;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The quests, read from {@code data/nexium/quests.json} in the mod: chapters in order, each a list of quests with a goal (an item, and
 * optionally an item tag whose members count too).
 * Both the server (which checks the goals) and the client (which draws the book) read the same file. Nothing is locked: every chapter
 * and quest is open from the start. Names and descriptions come from the language file:
 * {@code quest.nexium.<chapter>.<quest>} and {@code .desc}.
 */
public final class QuestBook {
    /** Make something (the crafting statistic, or having it counts) or use it (placing a block counts). */
    public enum Goal { GET, USE }

    /**
     * {@code item} is the item the quest names. When {@code tag} is set, any item in that tag counts as well (a Drawer of any wood, say),
     * and {@code item} is the one drawn in the book and the one a test hands over.
     */
    public record Quest(String chapter, String id, Goal goal, Item item, @Nullable TagKey<Item> tag, int count) {
        /** Every item that counts for this quest: the named one, then whatever else its tag holds (a tag is only known once a world is loaded). */
        public List<Item> items() {
            List<Item> out = new ArrayList<>();
            out.add(item);
            if (tag != null) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    if (!out.contains(holder.value())) out.add(holder.value());
                }
            }
            return out;
        }

        /** The key a quest is saved and sent under. */
        public String key() {
            return chapter + "." + id;
        }

        public String nameKey() {
            return "quest.nexium." + chapter + "." + id;
        }

        public String descKey() {
            return nameKey() + ".desc";
        }
    }

    /** {@code tier} is how many random Nexium items finishing a quest of this chapter gives. */
    public record Chapter(String id, Item icon, int tier, List<Quest> quests) {
        public String nameKey() {
            return "quest.nexium.chapter." + id;
        }

        public String descKey() {
            return nameKey() + ".desc";
        }
    }

    private static List<Chapter> chapters;

    /** The chapters in order, read once. */
    public static synchronized List<Chapter> chapters() {
        if (chapters == null) chapters = load();
        return chapters;
    }

    public static Quest find(String key) {
        for (Chapter c : chapters()) for (Quest q : c.quests()) if (q.key().equals(key)) return q;
        return null;
    }

    public static int totalQuests() {
        int n = 0;
        for (Chapter c : chapters()) n += c.quests().size();
        return n;
    }

    private static List<Chapter> load() {
        List<Chapter> out = new ArrayList<>();
        try (InputStream in = QuestBook.class.getResourceAsStream("/data/nexium/quests.json")) {
            if (in == null) throw new IOException("quests.json is missing");
            JsonObject root = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            for (var ce : root.getAsJsonArray("chapters")) {
                JsonObject c = ce.getAsJsonObject();
                String chapterId = c.get("id").getAsString();
                List<Quest> quests = new ArrayList<>();
                for (var qe : c.getAsJsonArray("quests")) {
                    JsonObject q = qe.getAsJsonObject();
                    // An optional "tag" lets any item in that item tag count as well as the named item
                    TagKey<Item> tag = q.has("tag") ? TagKey.create(Registries.ITEM, Identifier.parse(q.get("tag").getAsString())) : null;
                    quests.add(new Quest(chapterId, q.get("id").getAsString(), Goal.valueOf(q.get("goal").getAsString().toUpperCase(java.util.Locale.ROOT)),
                            item(q.get("item").getAsString()), tag, q.get("count").getAsInt()));
                }
                out.add(new Chapter(chapterId, item(c.get("icon").getAsString()), c.get("tier").getAsInt(), List.copyOf(quests)));
            }
        } catch (IOException | JsonParseException | IllegalArgumentException | NullPointerException e) {
            Nexium.LOG.error("Could not read the quests", e);
        }
        return List.copyOf(out);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private QuestBook() {}
}
