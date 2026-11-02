package net.storagenexus.aiot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

/**
 * Recipe type {@code storagenexus:aiot_merge}: a shapeless recipe (same JSON as {@code minecraft:crafting_shapeless})
 * whose result keeps the ingredients' enchantments, durability and repair cost; see {@link AiotMerge}.
 * <p>
 * It is a {@link ShapelessRecipe}, so the recipe book, JEI and REI show it like any other shapeless recipe.
 */
public class AiotMergeRecipe extends ShapelessRecipe {
    public static final MapCodec<AiotMergeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.mergeResult),
                    Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.mergeIngredients))
            .apply(i, AiotMergeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AiotMergeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, r -> r.mergeResult,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.mergeIngredients,
            AiotMergeRecipe::new);
    public static final RecipeSerializer<AiotMergeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    // ShapelessRecipe keeps its own copies private
    private final ItemStackTemplate mergeResult;
    private final List<Ingredient> mergeIngredients;

    public AiotMergeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                           ItemStackTemplate result, List<Ingredient> ingredients) {
        super(commonInfo, bookInfo, result, ingredients);
        this.mergeResult = result;
        this.mergeIngredients = ingredients;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<ItemStack> tools = input.items().stream().filter(stack -> !stack.isEmpty()).toList();
        return AiotMerge.merge(tools, mergeResult.create());
    }

    // ShapelessRecipe declares RecipeSerializer<ShapelessRecipe>; ours serializes this subclass with the same JSON.
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }
}
