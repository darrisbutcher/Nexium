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
import net.minecraft.world.item.crafting.BlastingRecipe;

/**
 * {@code nexium:durability_blasting}: a blasting recipe (same JSON as {@code minecraft:blasting}) whose result count is what an undamaged
 * item gives; a worn one gives less, see {@link DurabilityRefund}. It is a BlastingRecipe, so furnaces, the Nexus Furnace, the recipe book,
 * JEI and REI treat it like any other.
 */
public class DurabilityBlastingRecipe extends BlastingRecipe {
    public static final MapCodec<DurabilityBlastingRecipe> MAP_CODEC = AbstractCookingRecipe.cookingMapCodec(DurabilityBlastingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, DurabilityBlastingRecipe> STREAM_CODEC =
            AbstractCookingRecipe.cookingStreamCodec(DurabilityBlastingRecipe::new);
    public static final RecipeSerializer<DurabilityBlastingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public DurabilityBlastingRecipe(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo, Ingredient input,
                                ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, input, result, experience, cookingTime);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return DurabilityRefund.scale(super.assemble(input), input.item());
    }

    // BlastingRecipe declares RecipeSerializer<BlastingRecipe>; ours serializes this subclass with the same JSON.
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<BlastingRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }
}
