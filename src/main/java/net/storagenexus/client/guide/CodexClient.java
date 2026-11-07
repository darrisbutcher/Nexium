package net.storagenexus.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.essentials.client.NexusEssentialsClient;

/**
 * The Codex lookup key (H by default; G is the tool menu): in any inventory, press it over an item to open that item's
 * page, and close the Codex to come back. Outside a screen it opens the Codex. Items with a page say so in their tooltip.
 */
public final class CodexClient {
    public static final KeyMapping LOOKUP = new KeyMapping("key.storagenexus.codex_lookup", InputConstants.KEY_H, NexusEssentialsClient.CATEGORY);

    public static void init() {
        KeyMappingHelper.registerKeyMapping(LOOKUP);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> {
                if (!LOOKUP.matches(event) || s.getFocused() instanceof EditBox) return;
                Slot slot = container.hoveredSlot;
                if (slot != null && slot.hasItem()) GuideScreens.lookup(slot.getItem(), s);
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (LOOKUP.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) GuideScreens.open();
            }
        });
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (GuideScreens.topicFor(stack) != null) {
                lines.add(Component.translatable("tooltip.storagenexus.codex", LOOKUP.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }

    private CodexClient() {}
}
