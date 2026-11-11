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

    /** Captured frogs of one variant (a spawn egg doesn't say which variant it makes, so it doesn't count). */
    private static Part frog(String key, int needed, String variant) {
        return new Part(key, needed, s -> {
            var captured = s.get(net.storagenexus.essentials.registry.ModComponents.CAPTURED_MOB);
            return captured != null && captured.type() == EntityTypes.FROG
                    && captured.data().getStringOr("variant", "").endsWith(":" + variant);
        }, Items.FROG_SPAWN_EGG, true);
    }

    private static Part filler(String key, int stacks, Predicate<ItemStack> matches, Item icon) {
        return new Part(key, stacks * 64, matches, icon, false);
    }

    private static final net.minecraft.tags.TagKey<Item> SMALL_FLOWERS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.withDefaultNamespace("small_flowers"));

    /** Every glass block, plain, tinted or stained. */
    private static final net.minecraft.tags.TagKey<Item> GLASS_BLOCKS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "glass_blocks"));

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
        // The newer farms ask a quarter of the filler the first five do: 12 stacks
        RECIPES.put(FarmKind.WITCH, new FarmRecipe(FarmKind.WITCH,
                filler("mossy_cobblestone", 12, s -> s.is(Items.MOSSY_COBBLESTONE), Items.MOSSY_COBBLESTONE),
                mob("witches", 2, EntityTypes.WITCH, Items.WITCH_SPAWN_EGG),
                item("cauldrons", 8, Items.CAULDRON),
                item("spruce_planks", 16, Items.SPRUCE_PLANKS),
                item("lily_pads", 8, Items.LILY_PAD)));
        RECIPES.put(FarmKind.SUGAR_CANE, new FarmRecipe(FarmKind.SUGAR_CANE,
                filler("sand", 12, s -> s.is(Items.SAND), Items.SAND),
                item("sugar_cane", 32, Items.SUGAR_CANE),
                item("water_buckets", 4, Items.WATER_BUCKET),
                item("pistons", 8, Items.PISTON),
                item("observers", 16, Items.OBSERVER)));
        RECIPES.put(FarmKind.BAMBOO, new FarmRecipe(FarmKind.BAMBOO,
                filler("dirt", 12, s -> s.is(Items.DIRT), Items.DIRT),
                item("bamboo", 32, Items.BAMBOO),
                item("water_buckets", 4, Items.WATER_BUCKET),
                item("pistons", 8, Items.PISTON),
                item("observers", 16, Items.OBSERVER)));
        RECIPES.put(FarmKind.NETHER_WART, new FarmRecipe(FarmKind.NETHER_WART,
                filler("netherrack", 12, s -> s.is(Items.NETHERRACK), Items.NETHERRACK),
                item("nether_wart", 32, Items.NETHER_WART),
                item("soul_sand", 16, Items.SOUL_SAND),
                item("lava_buckets", 4, Items.LAVA_BUCKET),
                item("magma_blocks", 8, Items.MAGMA_BLOCK)));
        // The next ten ask 12 stacks of filler too
        RECIPES.put(FarmKind.MONSTER, new FarmRecipe(FarmKind.MONSTER,
                filler("cobblestone", 12, s -> s.is(Items.COBBLESTONE), Items.COBBLESTONE),
                mob("zombies", 1, EntityTypes.ZOMBIE, Items.ZOMBIE_SPAWN_EGG),
                mob("pillagers", 1, EntityTypes.PILLAGER, Items.PILLAGER_SPAWN_EGG),
                item("hoppers", 8, Items.HOPPER),
                item("iron_bars", 16, Items.IRON_BARS),
                item("pistons", 8, Items.PISTON),
                item("emerald_blocks", 4, Items.EMERALD_BLOCK)));
        RECIPES.put(FarmKind.NETHER_MOB, new FarmRecipe(FarmKind.NETHER_MOB,
                filler("netherrack", 12, s -> s.is(Items.NETHERRACK), Items.NETHERRACK),
                mob("blazes", 1, EntityTypes.BLAZE, Items.BLAZE_SPAWN_EGG),
                mob("ghasts", 1, EntityTypes.GHAST, Items.GHAST_SPAWN_EGG),
                mob("magma_cubes", 1, EntityTypes.MAGMA_CUBE, Items.MAGMA_CUBE_SPAWN_EGG),
                item("nether_bricks", 16, Items.NETHER_BRICKS),
                item("lava_buckets", 4, Items.LAVA_BUCKET),
                item("magma_blocks", 8, Items.MAGMA_BLOCK)));
        RECIPES.put(FarmKind.OCEAN, new FarmRecipe(FarmKind.OCEAN,
                filler("sand", 12, s -> s.is(Items.SAND), Items.SAND),
                mob("guardians", 1, EntityTypes.GUARDIAN, Items.GUARDIAN_SPAWN_EGG),
                mob("drowned", 1, EntityTypes.DROWNED, Items.DROWNED_SPAWN_EGG),
                mob("cods", 1, EntityTypes.COD, Items.COD_SPAWN_EGG),
                item("prismarine_bricks", 16, Items.PRISMARINE_BRICKS),
                item("sea_lanterns", 8, Items.SEA_LANTERN),
                item("water_buckets", 4, Items.WATER_BUCKET)));
        RECIPES.put(FarmKind.SLIME, new FarmRecipe(FarmKind.SLIME,
                filler("mud", 12, s -> s.is(Items.MUD), Items.MUD),
                mob("slimes", 2, EntityTypes.SLIME, Items.SLIME_SPAWN_EGG),
                item("slime_blocks", 4, Items.SLIME_BLOCK),
                item("sticky_pistons", 8, Items.STICKY_PISTON),
                item("hoppers", 8, Items.HOPPER)));
        RECIPES.put(FarmKind.LIVESTOCK, new FarmRecipe(FarmKind.LIVESTOCK,
                filler("dirt", 12, s -> s.is(Items.DIRT), Items.DIRT),
                mob("cows", 1, EntityTypes.COW, Items.COW_SPAWN_EGG),
                mob("pigs", 1, EntityTypes.PIG, Items.PIG_SPAWN_EGG),
                mob("sheep", 1, EntityTypes.SHEEP, Items.SHEEP_SPAWN_EGG),
                mob("chickens", 1, EntityTypes.CHICKEN, Items.CHICKEN_SPAWN_EGG),
                mob("rabbits", 1, EntityTypes.RABBIT, Items.RABBIT_SPAWN_EGG),
                item("hay_bales", 16, Items.HAY_BLOCK),
                item("oak_fences", 16, Items.OAK_FENCE)));
        RECIPES.put(FarmKind.CROP, new FarmRecipe(FarmKind.CROP,
                filler("dirt", 12, s -> s.is(Items.DIRT), Items.DIRT),
                item("wheat", 32, Items.WHEAT),
                item("carrots", 32, Items.CARROT),
                item("potatoes", 32, Items.POTATO),
                item("beetroots", 32, Items.BEETROOT),
                item("water_buckets", 4, Items.WATER_BUCKET)));
        RECIPES.put(FarmKind.GOURD, new FarmRecipe(FarmKind.GOURD,
                filler("sand", 12, s -> s.is(Items.SAND), Items.SAND),
                item("melons", 16, Items.MELON),
                item("pumpkins", 16, Items.PUMPKIN),
                item("cactus", 16, Items.CACTUS),
                item("kelp", 16, Items.KELP),
                item("water_buckets", 4, Items.WATER_BUCKET)));
        RECIPES.put(FarmKind.GENERATOR, new FarmRecipe(FarmKind.GENERATOR,
                filler("cobblestone", 12, s -> s.is(Items.COBBLESTONE), Items.COBBLESTONE),
                item("lava_buckets", 4, Items.LAVA_BUCKET),
                item("water_buckets", 4, Items.WATER_BUCKET),
                item("hoppers", 8, Items.HOPPER),
                item("diamond_pickaxes", 2, Items.DIAMOND_PICKAXE)));
        RECIPES.put(FarmKind.BEE, new FarmRecipe(FarmKind.BEE,
                filler("oak_planks", 12, s -> s.is(Items.OAK_PLANKS), Items.OAK_PLANKS),
                mob("bees", 2, EntityTypes.BEE, Items.BEE_SPAWN_EGG),
                item("beehives", 4, Items.BEEHIVE),
                item("campfires", 4, Items.CAMPFIRE),
                new Part("flowers", 16, s -> s.is(SMALL_FLOWERS), Items.POPPY, false),
                item("glass_bottles", 8, Items.GLASS_BOTTLE)));
        RECIPES.put(FarmKind.MOSS, new FarmRecipe(FarmKind.MOSS,
                filler("clay", 12, s -> s.is(Items.CLAY), Items.CLAY),
                item("moss_blocks", 32, Items.MOSS_BLOCK),
                item("azaleas", 8, Items.AZALEA),
                item("water_buckets", 4, Items.WATER_BUCKET),
                item("bone_blocks", 8, Items.BONE_BLOCK)));
        RECIPES.put(FarmKind.FROGLIGHT, new FarmRecipe(FarmKind.FROGLIGHT,
                filler("glass", 4, s -> s.is(GLASS_BLOCKS), Items.GLASS),
                frog("frogs_temperate", 3, "temperate"),
                frog("frogs_warm", 3, "warm"),
                frog("frogs_cold", 3, "cold"),
                mob("slimes", 9, EntityTypes.SLIME, Items.SLIME_SPAWN_EGG)));
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
