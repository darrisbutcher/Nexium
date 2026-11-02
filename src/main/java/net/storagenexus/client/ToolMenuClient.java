package net.storagenexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.client.screen.ToolMenuScreen;
import net.storagenexus.essentials.client.NexusEssentialsClient;

/** The tool menu key: G by default, listed under Storage Nexus in Controls > Key Binds next to the magnet's. */
public final class ToolMenuClient {
    public static final KeyMapping TOOL_MENU = new KeyMapping("key.storagenexus.tool_menu",
            InputConstants.KEY_G, NexusEssentialsClient.CATEGORY);

    public static void init() {
        KeyMappingHelper.registerKeyMapping(TOOL_MENU);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOOL_MENU.consumeClick()) {
                if (client.player == null || client.gui.screen() != null) continue;
                if (ToolMenu.appliesTo(client.player.getMainHandItem())) {
                    client.gui.setScreen(new ToolMenuScreen());
                } else {
                    client.player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.hold_tool")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        });
    }

    private ToolMenuClient() {}
}
