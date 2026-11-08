package net.storagenexus.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.StorageNexus;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.drawer.DrawerBlock;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.registry.ModComponents;

import java.util.List;

/**
 * Adds a short "what is this for" description under every Nexium item, from the
 * lang keys {@code <item>.desc}, {@code <item>.desc2}, ... and a red {@code <item>.warn}. Wooden
 * drawers share one description per size, the AIOTs one for every material and the Construction Builders one for
 * every tier.
 */
public final class ItemDescriptions {
    private static final int MAX_LINES = 4;

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> append(stack, lines));
    }

    private static void append(ItemStack stack, List<Component> lines) {
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(StorageNexus.MOD_ID)) return;
        String key;
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof DrawerBlock drawer) {
            key = "tooltip.storagenexus.drawer." + drawer.slots();
        } else if (stack.getItem() instanceof AiotItem) {
            key = "tooltip.storagenexus.aiot"; // the same for every material
        } else if (stack.getItem() instanceof net.storagenexus.disk.StorageDiskItem disk) {
            key = disk.isFluid() ? "tooltip.storagenexus.fluid_disk" : "tooltip.storagenexus.storage_disk"; // every size
        } else if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_storage_part")) {
            key = "tooltip.storagenexus.storage_part";
        } else if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_fluid_part")) {
            key = "tooltip.storagenexus.fluid_part";
        } else if (stack.getItem() instanceof net.storagenexus.builder.BuilderItem) {
            key = "tooltip.storagenexus.builder"; // the same for every tier
        } else {
            key = stack.getItem().getDescriptionId() + ".desc";
        }
        Language language = Language.getInstance();
        for (int i = 1; i <= MAX_LINES; i++) {
            String lineKey = i == 1 ? key : key + i;
            if (!language.has(lineKey)) break;
            lines.add(Component.translatable(lineKey).withStyle(ChatFormatting.GRAY));
        }
        // Nexium mining tools: whether their Haste II is on
        if (stack.is(NexiumMaterials.GRANTS_HASTE)) {
            lines.add(stack.has(ModComponents.HASTE_OFF)
                    ? Component.translatable("tooltip.storagenexus.haste_off").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable("tooltip.storagenexus.haste_on").withStyle(ChatFormatting.BLUE));
        }
        // Optional red warning line, e.g. for the Void Upgrade
        String warn = stack.getItem().getDescriptionId() + ".warn";
        if (language.has(warn)) lines.add(Component.translatable(warn).withStyle(ChatFormatting.RED));
    }

    private ItemDescriptions() {}
}
