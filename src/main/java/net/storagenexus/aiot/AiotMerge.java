package net.storagenexus.aiot;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.storagenexus.registry.ModComponents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Combines five tools into an AIOT, keeping what was put into them:
 * <ul>
 *   <li>every enchantment, at the highest level any of the tools had it;</li>
 *   <li>Silk Touch and Fortune together: Fortune is active and Silk Touch waits, inactive, until it is swapped in;</li>
 *   <li>of enchantments that can't go together (Sharpness, Smite, Bane of Arthropods), the highest level, Sharpness
 *       on a tie;</li>
 *   <li>curses, so crafting never gets rid of one;</li>
 *   <li>durability: the AIOT has exactly the uses the five tools had left;</li>
 *   <li>the anvil repair cost of the most expensive tool, so crafting can't reset it.</li>
 * </ul>
 */
public final class AiotMerge {
    /** Curses first (always kept), then higher levels, then Sharpness before the other damage enchantments. */
    private static final Comparator<Object2IntMap.Entry<Holder<Enchantment>>> KEEP_ORDER =
            Comparator.<Object2IntMap.Entry<Holder<Enchantment>>>comparingInt(e -> e.getKey().is(EnchantmentTags.CURSE) ? 0 : 1)
                    .thenComparing(Comparator.comparingInt((Object2IntMap.Entry<Holder<Enchantment>> e) -> e.getIntValue()).reversed())
                    .thenComparingInt(e -> e.getKey().is(Enchantments.SHARPNESS) ? 0 : 1)
                    .thenComparing(e -> e.getKey().getRegisteredName());

    /**
     * @param tools  the tools in the crafting grid
     * @param result a fresh AIOT; it is changed and returned
     */
    public static ItemStack merge(List<ItemStack> tools, ItemStack result) {
        mergeEnchantments(tools, result);

        // Durability: the uses the tools had left, added together
        int usesLeft = 0;
        for (ItemStack tool : tools) {
            usesLeft += tool.isDamageableItem() ? tool.getMaxDamage() - tool.getDamageValue() : tool.getMaxDamage();
        }
        if (result.isDamageableItem()) {
            int max = result.getMaxDamage();
            result.setDamageValue(Math.clamp(max - usesLeft, 0, max - 1));
        }

        int repairCost = 0;
        for (ItemStack tool : tools) repairCost = Math.max(repairCost, tool.getOrDefault(DataComponents.REPAIR_COST, 0));
        if (repairCost > 0) result.set(DataComponents.REPAIR_COST, repairCost);
        return result;
    }

    private static void mergeEnchantments(List<ItemStack> tools, ItemStack result) {
        // Highest level of each enchantment on any tool (a tool's inactive enchantments count too)
        Object2IntLinkedOpenHashMap<Holder<Enchantment>> best = new Object2IntLinkedOpenHashMap<>();
        for (ItemStack tool : tools) {
            for (var components : List.of(DataComponents.ENCHANTMENTS, ModComponents.INACTIVE_ENCHANTMENTS)) {
                for (Object2IntMap.Entry<Holder<Enchantment>> e : tool.getOrDefault(components, ItemEnchantments.EMPTY).entrySet()) {
                    best.mergeInt(e.getKey(), e.getIntValue(), Math::max);
                }
            }
        }
        List<Object2IntMap.Entry<Holder<Enchantment>>> candidates = new ArrayList<>(best.object2IntEntrySet());
        candidates.sort(KEEP_ORDER);

        ItemEnchantments.Mutable active = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        List<Holder<Enchantment>> kept = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : candidates) {
            Holder<Enchantment> enchantment = e.getKey();
            if (!enchantment.value().isSupportedItem(result)) continue;
            boolean fits = kept.stream().allMatch(k -> Enchantment.areCompatible(k, enchantment) || isSilkTouchAndFortune(k, enchantment));
            if (!fits) continue;
            kept.add(enchantment);
            active.set(enchantment, e.getIntValue());
        }

        result.set(DataComponents.ENCHANTMENTS, active.toImmutable());
        // Both Silk Touch and Fortune: Fortune stays active, Silk Touch waits
        AiotEnchanting.normalize(result, MiningMode.FORTUNE);
    }

    private static boolean isSilkTouchAndFortune(Holder<Enchantment> a, Holder<Enchantment> b) {
        return (a.is(Enchantments.SILK_TOUCH) && b.is(Enchantments.FORTUNE)) || (a.is(Enchantments.FORTUNE) && b.is(Enchantments.SILK_TOUCH));
    }

    private AiotMerge() {}
}
