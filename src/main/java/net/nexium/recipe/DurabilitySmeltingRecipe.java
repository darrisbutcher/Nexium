package net.nexium.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * {@code nexium:durability_smelting}: a smelting recipe (same JSON as {@code minecraft:smelting}) whose result count is what an undamaged
 * item gives; a worn one gives less, see {@link DurabilityRefund}. It is a SmeltingRecipe, so furnaces, the Nexus Furnace, the recipe book,
 * JEI and REI treat it like any other.
 */
public class DurabilitySmeltingRecipe extends SmeltingRecipe {
    public static final MapCodec<DurabilitySmeltingRecipe> MAP_CODEC = AbstractCookingRecipe.cookingMapCodec(DurabilitySmeltingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, DurabilitySmeltingRecipe> STREAM_CODEC =
            AbstractCookingRecipe.cookingStreamCodec(DurabilitySmeltingRecipe::new);
    public static final RecipeSerializer<DurabilitySmeltingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public DurabilitySmeltingRecipe(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo, Ingredient input,
                                ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, input, result, experience, cookingTime);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return DurabilityRefund.scale(super.assemble(input), input.item());
    }

    // SmeltingRecipe declares RecipeSerializer<SmeltingRecipe>; ours serializes this subclass with the same JSON.
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<SmeltingRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }
}
