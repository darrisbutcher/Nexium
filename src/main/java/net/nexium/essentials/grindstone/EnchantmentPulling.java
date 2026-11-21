package net.nexium.essentials.grindstone;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.nexium.aiot.AiotEnchanting;
import net.nexium.aiot.AiotItem;
import net.nexium.registry.ModComponents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What the Enchanted Grindstone does to an item: it lists the enchantments on it and takes one off onto a book of its own.
 * The item keeps its damage, name and every other enchantment. An enchanted book that holds just one enchantment has nothing to
 * separate (the new book would be the same book). An AIOT's inactive Silk Touch or Fortune counts too, and is taken off like any other.
 */
public final class EnchantmentPulling {
    /** Where on the item an enchantment is kept. */
    public enum Source {
        /** {@code minecraft:enchantments}, on tools and armor. */
        ACTIVE,
        /** {@code minecraft:stored_enchantments}, on an enchanted book. */
        STORED,
        /** {@code nexium:inactive_enchantments}, the Silk Touch or Fortune an AIOT is not using. */
        INACTIVE;

        DataComponentType<ItemEnchantments> component() {
            return switch (this) {
                case ACTIVE -> DataComponents.ENCHANTMENTS;
                case STORED -> DataComponents.STORED_ENCHANTMENTS;
                case INACTIVE -> ModComponents.INACTIVE_ENCHANTMENTS;
            };
        }
    }

    /** One enchantment on an item, as the Grindstone's list shows it. */
    public record Entry(Holder<Enchantment> enchantment, int level, Source source) {
        public boolean curse() {
            return enchantment.is(EnchantmentTags.CURSE);
        }
    }

    /** What pulling one enchantment leaves: the item (a changed copy) and the new book that holds just that enchantment. */
    public record Pulled(ItemStack source, ItemStack book) {
    }

    /** Every enchantment on the stack, in a fixed order (by name) so both sides of a menu list them the same way. */
    public static List<Entry> entries(ItemStack stack) {
        List<Entry> out = new ArrayList<>();
        if (stack.isEmpty()) return out;
        for (Source source : Source.values()) {
            ItemEnchantments held = stack.get(source.component());
            if (held == null) continue;
            for (Object2IntMap.Entry<Holder<Enchantment>> e : held.entrySet()) out.add(new Entry(e.getKey(), e.getIntValue(), source));
        }
        out.sort(Comparator.comparing((Entry e) -> e.enchantment().getRegisteredName()));
        return out;
    }

    /**
     * Whether there is something to separate: the stack holds an enchantment, and it is not an enchanted book that already holds
     * just the one (pulling that would only move it to another book).
     */
    public static boolean pullable(ItemStack stack) {
        List<Entry> entries = entries(stack);
        if (entries.isEmpty()) return false;
        return !(entries.size() == 1 && entries.get(0).source() == Source.STORED && stack.is(Items.ENCHANTED_BOOK));
    }

    /** The entry for this enchantment on the stack, or null if it has none. */
    public static @Nullable Entry find(ItemStack stack, Holder<Enchantment> enchantment) {
        for (Entry e : entries(stack)) if (e.enchantment().equals(enchantment)) return e;
        return null;
    }

    /**
     * Takes {@code enchantment} off a copy of {@code stack} and puts it alone on a new Enchanted Book. Returns null if the stack
     * does not hold it, or if it is an enchanted book that holds only it. The stack itself is not changed.
     */
    public static @Nullable Pulled pull(ItemStack stack, Holder<Enchantment> enchantment) {
        if (!pullable(stack)) return null;
        Entry entry = find(stack, enchantment);
        if (entry == null) return null;

        ItemStack source = stack.copy();
        ItemEnchantments.Mutable left = new ItemEnchantments.Mutable(source.getOrDefault(entry.source().component(), ItemEnchantments.EMPTY));
        left.removeIf(held -> held.equals(enchantment));
        ItemEnchantments remaining = left.toImmutable();
        if (remaining.isEmpty()) source.remove(entry.source().component());
        else source.set(entry.source().component(), remaining);

        // An AIOT keeps Silk Touch and Fortune apart in two places; this puts what is left where it belongs
        if (source.getItem() instanceof AiotItem) AiotEnchanting.normalize(source, null);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable alone = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        alone.set(enchantment, entry.level());
        book.set(DataComponents.STORED_ENCHANTMENTS, alone.toImmutable());
        return new Pulled(source, book);
    }

    private EnchantmentPulling() {}
}
