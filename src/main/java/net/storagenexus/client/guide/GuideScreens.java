package net.storagenexus.client.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.storagenexus.guide.GuideData;

import java.io.IOException;
import java.io.Reader;

/** Opens the Nexium Guide: a chapter picker, and each chapter as a book. */
public final class GuideScreens {
    private static final Identifier CHAPTERS = Identifier.fromNamespaceAndPath("storagenexus", "guide/chapters.json");
    private static final Identifier RECIPES = Identifier.fromNamespaceAndPath("storagenexus", "guide/recipes.json");

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new GuideChaptersScreen(load(minecraft.getResourceManager())));
    }

    /** Reads the chapters and recipes from the mod's assets. */
    public static GuideData load(ResourceManager resources) {
        return GuideData.parse(read(resources, CHAPTERS), read(resources, RECIPES));
    }

    private static JsonObject read(ResourceManager resources, Identifier id) {
        try (Reader reader = resources.openAsReader(id)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read the guide's " + id, e);
        }
    }

    private GuideScreens() {}
}
