package net.storagenexus.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.StorageNexus;
import net.storagenexus.drawer.DrawerBlock;

import java.util.List;

/**
 * Adds a short "what is this for" description under every Storage Nexus item, from the
 * lang keys {@code <item>.desc}, {@code <item>.desc2}, ... Wooden drawers share one
 * description per size.
 */
public final class ItemDescriptions {
    private static final int MAX_LINES = 4;

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> append(stack, lines));
    }

    private static void append(ItemStack stack, List<Component> lines) {
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(StorageNexus.MOD_ID)) return;
        String key = stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof DrawerBlock drawer
                ? "tooltip.storagenexus.drawer." + drawer.slots()
                : stack.getItem().getDescriptionId() + ".desc";
        Language language = Language.getInstance();
        for (int i = 1; i <= MAX_LINES; i++) {
            String lineKey = i == 1 ? key : key + i;
            if (!language.has(lineKey)) break;
            lines.add(Component.translatable(lineKey).withStyle(ChatFormatting.GRAY));
        }
    }

    private ItemDescriptions() {}
}
