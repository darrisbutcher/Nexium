package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.StorageNexus;

public final class ModTab {
    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, StorageNexus.id("main"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.storagenexus"))
                    .icon(() -> new ItemStack(ModBlocks.DRAWER_CONTROLLER))
                    .displayItems((params, output) -> {
                        for (Item item : ModItems.TAB_ORDER) output.accept(item);
                    })
                    .build());

    public static void init() {}

    private ModTab() {}
}
