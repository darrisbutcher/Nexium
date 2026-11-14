package net.storagenexus.essentials.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * What a Backpack holds: {@link #SIZE} slots, saved in the Backpack's {@code nexusessentials:backpack_items}
 * component so the contents travel with the item (dropped on death, kept with keepInventory, carried in chests).
 *
 * <p>Each stored slot is an item (with its components), a count and its slot number rather than a plain item stack,
 * because vanilla's stack codec stops at 99 and the Torch Stack module lets torches stack to {@value #TORCH_STACK_MAX}.
 */
public final class BackpackContents {
    /** Slots in a Backpack: the same as the player's inventory including the hotbar. */
    public static final int SIZE = 36;
    /** How many torches fit in a slot while the Torch Stack module is in and on. */
    public static final int TORCH_STACK_MAX = 256;

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
        if (amount <= 0) return 0;
        NonNullList<ItemStack> slots = read(backpack);
        int left = amount;
        for (int slot = SIZE - 1; slot >= 0 && left > 0; slot--) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty() || !stack.is(item)) continue;
            int took = Math.min(left, stack.getCount());
            stack.shrink(took);
            left -= took;
        }
        if (left == amount) return 0;
        write(backpack, slots);
        return amount - left;
    }

    /** How many of this stack fit in one slot: torches stack to {@link #TORCH_STACK_MAX} with the Torch Stack module on, everything else to its own limit. */
    public static int maxStack(ItemStack stack, boolean torchStack) {
        return torchStack && stack.is(Items.TORCH) ? TORCH_STACK_MAX : stack.getMaxStackSize();
    }

    private BackpackContents() {}
}
