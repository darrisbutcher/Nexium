package net.storagenexus.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens the guide in the vanilla book screen, one page per lang key guide.storagenexus.page.N.
 * A page's first line is its heading. Styling is done here rather than with § codes, because
 * a § reset in the text would also drop the book's black, shadowless text style.
 */
public final class GuideScreen {
    public static void open() {
        Language language = Language.getInstance();
        List<Component> pages = new ArrayList<>();
        for (int i = 1; language.has("guide.storagenexus.page." + i); i++) {
            String text = language.getOrDefault("guide.storagenexus.page." + i);
            int split = text.indexOf('\n');
            String heading = split < 0 ? text : text.substring(0, split);
            String body = split < 0 ? "" : text.substring(split);
            pages.add(Component.literal(heading).withStyle(ChatFormatting.BOLD).append(Component.literal(body)
                    .withStyle(s -> s.withBold(false))));
        }
        Minecraft.getInstance().gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }

    private GuideScreen() {}
}
