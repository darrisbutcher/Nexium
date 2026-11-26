package net.nexium.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.nexium.energy.ItemEnergy;
import net.nexium.registry.ModComponents;

/**
 * Recipe type {@code nexium:charge_keeping_shaped}: a shaped recipe (same JSON as {@code minecraft:crafting_shaped}) that gives the
 * result the charge of the battery or cell used up in it, as far as the result can hold it. So upgrading a Battery or a Cell
 * to the next tier keeps its energy.
 * <p>
 * It is a {@link ShapedRecipe}, so the recipe book, JEI and REI show it like any other shaped recipe.
 */
public class ChargeKeepingShapedRecipe extends ShapedRecipe {
    public static final MapCodec<ChargeKeepingShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.keptPattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.keptResult))
            .apply(i, ChargeKeepingShapedRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeKeepingShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.keptPattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.keptResult,
            ChargeKeepingShapedRecipe::new);
    public static final RecipeSerializer<ChargeKeepingShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    // ShapedRecipe keeps its own copies private
    private final ShapedRecipePattern keptPattern;
    private final ItemStackTemplate keptResult;

    public ChargeKeepingShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                     ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.keptPattern = pattern;
        this.keptResult = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = keptResult.create();
        long charge = 0;
        for (ItemStack stack : input.items()) {
            Long held = stack.get(ModComponents.BATTERY_CHARGE);
            if (held != null) charge = Math.max(charge, held);
        }
        long room = capacityOf(result);
        charge = Math.min(charge, room);
        if (charge > 0) result.set(ModComponents.BATTERY_CHARGE, charge);
        return result;
    }

    /** What the result can hold: a cell's capacity, or the Battery block's. */
    private static long capacityOf(ItemStack result) {
        long cell = ItemEnergy.capacity(result);
        if (cell > 0) return cell;
        return result.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock() instanceof net.nexium.energy.BatteryBlock battery ? battery.capacity() : 0;
    }

    // ShapedRecipe declares RecipeSerializer<ShapedRecipe>; ours serializes this subclass with the same JSON.
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }
}
