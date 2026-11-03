package net.storagenexus.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The Nexium advancement tab (data/storagenexus/advancement/guide): every node loads, has a title and
 * description in the lang file, is granted by the tick trigger, and sits at most two levels under the
 * root. Every node is unlocked from the start, but the depth limit keeps the tree readable.
 */
public class AdvancementTabGameTest {
    private static final String PREFIX = "guide/";

    private static List<AdvancementHolder> tab(GameTestHelper h) {
        ServerAdvancementManager manager = h.getLevel().getServer().getAdvancements();
        List<AdvancementHolder> nodes = new ArrayList<>();
        for (AdvancementHolder a : manager.getAllAdvancements()) {
            if (a.id().getNamespace().equals("storagenexus") && a.id().getPath().startsWith(PREFIX)) nodes.add(a);
        }
        return nodes;
    }

    private static JsonObject lang() {
        var in = AdvancementTabGameTest.class.getResourceAsStream("/assets/storagenexus/lang/en_us.json");
        return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @GameTest
    public void everyNodeIsThereWithItsText(GameTestHelper h) {
        List<AdvancementHolder> nodes = tab(h);
        h.assertTrue(nodes.size() == 33, "the tab has 33 nodes, found " + nodes.size());
        JsonObject lang = lang();
        for (AdvancementHolder a : nodes) {
            var display = a.value().display().orElse(null);
            h.assertTrue(display != null, a.id() + " has a display");
            for (var c : List.of(display.title(), display.description())) {
                h.assertTrue(c.getContents() instanceof TranslatableContents, a.id() + " text is translatable");
                String key = ((TranslatableContents) c.getContents()).getKey();
                h.assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), a.id() + " lacks lang key " + key);
            }
            h.assertTrue(!display.showToast() && !display.announceToChat() && !display.hidden(), a.id() + " must not pop a toast or chat line");
            h.assertTrue(a.value().criteria().containsKey("unlocked"), a.id() + " unlocks at the start");
        }
        h.succeed();
    }

    @GameTest
    public void oneRootAndNothingDeeperThanTwo(GameTestHelper h) {
        List<AdvancementHolder> nodes = tab(h);
        Identifier root = Identifier.fromNamespaceAndPath("storagenexus", PREFIX + "root");
        int roots = 0;
        for (AdvancementHolder a : nodes) {
            if (a.value().parent().isEmpty()) {
                roots++;
                h.assertTrue(a.id().equals(root), "the only root is guide/root, found " + a.id());
                h.assertTrue(a.value().display().get().background().isPresent(), "the root has the tab background");
                continue;
            }
            Identifier parent = a.value().parent().get();
            h.assertTrue(parent.equals(root) || tab(h).stream().anyMatch(p -> p.id().equals(parent)
                    && p.value().parent().filter(root::equals).isPresent()),
                    a.id() + " is more than two levels under the root");
        }
        h.assertTrue(roots == 1, "exactly one root, found " + roots);
        h.succeed();
    }
}
