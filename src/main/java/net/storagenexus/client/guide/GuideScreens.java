package net.storagenexus.client.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.guide.GuideData;

import java.io.IOException;
import java.io.Reader;

/** Opens the Nexium Codex, and finds the topic for an item (the lookup key and tooltips use it). */
public final class GuideScreens {
    private static final Identifier CODEX = Identifier.fromNamespaceAndPath("storagenexus", "codex/codex.json");
    private static final Identifier RECIPES = Identifier.fromNamespaceAndPath("storagenexus", "guide/recipes.json");
    private static GuideData cached;

    /** Opens the Codex where it was last left (the guide item, and the lookup key out of a screen). */
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        cached = load(minecraft.getResourceManager());
        minecraft.gui.setScreen(new CodexScreen(cached, null, minecraft.gui.screen()));
    }

    /** Opens the Codex on this item's topic, coming back to {@code parent} when closed. Returns false if it has none. */
    public static boolean lookup(ItemStack stack, Screen parent) {
        String topic = topicFor(stack);
        if (topic == null) return false;
        Minecraft.getInstance().gui.setScreen(new CodexScreen(data(), topic, parent));
        return true;
    }

    public static GuideData data() {
        if (cached == null) cached = load(Minecraft.getInstance().getResourceManager());
        return cached;
    }

    /**
     * The topic for an item: one that lists it (or one of its tags) first, then one whose icon it is. Vanilla items are only
     * found when a topic lists them, so a diamond doesn't open the AIOT page.
     */
    public static String topicFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        GuideData data = data();
        for (GuideData.Topic t : data.topics().values()) {
            for (String ref : t.items()) if (matches(stack, ref)) return t.id();
        }
        for (GuideData.Topic t : data.topics().values()) {
            if (!t.icon().startsWith("minecraft:") && matches(stack, t.icon())) return t.id();
        }
        return null;
    }

    private static boolean matches(ItemStack stack, String ref) {
        if (ref.startsWith("#")) return stack.is(TagKey.create(Registries.ITEM, Identifier.parse(ref.substring(1))));
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(ref);
    }

    /** Reads the Codex and its recipe cards from the mod's assets. */
    public static GuideData load(ResourceManager resources) {
        return GuideData.parse(read(resources, CODEX), read(resources, RECIPES));
    }

    private static JsonObject read(ResourceManager resources, Identifier id) {
        try (Reader reader = resources.openAsReader(id)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read the Codex's " + id, e);
        }
    }

    private GuideScreens() {}
}
