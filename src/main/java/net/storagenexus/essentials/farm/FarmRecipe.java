package net.storagenexus.essentials.farm;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import net.storagenexus.essentials.registry.ModTags;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What each {@link FarmKind} costs at the Farm Assembler: exact amounts of a few materials (mobs count as a spawn egg
 * or a Captured Mob) and an amount of one filler block. The filler is what the double chest the farm used to be built
 * from held besides the parts: full stacks in its 54 slots, less one slot per Captured Mob or bed and one stack's worth
 * of slots for each other part.
 */
public final class FarmRecipe {
    /** One material: its lang key suffix, how many, what counts, and the item the Assembler shows for it. */
    public record Part(String key, int needed, Predicate<ItemStack> matches, Item icon, boolean mob) {
        public Component displayName() {
            return Component.translatable("message.nexusessentials.farm_part." + key);
        }
    }

    private final FarmKind kind;
    private final List<Part> parts;
    private final Part filler;

    private FarmRecipe(FarmKind kind, Part filler, Part... parts) {
        this.kind = kind;
        this.filler = filler;
        this.parts = List.of(parts);
    }

    private static Part item(String key, int needed, Item item) {
        return new Part(key, needed, s -> s.is(item), item, false);
    }

    private static Part mob(String key, int needed, EntityType<?> type, Item egg) {
        return new Part(key, needed, s -> IronFarmRecipe.mobOf(s) == type, egg, true);
    }

    private static Part filler(String key, int stacks, Predicate<ItemStack> matches, Item icon) {
        return new Part(key, stacks * 64, matches, icon, false);
    }

    private static final Map<FarmKind, FarmRecipe> RECIPES = new EnumMap<>(FarmKind.class);

    static {
        RECIPES.put(FarmKind.VILLAGER_BREEDER, new FarmRecipe(FarmKind.VILLAGER_BREEDER,
                filler("planks", 45, s -> s.is(ItemTags.PLANKS), Items.OAK_PLANKS),
                new Part("villagers", 2, s -> IronFarmRecipe.partOf(s) == IronFarmRecipe.Part.VILLAGERS, Items.VILLAGER_SPAWN_EGG, true),
                new Part("beds", 4, s -> s.is(ModTags.IRON_FARM_BEDS), Items.BED.red(), false),
                item("composters", 2, Items.COMPOSTER),
                item("hay_bales", 8, Items.HAY_BLOCK),
                item("emerald_blocks", 2, Items.EMERALD_BLOCK)));
        RECIPES.put(FarmKind.PIGLIN_BARTER, new FarmRecipe(FarmKind.PIGLIN_BARTER,
                filler("netherrack", 49, s -> s.is(Items.NETHERRACK), Items.NETHERRACK),
                mob("piglins", 2, EntityTypes.PIGLIN, Items.PIGLIN_SPAWN_EGG),
                item("gold_blocks", 9, Items.GOLD_BLOCK),
                item("soul_sand", 16, Items.SOUL_SAND),
                new Part("planks", 16, s -> s.is(ItemTags.PLANKS), Items.OAK_PLANKS, false)));
        RECIPES.put(FarmKind.ZOMBIFIED_PIGLIN, new FarmRecipe(FarmKind.ZOMBIFIED_PIGLIN,
                filler("netherrack", 47, s -> s.is(Items.NETHERRACK), Items.NETHERRACK),
                mob("zombified_piglins", 4, EntityTypes.ZOMBIFIED_PIGLIN, Items.ZOMBIFIED_PIGLIN_SPAWN_EGG),
                item("magma_blocks", 8, Items.MAGMA_BLOCK),
                item("nether_bricks", 20, Items.NETHER_BRICKS),
                item("gold_ingots", 16, Items.GOLD_INGOT)));
        RECIPES.put(FarmKind.WITHER_SKELETON, new FarmRecipe(FarmKind.WITHER_SKELETON,
                filler("netherrack", 46, s -> s.is(Items.NETHERRACK), Items.NETHERRACK),
                mob("wither_skeletons", 4, EntityTypes.WITHER_SKELETON, Items.WITHER_SKELETON_SPAWN_EGG),
                item("nether_brick_fences", 16, Items.NETHER_BRICK_FENCE),
                item("soul_sand", 8, Items.SOUL_SAND),
                item("coal_blocks", 4, Items.COAL_BLOCK),
                item("magma_blocks", 4, Items.MAGMA_BLOCK)));
        RECIPES.put(FarmKind.SHULKER, new FarmRecipe(FarmKind.SHULKER,
                filler("end_stone_bricks", 47, s -> s.is(Items.END_STONE_BRICKS), Items.END_STONE_BRICKS),
                mob("shulkers", 4, EntityTypes.SHULKER, Items.SHULKER_SPAWN_EGG),
                item("purpur_blocks", 32, Items.PURPUR_BLOCK),
                item("end_rods", 8, Items.END_ROD),
                item("ender_pearls", 8, Items.ENDER_PEARL)));
    }

    public static FarmRecipe of(FarmKind kind) {
        return RECIPES.get(kind);
    }

    public FarmKind kind() { return kind; }

    /** The materials, without the filler. */
    public List<Part> parts() { return parts; }

    /** The filler block and how much of it. */
    public Part filler() { return filler; }
}
