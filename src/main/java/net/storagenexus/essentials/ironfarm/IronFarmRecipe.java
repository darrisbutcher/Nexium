package net.storagenexus.essentials.ironfarm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModTags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a "Nexus Iron Farm" double chest must hold: 4 zombies, 12 villagers (spawn eggs or Captured Mobs),
 * 33 torches, 20 slabs, 16 signs, 12 trapdoors and 12 beds, with a full stack of smooth stone in every other slot.
 * Amounts are totals, so a material can be split over several slots.
 */
public final class IronFarmRecipe {
    /** A double chest. */
    public static final int CHEST_SLOTS = 54;
    private static final int MAX_STRAYS_LISTED = 5;

    public enum Part {
        ZOMBIES("zombies", 4),
        VILLAGERS("villagers", 12),
        TORCHES("torches", 33),
        SLABS("slabs", 20),
        SIGNS("signs", 16),
        TRAPDOORS("trapdoors", 12),
        BEDS("beds", 12);

        public final String key;
        public final int needed;

        Part(String key, int needed) {
            this.key = key;
            this.needed = needed;
        }

        public Component displayName() {
            return Component.translatable("message.nexusessentials.iron_farm_part." + key);
        }
    }

    /** How a chest's contents compare with the recipe. */
    public record Check(int slots, int[] counts, int emptySlots, int partialFiller, List<ItemStack> strays) {
        public int count(Part part) {
            return counts[part.ordinal()];
        }

        public boolean complete() {
            if (slots != CHEST_SLOTS || emptySlots > 0 || partialFiller > 0 || !strays.isEmpty()) return false;
            for (Part part : Part.values()) if (count(part) != part.needed) return false;
            return true;
        }

        /** One chat line per thing to fix. Empty when complete. */
        public List<Component> problems() {
            List<Component> lines = new ArrayList<>();
            if (slots != CHEST_SLOTS) {
                lines.add(Component.translatable("message.nexusessentials.iron_farm_double_chest").withStyle(ChatFormatting.RED));
                lines.add(recipe());
                return lines;
            }
            MutableComponent missing = null, extra = null;
            for (Part part : Part.values()) {
                int diff = part.needed - count(part);
                if (diff > 0) missing = append(missing, amount(part, diff));
                if (diff < 0) extra = append(extra, amount(part, -diff));
            }
            if (missing != null) lines.add(Component.translatable("message.nexusessentials.iron_farm_missing", missing).withStyle(ChatFormatting.YELLOW));
            if (extra != null) lines.add(Component.translatable("message.nexusessentials.iron_farm_too_many", extra).withStyle(ChatFormatting.YELLOW));
            if (!strays.isEmpty()) {
                MutableComponent names = null;
                for (int i = 0; i < Math.min(MAX_STRAYS_LISTED, strays.size()); i++) names = append(names, strays.get(i).getHoverName());
                if (strays.size() > MAX_STRAYS_LISTED) {
                    names = append(names, Component.translatable("message.nexusessentials.iron_farm_more", strays.size() - MAX_STRAYS_LISTED));
                }
                lines.add(Component.translatable("message.nexusessentials.iron_farm_remove", names).withStyle(ChatFormatting.RED));
            }
            if (emptySlots > 0) {
                lines.add(Component.translatable("message.nexusessentials.iron_farm_fill", emptySlots).withStyle(ChatFormatting.YELLOW));
            }
            if (partialFiller > 0) {
                lines.add(Component.translatable("message.nexusessentials.iron_farm_partial", partialFiller).withStyle(ChatFormatting.YELLOW));
            }
            return lines;
        }
    }

    /** Checks a chest's slots, in any order. */
    public static Check check(List<ItemStack> slots) {
        int[] counts = new int[Part.values().length];
        int empty = 0, partialFiller = 0;
        // Different items that don't belong, one entry each (by item and name)
        Map<String, ItemStack> strays = new LinkedHashMap<>();
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                empty++;
                continue;
            }
            Part part = partOf(stack);
            if (part != null) {
                counts[part.ordinal()] += stack.getCount();
            } else if (stack.is(ModTags.IRON_FARM_FILLER)) {
                if (stack.getCount() < stack.getMaxStackSize()) partialFiller++;
            } else {
                strays.putIfAbsent(stack.getItem() + "|" + stack.getHoverName().getString(), stack);
            }
        }
        return new Check(slots.size(), counts, empty, partialFiller, List.copyOf(strays.values()));
    }

    /** Which material a stack counts as, or null if it isn't one. */
    public static @Nullable Part partOf(ItemStack stack) {
        EntityType<?> mob = mobOf(stack);
        if (mob != null) {
            if (mob.builtInRegistryHolder().is(ModTags.IRON_FARM_ZOMBIES)) return Part.ZOMBIES;
            if (mob.builtInRegistryHolder().is(ModTags.IRON_FARM_VILLAGERS)) return Part.VILLAGERS;
            return null;
        }
        if (stack.is(ModTags.IRON_FARM_TORCHES)) return Part.TORCHES;
        if (stack.is(ModTags.IRON_FARM_SLABS)) return Part.SLABS;
        if (stack.is(ModTags.IRON_FARM_SIGNS)) return Part.SIGNS;
        if (stack.is(ModTags.IRON_FARM_TRAPDOORS)) return Part.TRAPDOORS;
        if (stack.is(ModTags.IRON_FARM_BEDS)) return Part.BEDS;
        return null;
    }

    /** The mob in a spawn egg or a Captured Mob item, or null for anything else. */
    private static @Nullable EntityType<?> mobOf(ItemStack stack) {
        CapturedMob captured = stack.get(ModComponents.CAPTURED_MOB);
        if (captured != null) return captured.type();
        if (stack.getItem() instanceof SpawnEggItem) return SpawnEggItem.getType(stack);
        return null;
    }

    /** The whole recipe on one line, for a chest that can't be checked yet. */
    public static Component recipe() {
        MutableComponent parts = null;
        for (Part part : Part.values()) parts = append(parts, amount(part, part.needed));
        return Component.translatable("message.nexusessentials.iron_farm_recipe", parts).withStyle(ChatFormatting.GRAY);
    }

    private static Component amount(Part part, int count) {
        return Component.translatable("message.nexusessentials.iron_farm_amount", count, part.displayName());
    }

    private static MutableComponent append(@Nullable MutableComponent list, Component item) {
        if (list == null) return Component.empty().append(item);
        return list.append(", ").append(item);
    }

    private IronFarmRecipe() {}
}
