package net.storagenexus.upgrade;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Block entities with four storage-upgrade slots (drawers, fluid drawers, tanks).
 * Capacity upgrades add together: no upgrade = x1, one iron = x2, iron + gold = x6.
 */
public interface Upgradeable {
    int UPGRADE_SLOTS = 4;

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

    default int capacityMultiplier() {
        return multiplierFor(getUpgrades(), -1);
    }

    /** Multiplier with one slot ignored (-1 to ignore none). */
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
}
