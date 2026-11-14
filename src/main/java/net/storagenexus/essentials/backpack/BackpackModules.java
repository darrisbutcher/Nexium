package net.storagenexus.essentials.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.essentials.registry.ModComponents;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Modules installed in a Backpack: kept in its {@code nexusessentials:backpack_modules} component, {@link #SLOTS} slots,
 * one module of each type at most. Each module carries its own on/off switch ({@code storagenexus:module_off}), which
 * stays with it when it is taken out.
 */
public final class BackpackModules {
    /** Module slots in a Backpack: one for every module planned (Auto Torch, Torch Stack, Pickup, Feeding, Compacting). */
    public static final int SLOTS = 5;

    /** The Backpack's module slots, always {@link #SLOTS} long (empty stacks for empty slots). */
    public static NonNullList<ItemStack> slots(ItemStack backpack) {
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        List<ItemStack> stored = backpack.getOrDefault(ModComponents.BACKPACK_MODULES, ItemContainerContents.EMPTY).itemCopies().toList();
        for (int i = 0; i < Math.min(SLOTS, stored.size()); i++) {
            if (stored.get(i).getItem() instanceof BackpackModuleItem) list.set(i, stored.get(i));
        }
        return list;
    }

    public static void setSlots(ItemStack backpack, List<ItemStack> slots) {
        if (slots.stream().allMatch(ItemStack::isEmpty)) backpack.remove(ModComponents.BACKPACK_MODULES);
        else backpack.set(ModComponents.BACKPACK_MODULES, ItemContainerContents.fromItems(slots));
    }

    /** The installed module of this type, or an empty stack. */
    public static ItemStack find(ItemStack backpack, BackpackModuleType type) {
        if (!(backpack.getItem() instanceof BackpackItem)) return ItemStack.EMPTY;
        for (ItemStack module : slots(backpack)) {
            if (module.getItem() instanceof BackpackModuleItem item && item.type() == type) return module;
        }
        return ItemStack.EMPTY;
    }

    /** Whether the Backpack has this module installed and switched on. */
    public static boolean active(ItemStack backpack, BackpackModuleType type) {
        ItemStack module = find(backpack, type);
        return !module.isEmpty() && !module.has(net.storagenexus.registry.ModComponents.MODULE_OFF);
    }

    /** Switches the module in this slot on or off. Returns its new state, or null if the slot is empty. */
    public static @Nullable Boolean toggle(ItemStack backpack, int slot) {
        NonNullList<ItemStack> slots = slots(backpack);
        if (slot < 0 || slot >= SLOTS || slots.get(slot).isEmpty()) return null;
        ItemStack module = slots.get(slot).copy();
        boolean nowOn = module.has(net.storagenexus.registry.ModComponents.MODULE_OFF);
        if (nowOn) module.remove(net.storagenexus.registry.ModComponents.MODULE_OFF);
        else module.set(net.storagenexus.registry.ModComponents.MODULE_OFF, Unit.INSTANCE);
        slots.set(slot, module);
        setSlots(backpack, slots);
        return nowOn;
    }

    private BackpackModules() {}
}
