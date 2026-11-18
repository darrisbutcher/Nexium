package net.nexium.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.nexium.client.screen.ConfigScreen;

/** Adds a Configure button to Nexium in Mod Menu's list. Only loaded when Mod Menu is installed. */
public class NexiumModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
