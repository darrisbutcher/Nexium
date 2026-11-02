package net.storagenexus.aiot;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.storagenexus.registry.ModComponents;
import org.jspecify.annotations.Nullable;

/**
 * Silk Touch and Fortune on one AIOT. Vanilla never lets an item have both, so the AIOT keeps only one in
 * {@code minecraft:enchantments} and the other in {@code storagenexus:inactive_enchantments}, and records which is
 * active in {@code storagenexus:mining_mode}. Every change to an AIOT's enchantments (crafting, the anvil, the
 * grindstone, the tool menu) ends with {@link #normalize}, which keeps the three components in step.
 */
public final class AiotEnchanting {
    /**
     * Puts the AIOT's Silk Touch and Fortune where they belong. If it holds both (active or inactive), the one for
     * {@code prefer} is active (or, without a preference, whichever its mining mode says, else Fortune) and the other
     * inactive. If it holds only one, that one is active and the AIOT has no mining mode.
     */
    public static void normalize(ItemStack stack, @Nullable MiningMode prefer) {
        ItemEnchantments active = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments inactive = stack.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY);
        Holder<Enchantment> silkTouch = null, fortune = null;
        int silkLevel = 0, fortuneLevel = 0;
        for (ItemEnchantments set : new ItemEnchantments[]{active, inactive}) {
            for (Object2IntMap.Entry<Holder<Enchantment>> e : set.entrySet()) {
                if (e.getKey().is(Enchantments.SILK_TOUCH) && e.getIntValue() > silkLevel) {
                    silkTouch = e.getKey();
                    silkLevel = e.getIntValue();
                } else if (e.getKey().is(Enchantments.FORTUNE) && e.getIntValue() > fortuneLevel) {
                    fortune = e.getKey();
                    fortuneLevel = e.getIntValue();
                }
            }
        }

        ItemEnchantments.Mutable newActive = new ItemEnchantments.Mutable(active);
        newActive.removeIf(e -> e.is(Enchantments.SILK_TOUCH) || e.is(Enchantments.FORTUNE));
        if (silkTouch != null && fortune != null) {
            MiningMode mode = prefer != null ? prefer : stack.getOrDefault(ModComponents.MINING_MODE, MiningMode.FORTUNE);
            boolean silkActive = mode == MiningMode.SILK_TOUCH;
            newActive.set(silkActive ? silkTouch : fortune, silkActive ? silkLevel : fortuneLevel);
            ItemEnchantments.Mutable newInactive = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            newInactive.set(silkActive ? fortune : silkTouch, silkActive ? fortuneLevel : silkLevel);
            stack.set(ModComponents.INACTIVE_ENCHANTMENTS, newInactive.toImmutable());
            stack.set(ModComponents.MINING_MODE, mode);
        } else {
            if (silkTouch != null) newActive.set(silkTouch, silkLevel);
            if (fortune != null) newActive.set(fortune, fortuneLevel);
            stack.remove(ModComponents.INACTIVE_ENCHANTMENTS);
            stack.remove(ModComponents.MINING_MODE);
        }
        stack.set(DataComponents.ENCHANTMENTS, newActive.toImmutable());
    }

    /** Whether the AIOT holds both Silk Touch and Fortune, so the tool menu can switch between them. */
    public static boolean canSwap(ItemStack stack) {
        return stack.getItem() instanceof AiotItem && stack.has(ModComponents.MINING_MODE);
    }

    /** Swaps the active and inactive one. Returns the new mode, or null if the AIOT doesn't hold both. */
    public static @Nullable MiningMode swap(ItemStack stack) {
        if (!canSwap(stack)) return null;
        MiningMode next = stack.getOrDefault(ModComponents.MINING_MODE, MiningMode.FORTUNE).other();
        normalize(stack, next);
        return next;
    }

    /** The level of the active Silk Touch or Fortune, for messages. */
    public static int activeLevel(ItemStack stack, MiningMode mode) {
        for (Object2IntMap.Entry<Holder<Enchantment>> e : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (e.getKey().is(mode.enchantment())) return e.getIntValue();
        }
        return 0;
    }

    /** At an anvil, an AIOT can take Silk Touch on top of Fortune or the other way round. */
    public static boolean mayHoldBoth(ItemStack left, Holder<Enchantment> a, Holder<Enchantment> b) {
        return left.getItem() instanceof AiotItem
                && ((a.is(Enchantments.SILK_TOUCH) && b.is(Enchantments.FORTUNE)) || (a.is(Enchantments.FORTUNE) && b.is(Enchantments.SILK_TOUCH)));
    }

    /**
     * Tidies the anvil's result when it is an AIOT. The anvil only looks at active enchantments, so this adds the
     * right-hand AIOT's inactive one too, and keeps active whichever of Silk Touch and Fortune the left AIOT was using.
     */
    public static void afterAnvil(ItemStack left, ItemStack right, ItemStack result) {
        if (!(result.getItem() instanceof AiotItem)) return;
        if (right.getItem() instanceof AiotItem && right.has(ModComponents.INACTIVE_ENCHANTMENTS)) {
            ItemEnchantments.Mutable inactive = new ItemEnchantments.Mutable(
                    result.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY));
            for (Object2IntMap.Entry<Holder<Enchantment>> e : right.get(ModComponents.INACTIVE_ENCHANTMENTS).entrySet()) {
                inactive.upgrade(e.getKey(), e.getIntValue());
            }
            result.set(ModComponents.INACTIVE_ENCHANTMENTS, inactive.toImmutable());
        }
        MiningMode leftMode = left.get(ModComponents.MINING_MODE);
        if (leftMode == null) {
            // The left AIOT had at most one of them: keep that one active
            leftMode = activeLevel(left, MiningMode.SILK_TOUCH) > 0 ? MiningMode.SILK_TOUCH : MiningMode.FORTUNE;
        }
        normalize(result, leftMode);
    }

    private AiotEnchanting() {}
}
