package net.nexium.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * What a worn tool gives back when it is smelted down: the recipe's result count is what an undamaged one gives, and every bit of
 * wear takes its share off, rounded to the nearest and never below one.
 */
public final class DurabilityRefund {
    public static int count(ItemStack input, int full) {
        int max = input.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        if (max <= 0 || full <= 1) return Math.max(1, full);
        int damage = Math.min(max, Math.max(0, input.getOrDefault(DataComponents.DAMAGE, 0)));
        return Math.max(1, Math.round(full * (max - damage) / (float) max));
    }

    public static ItemStack scale(ItemStack result, ItemStack input) {
        result.setCount(count(input, result.getCount()));
        return result;
    }

    private DurabilityRefund() {}
}
