package net.storagenexus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.storagenexus.essentials.client.NexusEssentialsClient;

/** The key that opens the Nexium settings screen; not bound to anything until the player picks one. */
public final class ConfigKey {
    public static final KeyMapping OPEN = new KeyMapping("key.nexium.open_settings",
            InputConstants.UNKNOWN.getValue(), NexusEssentialsClient.CATEGORY);

    public static final KeyMapping QUESTS = new KeyMapping("key.nexium.open_quests",
            InputConstants.UNKNOWN.getValue(), NexusEssentialsClient.CATEGORY);

    private ConfigKey() {}

    public static void init() {
        KeyMappingHelper.registerKeyMapping(OPEN);
        KeyMappingHelper.registerKeyMapping(QUESTS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new ConfigScreen(null));
            }
            while (QUESTS.consumeClick()) {
                if (client.gui.screen() == null && client.player != null) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.storagenexus.quest.QuestRequestPayload(true));
                }
            }
        });
    }
}
