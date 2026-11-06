package net.storagenexus.essentials.farm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModTags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What a farm's double chest must hold: exact amounts of a few materials (mobs count as a spawn egg or a Captured Mob)
 * and a full stack of one filler item in every other slot. Amounts are totals, so a material can be split over slots.
 */
public final class FarmRecipe {
    /** A double chest. */
    public static final int CHEST_SLOTS = 54;
    private static final int MAX_STRAYS_LISTED = 5;

    /** One material: its lang key suffix, how many, and what counts. */
    public record Part(String key, int needed, Predicate<ItemStack> matches) {
        public Component displayName() {
            return Component.translatable("message.nexusessentials.farm_part." + key);
        }
    }

    private final FarmKind kind;
    private final List<Part> parts;
    private final String fillerKey;
    private final Predicate<ItemStack> filler;

    private FarmRecipe(FarmKind kind, String fillerKey, Predicate<ItemStack> filler, Part... parts) {
        this.kind = kind;
        this.fillerKey = fillerKey;
        this.filler = filler;
        this.parts = List.of(parts);
    }

    private static Part item(String key, int needed, Item item) {
        return new Part(key, needed, s -> s.is(item));
    }

    private static Part mob(String key, int needed, EntityType<?> type) {
        return new Part(key, needed, s -> mobOf(s) == type);
    }

    private static final Map<FarmKind, FarmRecipe> RECIPES = new java.util.EnumMap<>(FarmKind.class);

    static {
        RECIPES.put(FarmKind.VILLAGER_BREEDER, new FarmRecipe(FarmKind.VILLAGER_BREEDER, "planks", s -> s.is(ItemTags.PLANKS),
                new Part("villagers", 2, s -> IronFarmRecipe.partOf(s) == IronFarmRecipe.Part.VILLAGERS),
                new Part("beds", 4, s -> s.is(ModTags.IRON_FARM_BEDS)),
                item("composters", 2, Items.COMPOSTER),
                item("hay_bales", 8, Items.HAY_BLOCK),
                item("emerald_blocks", 2, Items.EMERALD_BLOCK)));
        RECIPES.put(FarmKind.PIGLIN_BARTER, new FarmRecipe(FarmKind.PIGLIN_BARTER, "netherrack", s -> s.is(Items.NETHERRACK),
                mob("piglins", 2, EntityTypes.PIGLIN),
                item("gold_blocks", 9, Items.GOLD_BLOCK),
                item("soul_sand", 16, Items.SOUL_SAND),
                new Part("planks", 16, s -> s.is(ItemTags.PLANKS))));
        RECIPES.put(FarmKind.ZOMBIFIED_PIGLIN, new FarmRecipe(FarmKind.ZOMBIFIED_PIGLIN, "netherrack", s -> s.is(Items.NETHERRACK),
                mob("zombified_piglins", 4, EntityTypes.ZOMBIFIED_PIGLIN),
                item("magma_blocks", 8, Items.MAGMA_BLOCK),
                item("nether_bricks", 20, Items.NETHER_BRICKS),
                item("gold_ingots", 16, Items.GOLD_INGOT)));
        RECIPES.put(FarmKind.WITHER_SKELETON, new FarmRecipe(FarmKind.WITHER_SKELETON, "netherrack", s -> s.is(Items.NETHERRACK),
                mob("wither_skeletons", 4, EntityTypes.WITHER_SKELETON),
                item("nether_brick_fences", 16, Items.NETHER_BRICK_FENCE),
                item("soul_sand", 8, Items.SOUL_SAND),
                item("coal_blocks", 4, Items.COAL_BLOCK),
                item("magma_blocks", 4, Items.MAGMA_BLOCK)));
        RECIPES.put(FarmKind.SHULKER, new FarmRecipe(FarmKind.SHULKER, "end_stone_bricks", s -> s.is(Items.END_STONE_BRICKS),
                mob("shulkers", 4, EntityTypes.SHULKER),
                item("purpur_blocks", 32, Items.PURPUR_BLOCK),
                item("end_rods", 8, Items.END_ROD),
                item("ender_pearls", 8, Items.ENDER_PEARL)));
    }

    public static FarmRecipe of(FarmKind kind) {
        return RECIPES.get(kind);
    }

    public FarmKind kind() { return kind; }

    public List<Part> parts() { return parts; }

    /** How a chest's contents compare with the recipe. */
    public record Check(int slots, int[] counts, int emptySlots, int partialFiller, List<ItemStack> strays) {}

