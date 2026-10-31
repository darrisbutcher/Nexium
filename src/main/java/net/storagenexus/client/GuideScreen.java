package net.storagenexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Opens the guide in the vanilla book screen, one page per lang key guide.storagenexus.page.N. */
public final class GuideScreen {
    public static void open() {
        List<Component> pages = new ArrayList<>();
        for (int i = 1; Language.getInstance().has("guide.storagenexus.page." + i); i++) {
            pages.add(Component.translatable("guide.storagenexus.page." + i));
        }
        Minecraft.getInstance().gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }

    private GuideScreen() {}
}
