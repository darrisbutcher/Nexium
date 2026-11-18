package net.storagenexus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.storagenexus.essentials.client.NexusEssentialsClient;

/** The key that opens the Nexium settings screen; not bound to anything until the player picks one. */
public final class ConfigKey {
    public static final KeyMapping OPEN = new KeyMapping("key.storagenexus.open_settings",
            InputConstants.UNKNOWN.getValue(), NexusEssentialsClient.CATEGORY);

    private ConfigKey() {}

    public static void init() {
        KeyMappingHelper.registerKeyMapping(OPEN);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new ConfigScreen(null));
            }
        });
    }
}