    /** Checks a chest's slots, in any order. */
    public Check check(List<ItemStack> slots) {
        int[] counts = new int[parts.size()];
        int empty = 0, partialFiller = 0;
        Map<String, ItemStack> strays = new LinkedHashMap<>();
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                empty++;
                continue;
            }
            int part = partOf(stack);
            if (part >= 0) {
                counts[part] += stack.getCount();
            } else if (filler.test(stack)) {
                if (stack.getCount() < stack.getMaxStackSize()) partialFiller++;
            } else {
                strays.putIfAbsent(stack.getItem() + "|" + stack.getHoverName().getString(), stack);
            }
        }
        return new Check(slots.size(), counts, empty, partialFiller, List.copyOf(strays.values()));
    }

    private int partOf(ItemStack stack) {
        for (int i = 0; i < parts.size(); i++) if (parts.get(i).matches().test(stack)) return i;
        return -1;
    }

    public boolean complete(Check check) {
        if (check.slots() != CHEST_SLOTS || check.emptySlots() > 0 || check.partialFiller() > 0 || !check.strays().isEmpty()) return false;
        for (int i = 0; i < parts.size(); i++) if (check.counts()[i] != parts.get(i).needed()) return false;
        return true;
    }

    /** One chat line per thing to fix. Empty when complete. */
    public List<Component> problems(Check check) {
        List<Component> lines = new ArrayList<>();
        if (check.slots() != CHEST_SLOTS) {
            lines.add(Component.translatable("message.nexusessentials.farm_double_chest").withStyle(ChatFormatting.RED));
            lines.add(recipe());
            return lines;
        }
        MutableComponent missing = null, extra = null;
        for (int i = 0; i < parts.size(); i++) {
            int diff = parts.get(i).needed() - check.counts()[i];
            if (diff > 0) missing = append(missing, amount(parts.get(i), diff));
            if (diff < 0) extra = append(extra, amount(parts.get(i), -diff));
        }
        if (missing != null) lines.add(Component.translatable("message.nexusessentials.farm_missing", missing).withStyle(ChatFormatting.YELLOW));
        if (extra != null) lines.add(Component.translatable("message.nexusessentials.farm_too_many", extra).withStyle(ChatFormatting.YELLOW));
        if (!check.strays().isEmpty()) {
            MutableComponent names = null;
            for (int i = 0; i < Math.min(MAX_STRAYS_LISTED, check.strays().size()); i++) names = append(names, check.strays().get(i).getHoverName());
            if (check.strays().size() > MAX_STRAYS_LISTED) {
                names = append(names, Component.translatable("message.nexusessentials.iron_farm_more", check.strays().size() - MAX_STRAYS_LISTED));
            }
            lines.add(Component.translatable("message.nexusessentials.farm_remove", names).withStyle(ChatFormatting.RED));
        }
        if (check.emptySlots() > 0) {
            lines.add(Component.translatable("message.nexusessentials.farm_fill", check.emptySlots(), fillerName()).withStyle(ChatFormatting.YELLOW));
        }
        if (check.partialFiller() > 0) {
            lines.add(Component.translatable("message.nexusessentials.farm_partial", check.partialFiller(), fillerName()).withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }

    private Component fillerName() {
        return Component.translatable("message.nexusessentials.farm_part." + fillerKey);
    }

    /** The whole recipe on one line, for a chest that can't be checked yet. */
    public Component recipe() {
        MutableComponent list = null;
        for (Part part : parts) list = append(list, amount(part, part.needed()));
        return Component.translatable("message.nexusessentials.farm_recipe", list, fillerName()).withStyle(ChatFormatting.GRAY);
    }

    private static Component amount(Part part, int count) {
        return Component.translatable("message.nexusessentials.iron_farm_amount", count, part.displayName());
    }

    private static MutableComponent append(@Nullable MutableComponent list, Component item) {
        if (list == null) return Component.empty().append(item);
        return list.append(", ").append(item);
    }

    /** The mob in a spawn egg or a Captured Mob item, or null for anything else. */
    private static @Nullable EntityType<?> mobOf(ItemStack stack) {
        CapturedMob captured = stack.get(ModComponents.CAPTURED_MOB);
        if (captured != null) return captured.type();
        if (stack.getItem() instanceof SpawnEggItem) return SpawnEggItem.getType(stack);
        return null;
    }
}
