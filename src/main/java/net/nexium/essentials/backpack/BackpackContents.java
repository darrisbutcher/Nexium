package net.nexium.essentials.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * What a Backpack holds: {@link #SIZE} slots, saved in the Backpack's {@code nexium:backpack_items}
 * component so the contents travel with the item (dropped on death, kept with keepInventory, carried in chests).
 *
 * <p>Each stored slot is an item (with its components), a count and its slot number rather than a plain item stack,
 * because vanilla's stack codec stops at 99 and every stackable item stacks to {@value #STACK_MAX} in a Backpack.
 */
public final class BackpackContents {
    /** Slots in a Backpack: the same as the player's inventory including the hotbar. */
    public static final int SIZE = 36;
    /** How many of a stackable item fit in a slot of a Backpack (items that do not stack, such as tools, still hold one). */
    public static final int STACK_MAX = 1024;

    /** One occupied slot. */
    public record Entry(int slot, ItemVariant variant, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, SIZE - 1).fieldOf("slot").forGetter(Entry::slot),
                ItemVariant.CODEC.fieldOf("item").forGetter(Entry::variant),
                Codec.intRange(1, 1 << 20).fieldOf("count").forGetter(Entry::count)
        ).apply(i, Entry::new));
    }

    /** The saved form: every occupied slot. */
    public record Saved(List<Entry> entries) {
        public static final Saved EMPTY = new Saved(List.of());
        public static final Codec<Saved> CODEC = Entry.CODEC.listOf().xmap(Saved::new, Saved::entries);
    }

    /** The Backpack's slots, always {@link #SIZE} long (empty stacks for empty slots). */
    public static NonNullList<ItemStack> read(ItemStack backpack) {
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (Entry entry : backpack.getOrDefault(ModComponents.BACKPACK_ITEMS, Saved.EMPTY).entries()) {
            if (entry.variant().isBlank() || entry.slot() < 0 || entry.slot() >= SIZE) continue;
            list.set(entry.slot(), entry.variant().toStack(entry.count()));
        }
        return list;
    }

    /** Saves these slots (as many as are given, up to {@link #SIZE}) in the Backpack. */
    public static void write(ItemStack backpack, List<ItemStack> stacks) {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < Math.min(SIZE, stacks.size()); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) entries.add(new Entry(slot, ItemVariant.of(stack), stack.getCount()));
        }
        if (entries.isEmpty()) backpack.remove(ModComponents.BACKPACK_ITEMS);
        else backpack.set(ModComponents.BACKPACK_ITEMS, new Saved(List.copyOf(entries)));
    }

    /** How many slots hold something. */
    public static int usedSlots(ItemStack backpack) {
        return (int) backpack.getOrDefault(ModComponents.BACKPACK_ITEMS, Saved.EMPTY).entries().stream()
                .filter(e -> !e.variant().isBlank() && e.slot() >= 0 && e.slot() < SIZE).count();
    }

    /** How many of this item the Backpack holds in all. */
    public static int count(ItemStack backpack, Item item) {
        long total = 0;
        for (Entry entry : backpack.getOrDefault(ModComponents.BACKPACK_ITEMS, Saved.EMPTY).entries()) {
            if (entry.variant().getItem() == item) total += entry.count();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Takes up to this many of the item out of the Backpack, from its last slots first. Returns how many came out. */
    public static int take(ItemStack backpack, Item item, int amount) {
        NonNullList<ItemStack> slots = read(backpack);
        int took = takeFrom(slots, item, amount);
        if (took > 0) write(backpack, slots);
        return took;
    }

    /** Takes up to this many of the item out of these slots, from the last slot first. Returns how many came out. */
    public static int takeFrom(List<ItemStack> slots, Item item, int amount) {
        if (amount <= 0) return 0;
        int left = amount;
        for (int slot = slots.size() - 1; slot >= 0 && left > 0; slot--) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty() || !stack.is(item)) continue;
            int took = Math.min(left, stack.getCount());
            stack.shrink(took);
            left -= took;
        }
        return amount - left;
    }

    /**
     * Puts the stack into these slots: first on top of stacks of the same item, then into empty slots, as many as fit
     * (up to {@value #STACK_MAX} for a stackable item). Returns what did not fit.
     */
    public static ItemStack insert(List<ItemStack> slots, ItemStack stack) {
        ItemStack left = stack.copy();
        int max = maxStack(left);
        for (int i = 0; i < slots.size() && !left.isEmpty(); i++) {
            ItemStack there = slots.get(i);
            if (there.isEmpty() || !ItemStack.isSameItemSameComponents(there, left)) continue;
            int moved = Math.min(max - there.getCount(), left.getCount());
            if (moved <= 0) continue;
            there.grow(moved);
            left.shrink(moved);
        }
        for (int i = 0; i < slots.size() && !left.isEmpty(); i++) {
            if (slots.get(i).isEmpty()) slots.set(i, left.split(Math.min(max, left.getCount())));
        }
        return left;
    }

    /** How many of this stack fit in one slot: {@link #STACK_MAX} for anything that stacks, one for anything that does not. */
    public static int maxStack(ItemStack stack) {
        return stack.getMaxStackSize() > 1 ? STACK_MAX : 1;
    }

    private BackpackContents() {}
}
