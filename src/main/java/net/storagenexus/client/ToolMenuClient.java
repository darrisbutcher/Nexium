package net.storagenexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.aiot.ToolMenuPayload;
import net.storagenexus.aiot.module.AreaTogglePayload;
import net.storagenexus.aiot.module.VeinKeyPayload;
import net.storagenexus.client.screen.ArmorMenuScreen;
import net.storagenexus.client.screen.ToolMenuScreen;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.storagenexus.nexium.NexiumArmor;
import net.storagenexus.registry.ModMenus;

/**
 * The tool menu key: G by default, listed under Nexium in Controls > Key Binds next to the magnet's. Holding an
 * AIOT or a Nexium tool, it asks the server for the tool menu (which has slots for modules); otherwise, wearing Nexium
 * armor, it opens the armor menu.
 */
public final class ToolMenuClient {
    public static final KeyMapping TOOL_MENU = new KeyMapping("key.storagenexus.tool_menu",
            InputConstants.KEY_G, NexusEssentialsClient.CATEGORY);

    /** Hold while mining to vein mine (with a Vein Mining module in the AIOT). */
    public static final KeyMapping VEIN_MINE = new KeyMapping("key.storagenexus.vein_mine",
            InputConstants.KEY_V, NexusEssentialsClient.CATEGORY);
    private static boolean veinSent;

    /** Press with an AIOT in hand to switch its 3x3 Mining Module on or off. */
    public static final KeyMapping TOGGLE_AREA = new KeyMapping("key.storagenexus.toggle_area",
            InputConstants.KEY_X, NexusEssentialsClient.CATEGORY);

    public static void init() {
        KeyMappingHelper.registerKeyMapping(VEIN_MINE);
        KeyMappingHelper.registerKeyMapping(TOGGLE_AREA);
        KeyMappingHelper.registerKeyMapping(TOOL_MENU);
        MenuScreens.register(ModMenus.TOOL_MENU, ToolMenuScreen::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Tell the server when the vein key goes down or up
            boolean veinDown = client.player != null && VEIN_MINE.isDown();
            if (veinDown != veinSent) {
                veinSent = veinDown;
                if (client.player != null) ClientPlayNetworking.send(new VeinKeyPayload(veinDown));
            }
            while (TOGGLE_AREA.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(AreaTogglePayload.INSTANCE);
            }
            while (TOOL_MENU.consumeClick()) {
                if (client.player == null || client.gui.screen() != null) continue;
                if (ToolMenu.appliesTo(client.player.getMainHandItem())) {
                    ClientPlayNetworking.send(ToolMenuPayload.INSTANCE);
                } else if (NexiumArmor.piecesWorn(client.player) > 0) {
                    client.gui.setScreen(new ArmorMenuScreen());
                } else {
                    client.player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.hold_tool")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        });
    }

    private ToolMenuClient() {}
}
