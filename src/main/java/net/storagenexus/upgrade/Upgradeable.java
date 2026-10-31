package net.storagenexus.upgrade;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.registry.ModComponents;

/**
 * Block entities with four storage-upgrade slots (drawers, fluid drawers, tanks).
 * Capacity upgrades add together: no upgrade = x1, one iron = x2, iron + gold = x6.
 */
public interface Upgradeable {
    /** Four general upgrade slots, then one slot that only takes the Lock Upgrade. */
    int UPGRADE_SLOTS = 5;
    int LOCK_SLOT = 4;

    NonNullList<ItemStack> getUpgrades();

    /** Called after the upgrade list changes so capacity and flags can be refreshed. */
    void onUpgradesChanged();

    /** Whether removing the upgrade in this slot would leave contents over capacity. */
    boolean canRemoveUpgrade(int slot);

    default boolean acceptsUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem up) || !up.kind().isStorageUpgrade()) return false;
        if (up.kind() != UpgradeItem.Kind.CAPACITY && hasUpgrade(up.kind())) return false;
        return true;
    }

    /** Whether this upgrade may go in this particular slot: the Lock only in the lock slot. */
    default boolean acceptsUpgrade(ItemStack stack, int slot) {
        return acceptsUpgrade(stack) && fitsSlot(stack, slot);
    }

    static boolean fitsSlot(ItemStack stack, int slot) {
        boolean lock = stack.getItem() instanceof UpgradeItem up && up.kind() == UpgradeItem.Kind.LOCK;
        return lock == (slot == LOCK_SLOT);
    }

    /** Older saves kept the Lock in a general slot; move it to the lock slot. */
    static void moveLockToLockSlot(NonNullList<ItemStack> upgrades) {
        if (!upgrades.get(LOCK_SLOT).isEmpty()) return;
        for (int i = 0; i < LOCK_SLOT; i++) {
            if (upgrades.get(i).getItem() instanceof UpgradeItem up && up.kind() == UpgradeItem.Kind.LOCK) {
                upgrades.set(LOCK_SLOT, upgrades.get(i));
                upgrades.set(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    default int capacityMultiplier() {
        return multiplierFor(getUpgrades(), -1);
    }

    /** No drawer holds more than this many times its normal capacity, however many upgrades it has. */
    int MAX_MULTIPLIER = 32;

    /** Multiplier with one slot ignored (-1 to ignore none), capped at {@link #MAX_MULTIPLIER}. */
    static int multiplierFor(NonNullList<ItemStack> upgrades, int skipSlot) {
        int sum = 0;
        for (int i = 0; i < upgrades.size(); i++) {
            if (i == skipSlot) continue;
            if (upgrades.get(i).getItem() instanceof CapacityUpgradeItem cap) sum += cap.multiplier();
        }
        return Math.clamp(sum, 1, MAX_MULTIPLIER);
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
            contents.copyInto(getUpgrades());
            moveLockToLockSlot(getUpgrades());
        }
    }

    /** Installs one item from the stack into a free slot. Returns true if it went in. */
    default boolean tryInstall(ItemStack stack) {
        if (!acceptsUpgrade(stack)) return false;
        NonNullList<ItemStack> list = getUpgrades();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isEmpty() && fitsSlot(stack, i)) {
                list.set(i, stack.copyWithCount(1));
                stack.shrink(1);
                onUpgradesChanged();
                return true;
            }
        }
        return false;
    }
}
