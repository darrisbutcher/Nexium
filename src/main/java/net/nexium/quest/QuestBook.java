package net.nexium.quest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.nexium.Nexium;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The quests, read from {@code data/nexium/quests.json} in the mod: chapters in order, each a list of quests with a goal.
 * Both the server (which checks the goals) and the client (which draws the book) read the same file. A chapter is open once every
 * quest of the one before it is done. Names and descriptions come from the language file:
 * {@code quest.nexium.<chapter>.<quest>} and {@code .desc}.
 */
public final class QuestBook {
    /** Make something (the crafting statistic, or having it counts) or use it (placing a block counts). */
    public enum Goal { GET, USE }

    public record Quest(String chapter, String id, Goal goal, Item item, int count) {
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
                    quests.add(new Quest(chapterId, q.get("id").getAsString(), Goal.valueOf(q.get("goal").getAsString().toUpperCase(java.util.Locale.ROOT)),
                            item(q.get("item").getAsString()), q.get("count").getAsInt()));
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
