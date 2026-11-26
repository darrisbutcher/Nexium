package net.nexium.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.nexium.Nexium;
import net.nexium.aiot.AiotMergeRecipe;

public final class ModRecipes {
    /** {@code nexium:aiot_merge}: five tools into an AIOT, keeping their enchantments and durability. */
    public static final RecipeSerializer<AiotMergeRecipe> AIOT_MERGE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            Nexium.id("aiot_merge"), AiotMergeRecipe.SERIALIZER);

    /** {@code nexium:durability_smelting} and {@code nexium:durability_blasting}: a worn tool gives back less when smelted down. */
    public static final RecipeSerializer<net.nexium.recipe.DurabilitySmeltingRecipe> DURABILITY_SMELTING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, Nexium.id("durability_smelting"), net.nexium.recipe.DurabilitySmeltingRecipe.SERIALIZER);
    public static final RecipeSerializer<net.nexium.recipe.DurabilityBlastingRecipe> DURABILITY_BLASTING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, Nexium.id("durability_blasting"), net.nexium.recipe.DurabilityBlastingRecipe.SERIALIZER);

    /** {@code nexium:charge_keeping_shaped}: upgrading a Battery or Cell to the next tier keeps its charge. */
    public static final RecipeSerializer<net.nexium.recipe.ChargeKeepingShapedRecipe> CHARGE_KEEPING_SHAPED = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, Nexium.id("charge_keeping_shaped"), net.nexium.recipe.ChargeKeepingShapedRecipe.SERIALIZER);

    public static void init() {}

    private ModRecipes() {}
}
