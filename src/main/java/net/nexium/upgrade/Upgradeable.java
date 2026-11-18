package net.nexium.upgrade;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.nexium.registry.ModComponents;

/**
 * Block entities with four storage-upgrade slots (drawers, fluid drawers, tanks).
 * Capacity upgrades add together: no upgrade = x1, one iron = x2, iron + gold = x6.
 */
public interface Upgradeable {
    /** Four upgrade slots. (Older saves had a fifth, just for the Lock Upgrade, which is the Drawer Key now.) */
    int UPGRADE_SLOTS = 4;

    NonNullList<ItemStack> getUpgrades();

    /** Called after the upgrade list changes so capacity and flags can be refreshed. */
    void onUpgradesChanged();

    /** Whether removing the upgrade in this slot would leave contents over capacity. */
    boolean canRemoveUpgrade(int slot);

    /**
     * Everything this block holds with its current upgrades, across all its slots: in stacks
     * (of 64, or of the largest form for a compacting drawer) or, for fluids, in buckets.
     */
    long totalCapacity();

    default boolean holdsFluid() {
        return false;
    }

    /** How many separate slots the total is split between. */
    default int capacitySlots() {
        return 1;
    }

    default boolean acceptsUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem up) || !up.kind().isStorageUpgrade()) return false;
        if (up.kind() != UpgradeItem.Kind.CAPACITY && hasUpgrade(up.kind())) return false;
        return true;
    }

    /**
     * Fills {@code upgrades} from a saved list. Older saves kept a Lock Upgrade (now the Drawer Key) in the list, in the
     * fifth slot or an older save's general one: it is not an upgrade any more, so it is left out, and counted so the
     * drawer can stay locked and hand the key back. Returns how many keys there were.
     */
    static int loadSaved(java.util.List<ItemStack> saved, NonNullList<ItemStack> upgrades) {
        int keys = 0;
        for (int i = 0; i < upgrades.size(); i++) upgrades.set(i, ItemStack.EMPTY);
        for (int i = 0; i < saved.size(); i++) {
            ItemStack stack = saved.get(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof DrawerKeyItem) keys += stack.getCount();
            else if (i < upgrades.size()) upgrades.set(i, stack);
        }
        return keys;
    }

    default int capacityMultiplier() {
        return multiplierFor(getUpgrades(), -1);
    }

    /** Multiplier with one slot ignored (-1 to ignore none). Capacity upgrades add together. */
    static int multiplierFor(NonNullList<ItemStack> upgrades, int skipSlot) {
        int sum = 0;
        for (int i = 0; i < upgrades.size(); i++) {
            if (i == skipSlot) continue;
            if (upgrades.get(i).getItem() instanceof CapacityUpgradeItem cap) sum += cap.multiplier();
        }
        return Math.max(1, sum);
    }

    default boolean hasUpgrade(UpgradeItem.Kind kind) {
        for (ItemStack s : getUpgrades()) {
            if (s.getItem() instanceof UpgradeItem up && up.kind() == kind) return true;
        }
        return false;
    }

    /** Puts the upgrades on the dropped item (only if there are any, so empty blocks still stack). */
    default void collectUpgrades(DataComponentMap.Builder components) {
        if (getUpgrades().stream().anyMatch(s -> !s.isEmpty())) {
            components.set(ModComponents.UPGRADES, ItemContainerContents.fromItems(getUpgrades()));
        }
    }

    /** Restores upgrades from a placed item. Does not call {@link #onUpgradesChanged()}. */
    default void applyUpgrades(DataComponentGetter components) {
        ItemContainerContents contents = components.get(ModComponents.UPGRADES);
        if (contents != null) {
            int keys = loadSaved(contents.itemCopies().toList(), getUpgrades());
            if (keys > 0 && this instanceof Lockable lockable) lockable.restoreLegacyLock(keys);
        }
        if (this instanceof Lockable lockable && Boolean.TRUE.equals(components.get(ModComponents.DRAWER_LOCKED))) {
            lockable.setLocked(true);
        }
    }

    /** Installs one item from the stack into a free slot. Returns true if it went in. */
    default boolean tryInstall(ItemStack stack) {
        if (!acceptsUpgrade(stack)) return false;
        NonNullList<ItemStack> list = getUpgrades();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, stack.copyWithCount(1));
                stack.shrink(1);
                onUpgradesChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * With no free slot, swaps the weakest capacity upgrade for a stronger one from the stack.
     * Capacity only goes up, so this is always safe even when the drawer is full.
     * Returns the upgrade that came out, or empty if nothing changed.
     */
    default ItemStack tryReplaceCapacity(ItemStack stack) {
        if (!(stack.getItem() instanceof CapacityUpgradeItem held)) return ItemStack.EMPTY;
        NonNullList<ItemStack> list = getUpgrades();
        int weakest = -1;
        int weakestMultiplier = held.multiplier();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getItem() instanceof CapacityUpgradeItem cap && cap.multiplier() < weakestMultiplier) {
                weakest = i;
                weakestMultiplier = cap.multiplier();
            }
        }
        if (weakest < 0) return ItemStack.EMPTY;
        ItemStack old = list.get(weakest);
        list.set(weakest, stack.copyWithCount(1));
        stack.shrink(1);
        onUpgradesChanged();
        return old;
    }

    /** Whether the replacement is a strictly higher capacity upgrade than what is in the slot. */
    static boolean isHigherCapacity(ItemStack replacement, ItemStack current) {
        return replacement.getItem() instanceof CapacityUpgradeItem a && current.getItem() instanceof CapacityUpgradeItem b
                && a.multiplier() > b.multiplier();
    }

    /** Whether the upgrade in this slot may be swapped for the given one without losing capacity. */
    static boolean isStrongerCapacity(ItemStack replacement, ItemStack current) {
        return replacement.getItem() instanceof CapacityUpgradeItem a && current.getItem() instanceof CapacityUpgradeItem b
                && a.multiplier() >= b.multiplier();
    }
}
