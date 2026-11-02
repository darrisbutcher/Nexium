package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.storagenexus.StorageNexus;
import net.storagenexus.aiot.AiotMergeRecipe;

public final class ModRecipes {
    /** {@code storagenexus:aiot_merge}: five tools into an AIOT, keeping their enchantments and durability. */
    public static final RecipeSerializer<AiotMergeRecipe> AIOT_MERGE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            StorageNexus.id("aiot_merge"), AiotMergeRecipe.SERIALIZER);

    public static void init() {}

    private ModRecipes() {}
}
